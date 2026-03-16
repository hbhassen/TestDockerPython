package com.hmiso.saml.integration;

import com.hmiso.saml.binding.BindingMessage;
import com.hmiso.saml.config.BindingType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Servlet filter that handles ACS/SLO endpoints and redirects to the IdP when needed.
 */
@WebFilter("/*")
public class SamlJakartaFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SamlJakartaFilter.class);

    @Override
    public void init(FilterConfig filterConfig) {
        LOGGER.info("SamlJakartaFilter init start");
        // No-op: configuration is provided by SamlBootstrapListener.
        LOGGER.info("SamlJakartaFilter init end");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String method = httpRequest.getMethod();
        String path = httpRequest.getRequestURI();
        LOGGER.info("SamlJakartaFilter doFilter start method={} path={}", method, path);

        try {
            SamlAuthenticationFilterConfig config = (SamlAuthenticationFilterConfig) request.getServletContext()
                    .getAttribute(SamlAppConfiguration.FILTER_CONFIG_CONTEXT_KEY);
            SamlAuthenticationFilterHelper helper = (SamlAuthenticationFilterHelper) request.getServletContext()
                    .getAttribute(SamlAppConfiguration.HELPER_CONTEXT_KEY);
            SamlAppConfiguration appConfig = (SamlAppConfiguration) request.getServletContext()
                    .getAttribute(SamlAppConfiguration.CONFIG_CONTEXT_KEY);

            if (config == null || helper == null) {
                chain.doFilter(request, response);
                return;
            }

            if (path.equals(config.getAcsPath()) && "POST".equalsIgnoreCase(method)) {
                helper.handleAcsRequest(httpRequest, httpResponse);
                return;
            }
            if (path.equals(config.getAcsPath()) && "GET".equalsIgnoreCase(method)) {
                String target = resolveFrontRedirectTarget(httpRequest, appConfig, config);
                Optional<BindingMessage> message = helper.initiateAuthenticationForTarget(httpRequest, target);
                if (message.isPresent()) {
                    sendBindingMessage(httpResponse, message.get());
                    return;
                }
            }
            if (path.equals(config.getSloPath())) {
                helper.handleSloRequest(httpRequest, httpResponse);
                return;
            }
            String errorPath = appConfig != null ? appConfig.getErrorPath() : SamlAppConfiguration.DEFAULT_ERROR_PATH;
            if (path.startsWith(errorPath)) {
                chain.doFilter(request, response);
                return;
            }

            if (!helper.validateJwtFromRequest(httpRequest, httpResponse)) {
                return;
            }
            Optional<BindingMessage> redirect = helper.shouldRedirectToIdP(httpRequest, httpResponse);
            if (redirect.isPresent()) {
                sendBindingMessage(httpResponse, redirect.get());
                return;
            }

            chain.doFilter(request, response);
            helper.attachJwtHeader(httpRequest, httpResponse);
        } finally {
            LOGGER.info("SamlJakartaFilter doFilter end method={} path={}", method, path);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("SamlJakartaFilter destroy start");
        LOGGER.info("SamlJakartaFilter destroy end");
    }

    private void renderPost(HttpServletResponse response, BindingMessage message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html");
        String relayStateInput = message.getRelayState() == null ? "" :
                "<input type=\"hidden\" name=\"RelayState\" value=\"" + message.getRelayState() + "\" />";
        String html = """
                <html>
                  <body onload=\"document.forms[0].submit()\">
                    <form method=\"post\" action=\"%s\">\n
                      <input type=\"hidden\" name=\"SAMLRequest\" value=\"%s\" />\n
                      %s
                    </form>
                  </body>
                </html>
                """.formatted(message.getDestination(), message.getPayload(), relayStateInput);
        response.getWriter().write(html);
    }

    private void sendBindingMessage(HttpServletResponse response, BindingMessage message) throws IOException {
        if (message.getBindingType() == BindingType.HTTP_REDIRECT) {
            String target = message.getDestination() + "?SAMLRequest=" + urlEncode(message.getPayload());
            if (message.getRelayState() != null) {
                target += "&RelayState=" + urlEncode(message.getRelayState());
            }
            response.sendRedirect(target);
        } else {
            renderPost(response, message);
        }
    }

    private String resolveFrontRedirectTarget(HttpServletRequest request,
                                              SamlAppConfiguration appConfig,
                                              SamlAuthenticationFilterConfig config) {
        String target = resolveConfiguredFrontTarget(request, appConfig);
        if (target == null) {
            target = resolveTargetFromRequestOrigin(request, appConfig);
        }
        if (target == null) {
            target = appConfig != null ? appConfig.getFrontRedirectUrl() : null;
        }
        if (target == null || target.isBlank()) {
            target = config.getPostLoginRedirectUrl();
        }
        if (target == null || target.isBlank() || target.equals(config.getAcsPath())) {
            String contextPath = request.getContextPath();
            if (contextPath == null || contextPath.isBlank()) {
                return "/";
            }
            return contextPath;
        }
        return target;
    }

    private String resolveConfiguredFrontTarget(HttpServletRequest request, SamlAppConfiguration appConfig) {
        if (request == null || appConfig == null) {
            return null;
        }
        String parameterName = normalize(appConfig.getFrontSelectionParameter());
        if (parameterName == null) {
            return null;
        }
        String frontId = normalize(request.getParameter(parameterName));
        if (frontId == null) {
            return null;
        }
        String target = normalize(appConfig.getFrontRedirects().get(frontId));
        if (target == null) {
            LOGGER.warn("Unknown front id '{}' for ACS path {}", frontId, request.getRequestURI());
        }
        return target;
    }

    private String resolveTargetFromRequestOrigin(HttpServletRequest request, SamlAppConfiguration appConfig) {
        if (request == null || appConfig == null) {
            return null;
        }
        Map<String, String> frontRedirects = appConfig.getFrontRedirects();
        if (frontRedirects.isEmpty()) {
            return null;
        }
        String requestOrigin = extractOrigin(request.getHeader("Origin"));
        if (requestOrigin == null) {
            requestOrigin = extractOrigin(request.getHeader("Referer"));
        }
        if (requestOrigin == null) {
            return null;
        }
        for (String target : frontRedirects.values()) {
            String configuredOrigin = extractOrigin(target);
            if (requestOrigin.equals(configuredOrigin)) {
                return target;
            }
        }
        return null;
    }

    private String extractOrigin(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            if (port < 0) {
                return scheme + "://" + host;
            }
            return scheme + "://" + host + ":" + port;
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Unable to parse front redirect origin '{}'", value);
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
