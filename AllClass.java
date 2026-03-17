package com.hmiso.saml.integration;

import com.hmiso.saml.config.BindingType;
import com.hmiso.saml.config.IdentityProviderConfig;
import com.hmiso.saml.config.SamlConfiguration;
import com.hmiso.saml.config.SecurityConfig;
import com.hmiso.saml.config.ServiceProviderConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads application and SAML configuration from a YAML file.
 */
public final class SamlAppYamlConfigLoader {
    public static final String CONFIG_PROPERTY = "saml.config.path";
    public static final String CONFIG_ENV = "SAML_CONFIG_PATH";
    public static final String DEFAULT_RESOURCE = "/saml-config.yml";

    public SamlAppConfiguration load(String contextPath) {
        return load(contextPath, null);
    }

    public SamlAppConfiguration load(String contextPath, String configPath) {
        Map<String, Object> root = loadYaml(configPath);
        Map<String, Object> sp = requireMap(root, "service-provider");
        Map<String, Object> idp = requireMap(root, "identity-provider");
        Map<String, Object> security = requireMap(root, "security");
        Map<String, Object> app = requireMap(root, "app");

        String spEntityId = requireString(sp, "entity-id");
        String baseUrl = requireString(sp, "base-url");
        String acsPath = requireString(sp, "acs-path");
        String sloPath = requireString(sp, "slo-path");
        String nameIdFormat = requireString(sp, "name-id-format");
        BindingType authnRequestBinding = parseBinding(requireString(sp, "authn-request-binding"),
                "service-provider.authn-request-binding");
        boolean spWantAssertionsSigned = requireBoolean(sp, "want-assertions-signed");
        List<String> supportedNameIdFormats = stringList(sp, "supported-name-id-formats");
        if (supportedNameIdFormats.isEmpty()) {
            supportedNameIdFormats = List.of(nameIdFormat);
        }

        String normalizedContextPath = normalizeContextPath(contextPath);
        URI acsUrl = URI.create(joinUrl(baseUrl, normalizedContextPath, acsPath));
        URI sloUrl = URI.create(joinUrl(baseUrl, normalizedContextPath, sloPath));

        ServiceProviderConfig spConfig = new ServiceProviderConfig(
                spEntityId,
                acsUrl,
                sloUrl,
                nameIdFormat,
                authnRequestBinding,
                spWantAssertionsSigned,
                supportedNameIdFormats
        );

        String idpEntityId = requireString(idp, "entity-id");
        URI ssoUrl = URI.create(requireString(idp, "single-sign-on-service-url"));
        String idpSloRaw = optionalString(idp, "single-logout-service-url");
        URI idpSloUrl = idpSloRaw == null ? null : URI.create(idpSloRaw);
        String metadataRaw = optionalString(idp, "metadata-url");
        URI metadataUrl = metadataRaw == null ? null : URI.create(metadataRaw);
        boolean idpWantAssertionsSigned = requireBoolean(idp, "want-assertions-signed");
        boolean idpWantMessagesSigned = requireBoolean(idp, "want-messages-signed");
        List<BindingType> supportedBindings = bindingList(idp, "supported-bindings");

        IdentityProviderConfig idpConfig = new IdentityProviderConfig(
                idpEntityId,
                ssoUrl,
                idpSloUrl,
                null,
                null,
                metadataUrl,
                idpWantAssertionsSigned,
                idpWantMessagesSigned,
                supportedBindings
        );

        Duration clockSkew = parseDuration(requireString(security, "clock-skew"), "security.clock-skew");
        String signatureAlgorithm = requireString(security, "signature-algorithm");
        String digestAlgorithm = requireString(security, "digest-algorithm");
        String encryptionAlgorithm = optionalString(security, "encryption-algorithm");
        boolean forceHttpsRedirect = optionalBoolean(security, "force-https-redirect", false);
        boolean enableDetailedLogging = optionalBoolean(security, "enable-detailed-logging", false);

        SecurityConfig securityConfig = new SecurityConfig(
                clockSkew,
                signatureAlgorithm,
                digestAlgorithm,
                null,
                null,
                encryptionAlgorithm,
                forceHttpsRedirect,
                enableDetailedLogging
        );

        SamlConfiguration samlConfiguration = new SamlConfiguration(spConfig, idpConfig, securityConfig);

        String sessionAttributeKey = optionalString(app, "session-attribute-key");
        if (sessionAttributeKey == null) {
            sessionAttributeKey = SamlAppConfiguration.DEFAULT_SESSION_ATTRIBUTE_KEY;
        }
        String serverSessionAttributeKey = optionalString(app, "server-session-attribute-key");
        if (serverSessionAttributeKey == null) {
            serverSessionAttributeKey = SamlAppConfiguration.DEFAULT_SERVER_SESSION_ATTRIBUTE_KEY;
        }
        Duration sessionMaxTtl = parseSessionMaxTtl(app);
        String errorPath = optionalString(app, "error-path");
        if (errorPath == null) {
            errorPath = SamlAppConfiguration.DEFAULT_ERROR_PATH;
        }
        errorPath = prefixContextPath(normalizedContextPath, errorPath);
        String frontRedirectUrl = optionalString(app, "front-redirect-url");
        Map<String, String> frontRedirects = stringMap(app, "front-redirects");
        String frontSelectionParameter = optionalString(app, "front-redirect-parameter");
        if (frontSelectionParameter == null) {
            frontSelectionParameter = SamlAppConfiguration.DEFAULT_FRONT_SELECTION_PARAMETER;
        }
        List<String> protectedPaths = stringList(app, "protected-paths");
        if (protectedPaths.isEmpty()) {
            throw new IllegalStateException("Missing app.protected-paths");
        }
        List<String> normalizedProtectedPaths = new ArrayList<>(protectedPaths.size());
        for (String path : protectedPaths) {
            normalizedProtectedPaths.add(prefixContextPath(normalizedContextPath, path));
        }
        List<SamlRoleConstraint> roleConstraints = parseRoleConstraints(app, normalizedContextPath);
        Duration relayStateTtl = parseRelayStateTtl(app);
        Duration jwtTtl = parseJwtTtl(security);
        String jwtSecret = optionalString(security, "jwt-secret");
        CorsConfiguration corsConfiguration = buildCorsConfiguration(app);
        boolean blockBrowserNavigation = optionalBoolean(app, "block-browser-navigation",
                SamlAppConfiguration.DEFAULT_BLOCK_BROWSER_NAVIGATION);
        boolean jaspicEnabled = optionalBoolean(app, "jaspic-enabled",
                SamlAppConfiguration.DEFAULT_JASPIC_ENABLED);

        return new SamlAppConfiguration(
                samlConfiguration,
                sessionAttributeKey,
                serverSessionAttributeKey,
                sessionMaxTtl,
                normalizedProtectedPaths,
                roleConstraints,
                prefixContextPath(normalizedContextPath, acsPath),
                prefixContextPath(normalizedContextPath, sloPath),
                relayStateTtl,
                errorPath,
                frontRedirectUrl,
                frontRedirects,
                frontSelectionParameter,
                jwtTtl,
                jwtSecret,
                corsConfiguration,
                blockBrowserNavigation,
                jaspicEnabled
        );
    }

    private CorsConfiguration buildCorsConfiguration(Map<String, Object> app) {
        List<String> allowedOrigins = stringList(app, "cors-allowed-origins");
        boolean enabled = optionalBoolean(app, "cors-enabled", !allowedOrigins.isEmpty());
        if (allowedOrigins.isEmpty()) {
            enabled = false;
        }

        List<String> allowedMethods = stringList(app, "cors-allowed-methods");
        if (allowedMethods.isEmpty()) {
            allowedMethods = CorsConfiguration.DEFAULT_ALLOWED_METHODS;
        }
        List<String> allowedHeaders = stringList(app, "cors-allowed-headers");
        if (allowedHeaders.isEmpty()) {
            allowedHeaders = CorsConfiguration.DEFAULT_ALLOWED_HEADERS;
        }
        List<String> exposeHeaders = stringList(app, "cors-expose-headers");
        if (exposeHeaders.isEmpty()) {
            exposeHeaders = CorsConfiguration.DEFAULT_EXPOSE_HEADERS;
        }
        boolean allowCredentials = optionalBoolean(app, "cors-allow-credentials", true);
        return new CorsConfiguration(enabled, allowCredentials, allowedOrigins, allowedMethods, allowedHeaders,
                exposeHeaders);
    }

    private Duration parseRelayStateTtl(Map<String, Object> app) {
        Object value = app.get("relay-state-ttl-minutes");
        if (value == null) {
            return SamlAppConfiguration.DEFAULT_RELAY_STATE_TTL;
        }
        if (value instanceof Number) {
            return Duration.ofMinutes(((Number) value).longValue());
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return SamlAppConfiguration.DEFAULT_RELAY_STATE_TTL;
        }
        try {
            return Duration.ofMinutes(Long.parseLong(text));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid relay-state-ttl-minutes", ex);
        }
    }

    private Duration parseSessionMaxTtl(Map<String, Object> app) {
        Object value = app.get("session-max-minutes");
        if (value == null) {
            return SamlAppConfiguration.DEFAULT_SESSION_MAX_TTL;
        }
        if (value instanceof Number) {
            return Duration.ofMinutes(((Number) value).longValue());
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return SamlAppConfiguration.DEFAULT_SESSION_MAX_TTL;
        }
        try {
            return Duration.ofMinutes(Long.parseLong(text));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid session-max-minutes", ex);
        }
    }

    private Duration parseJwtTtl(Map<String, Object> security) {
        Object value = security.get("jwt-ttl-seconds");
        if (value == null) {
            return SamlAppConfiguration.DEFAULT_JWT_TTL;
        }
        if (value instanceof Number) {
            return Duration.ofSeconds(((Number) value).longValue());
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return SamlAppConfiguration.DEFAULT_JWT_TTL;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(text));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid jwt-ttl-seconds", ex);
        }
    }

    private List<SamlRoleConstraint> parseRoleConstraints(Map<String, Object> app, String contextPath) {
        Object raw = app.get("role-constraints");
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof Map<?, ?>)) {
            throw new IllegalStateException("Expected map for app.role-constraints");
        }
        Map<?, ?> rawMap = (Map<?, ?>) raw;
        List<SamlRoleConstraint> constraints = new ArrayList<>(rawMap.size());
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new IllegalStateException("Expected string path in app.role-constraints");
            }
            String path = ((String) entry.getKey()).trim();
            if (path.isEmpty()) {
                throw new IllegalStateException("Blank path in app.role-constraints");
            }
            List<String> roles = stringListFromObject(entry.getValue(), "app.role-constraints." + path);
            if (roles.isEmpty()) {
                throw new IllegalStateException("Missing roles for app.role-constraints." + path);
            }
            String normalizedPath = prefixContextPath(contextPath, path);
            constraints.add(new SamlRoleConstraint(normalizedPath, roles));
        }
        return List.copyOf(constraints);
    }

    private Map<String, Object> loadYaml(String configPath) {
        try (InputStream input = openConfigStream(configPath)) {
            Yaml yaml = new Yaml();
            Object data = yaml.load(input);
            if (!(data instanceof Map<?, ?>)) {
                throw new IllegalStateException("Invalid YAML: root must be a map");
            }
            return castMap(data);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read SAML config", e);
        }
    }

    private InputStream openConfigStream(String explicitConfigPath) throws IOException {
        String configuredPath = normalizePath(explicitConfigPath);
        if (configuredPath == null) {
            configuredPath = normalizePath(System.getProperty(CONFIG_PROPERTY));
        }
        if (configuredPath == null) {
            configuredPath = normalizePath(System.getenv(CONFIG_ENV));
        }
        if (configuredPath != null) {
            return Files.newInputStream(Path.of(configuredPath));
        }
        InputStream input = openFromContextClassLoader();
        if (input != null) {
            return input;
        }
        input = SamlAppYamlConfigLoader.class.getResourceAsStream(DEFAULT_RESOURCE);
        if (input == null) {
            throw new IllegalStateException("Missing " + DEFAULT_RESOURCE + " on the classpath");
        }
        return input;
    }

    private InputStream openFromContextClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            return null;
        }
        String resourceName = DEFAULT_RESOURCE.startsWith("/") ? DEFAULT_RESOURCE.substring(1) : DEFAULT_RESOURCE;
        InputStream input = loader.getResourceAsStream(resourceName);
        if (input != null) {
            return input;
        }
        if (!resourceName.equals(DEFAULT_RESOURCE)) {
            return loader.getResourceAsStream(DEFAULT_RESOURCE);
        }
        return null;
    }

    private static String normalizePath(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeContextPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "";
        }
        if (!contextPath.startsWith("/")) {
            return "/" + contextPath;
        }
        return contextPath;
    }

    private static String joinUrl(String baseUrl, String contextPath, String path) {
        String normalizedBase = stripTrailingSlash(baseUrl);
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + contextPath + normalizedPath;
    }

    private static String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String prefixContextPath(String contextPath, String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        if (contextPath == null || contextPath.isBlank()) {
            return normalized;
        }
        if (normalized.startsWith(contextPath + "/") || normalized.equals(contextPath)) {
            return normalized;
        }
        return contextPath + normalized;
    }

    private static Map<String, Object> requireMap(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value instanceof Map<?, ?>) {
            return castMap(value);
        }
        throw new IllegalStateException("Missing map key: " + key);
    }

    private static String requireString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalStateException("Missing key: " + key);
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.isEmpty()) {
                throw new IllegalStateException("Blank value for key: " + key);
            }
            return text;
        }
        throw new IllegalStateException("Expected string for key: " + key);
    }

    private static String optionalString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            return text.isEmpty() ? null : text;
        }
        throw new IllegalStateException("Expected string for key: " + key);
    }

    private static boolean requireBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalStateException("Missing key: " + key);
        }
        return parseBoolean(value, key);
    }

    private static boolean optionalBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return parseBoolean(value, key);
    }

    private static boolean parseBoolean(Object value, String key) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = value.toString().trim();
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw new IllegalStateException("Expected boolean for key: " + key);
    }

    private static List<String> stringListFromObject(Object value, String key) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?>) {
            List<?> rawList = (List<?>) value;
            List<String> values = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                if (!(item instanceof String)) {
                    throw new IllegalStateException("Expected string items for key: " + key);
                }
                String text = ((String) item).trim();
                if (!text.isEmpty()) {
                    values.add(text);
                }
            }
            return values;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            return text.isEmpty() ? List.of() : List.of(text);
        }
        throw new IllegalStateException("Expected list for key: " + key);
    }

    private static List<String> stringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?>) {
            List<?> rawList = (List<?>) value;
            List<String> values = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                if (!(item instanceof String)) {
                    throw new IllegalStateException("Expected string items for key: " + key);
                }
                String text = ((String) item).trim();
                if (!text.isEmpty()) {
                    values.add(text);
                }
            }
            return values;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            return text.isEmpty() ? List.of() : List.of(text);
        }
        throw new IllegalStateException("Expected list for key: " + key);
    }

    private static Map<String, String> stringMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw new IllegalStateException("Expected map for key: " + key);
        }
        Map<String, String> values = new LinkedHashMap<>(rawMap.size());
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String rawKey)) {
                throw new IllegalStateException("Expected string key in map: " + key);
            }
            if (!(entry.getValue() instanceof String rawValue)) {
                throw new IllegalStateException("Expected string value in map: " + key);
            }
            String cleanedKey = rawKey.trim();
            String cleanedValue = rawValue.trim();
            if (cleanedKey.isEmpty()) {
                throw new IllegalStateException("Blank map key: " + key);
            }
            if (cleanedValue.isEmpty()) {
                throw new IllegalStateException("Blank map value for key: " + key + "." + cleanedKey);
            }
            values.put(cleanedKey, cleanedValue);
        }
        return values;
    }

    private static List<BindingType> bindingList(Map<String, Object> map, String key) {
        List<String> rawValues = stringList(map, key);
        if (rawValues.isEmpty()) {
            throw new IllegalStateException("Missing bindings for key: " + key);
        }
        List<BindingType> bindings = new ArrayList<>(rawValues.size());
        for (String raw : rawValues) {
            bindings.add(parseBinding(raw, key));
        }
        return bindings;
    }

    private static BindingType parseBinding(String raw, String key) {
        try {
            return BindingType.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid binding for key: " + key, ex);
        }
    }

    private static Duration parseDuration(String raw, String key) {
        try {
            return Duration.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("Invalid duration for key: " + key, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
package com.hmiso.saml.integration;

import com.hmiso.saml.config.SamlConfiguration;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregates application-level settings needed to wire the servlet filter.
 */
public final class SamlAppConfiguration {
    public static final String DEFAULT_SESSION_ATTRIBUTE_KEY = "saml.principal";
    public static final String DEFAULT_SERVER_SESSION_ATTRIBUTE_KEY = "saml.server.session";
    public static final String DEFAULT_ERROR_PATH = "/saml/error";
    public static final Duration DEFAULT_RELAY_STATE_TTL = Duration.ofMinutes(5);
    public static final Duration DEFAULT_SESSION_MAX_TTL = Duration.ofMinutes(60);
    public static final Duration DEFAULT_JWT_TTL = Duration.ofSeconds(10);
    public static final String DEFAULT_FRONT_SELECTION_PARAMETER = "front";
    public static final boolean DEFAULT_BLOCK_BROWSER_NAVIGATION = false;
    public static final boolean DEFAULT_JASPIC_ENABLED = false;

    public static final String CONFIG_CONTEXT_KEY = "smalib.saml.app.config";
    public static final String FILTER_CONFIG_CONTEXT_KEY = "smalib.saml.filter.config";
    public static final String HELPER_CONTEXT_KEY = "smalib.saml.filter.helper";

    private final SamlConfiguration samlConfiguration;
    private final String sessionAttributeKey;
    private final String serverSessionAttributeKey;
    private final Duration sessionMaxTtl;
    private final List<String> protectedPaths;
    private final List<SamlRoleConstraint> roleConstraints;
    private final String acsPath;
    private final String sloPath;
    private final Duration relayStateTtl;
    private final String errorPath;
    private final String frontRedirectUrl;
    private final Map<String, String> frontRedirects;
    private final String frontSelectionParameter;
    private final Duration jwtTtl;
    private final String jwtSecret;
    private final CorsConfiguration corsConfiguration;
    private final boolean blockBrowserNavigation;
    private final boolean jaspicEnabled;

    public SamlAppConfiguration(SamlConfiguration samlConfiguration,
                                String sessionAttributeKey,
                                String serverSessionAttributeKey,
                                Duration sessionMaxTtl,
                                List<String> protectedPaths,
                                List<SamlRoleConstraint> roleConstraints,
                                String acsPath,
                                String sloPath,
                                Duration relayStateTtl,
                                String errorPath,
                                String frontRedirectUrl,
                                Map<String, String> frontRedirects,
                                String frontSelectionParameter,
                                Duration jwtTtl,
                                String jwtSecret,
                                CorsConfiguration corsConfiguration,
                                boolean blockBrowserNavigation,
                                boolean jaspicEnabled) {
        this.samlConfiguration = Objects.requireNonNull(samlConfiguration, "samlConfiguration");
        this.sessionAttributeKey = Objects.requireNonNull(sessionAttributeKey, "sessionAttributeKey");
        this.serverSessionAttributeKey = Objects.requireNonNull(serverSessionAttributeKey, "serverSessionAttributeKey");
        this.sessionMaxTtl = Objects.requireNonNull(sessionMaxTtl, "sessionMaxTtl");
        this.protectedPaths = List.copyOf(Objects.requireNonNull(protectedPaths, "protectedPaths"));
        this.roleConstraints = List.copyOf(Objects.requireNonNull(roleConstraints, "roleConstraints"));
        this.acsPath = Objects.requireNonNull(acsPath, "acsPath");
        this.sloPath = Objects.requireNonNull(sloPath, "sloPath");
        this.relayStateTtl = Objects.requireNonNull(relayStateTtl, "relayStateTtl");
        this.errorPath = Objects.requireNonNull(errorPath, "errorPath");
        this.frontRedirectUrl = frontRedirectUrl;
        this.frontRedirects = Collections.unmodifiableMap(
                new LinkedHashMap<>(Objects.requireNonNull(frontRedirects, "frontRedirects")));
        this.frontSelectionParameter = Objects.requireNonNull(frontSelectionParameter, "frontSelectionParameter");
        this.jwtTtl = Objects.requireNonNull(jwtTtl, "jwtTtl");
        this.jwtSecret = jwtSecret;
        this.corsConfiguration = Objects.requireNonNull(corsConfiguration, "corsConfiguration");
        this.blockBrowserNavigation = blockBrowserNavigation;
        this.jaspicEnabled = jaspicEnabled;
    }

    public SamlConfiguration getSamlConfiguration() {
        return samlConfiguration;
    }

    public String getSessionAttributeKey() {
        return sessionAttributeKey;
    }

    public String getServerSessionAttributeKey() {
        return serverSessionAttributeKey;
    }

    public Duration getSessionMaxTtl() {
        return sessionMaxTtl;
    }

    public List<String> getProtectedPaths() {
        return protectedPaths;
    }

    public List<SamlRoleConstraint> getRoleConstraints() {
        return roleConstraints;
    }

    public String getAcsPath() {
        return acsPath;
    }

    public String getSloPath() {
        return sloPath;
    }

    public Duration getRelayStateTtl() {
        return relayStateTtl;
    }

    public String getErrorPath() {
        return errorPath;
    }

    public String getFrontRedirectUrl() {
        return frontRedirectUrl;
    }

    public Map<String, String> getFrontRedirects() {
        return frontRedirects;
    }

    public String getFrontSelectionParameter() {
        return frontSelectionParameter;
    }

    public Duration getJwtTtl() {
        return jwtTtl;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public CorsConfiguration getCorsConfiguration() {
        return corsConfiguration;
    }

    public boolean isBlockBrowserNavigation() {
        return blockBrowserNavigation;
    }

    public boolean isJaspicEnabled() {
        return jaspicEnabled;
    }
}
package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlAttributeKeys;
import com.hmiso.saml.api.SamlException;
import com.hmiso.saml.api.SamlPrincipal;
import com.hmiso.saml.api.SamlRoleHelper;
import com.hmiso.saml.binding.BindingMessage;
import com.hmiso.saml.config.BindingType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Logique centrale du filtre Servlet decrite dans le module VII.
 */
public class SamlAuthenticationFilterHelper {
    private final SamlAuthenticationFilterConfig config;
    private final SamlSessionHelper sessionHelper;
    private final SamlAuditLogger auditLogger;
    private final SamlErrorHandler errorHandler;
    private final Predicate<HttpServletRequest> protectedRequestPredicate;
    private final SamlServerSessionRegistry sessionRegistry;
    private final String serverSessionAttributeKey;
    private final Duration sessionMaxTtl;
    private final SamlJwtService jwtService;
    private final Duration jwtTtl;

    public SamlAuthenticationFilterHelper(SamlAuthenticationFilterConfig config,
                                          SamlSessionHelper sessionHelper,
                                          SamlAuditLogger auditLogger,
                                          SamlErrorHandler errorHandler) {
        this.config = config;
        this.sessionHelper = sessionHelper;
        this.auditLogger = auditLogger;
        this.errorHandler = errorHandler;
        this.protectedRequestPredicate = req -> isProtected(req.getRequestURI(), config.getProtectedPaths());
        this.sessionRegistry = config.getServerSessionRegistry();
        this.serverSessionAttributeKey = config.getServerSessionAttributeKey();
        this.sessionMaxTtl = config.getSessionMaxTtl();
        this.jwtService = config.getJwtService();
        this.jwtTtl = config.getJwtTtl();
    }

    public Optional<BindingMessage> shouldRedirectToIdP(HttpServletRequest request, HttpServletResponse response) {
        if (!protectedRequestPredicate.test(request)) {
            return Optional.empty();
        }
        HttpSession session = request.getSession(false);
        Optional<SamlPrincipal> existing = sessionHelper.retrievePrincipalFromSession(session,
                config.getSessionAttributeKey());
        if (existing.isPresent()) {
            if (isServerSessionValid(session, existing.get())) {
                return Optional.empty();
            }
            sessionHelper.invalidateSession(session);
        }
        String originalUri = request.getRequestURI();
        // Enregistre un relay state serveur (si disponible) pour restaurer l'URL initiale apres ACS.
        if (config.getRelayStateStore() != null) {
            config.getRelayStateStore().save(originalUri, originalUri);
        }
        BindingMessage message = config.getSamlServiceProvider().initiateAuthentication(originalUri);
        if (auditLogger != null) {
            auditLogger.logAuthnRequestInitiated(message);
        }
        return Optional.of(message);
    }

    public Optional<BindingMessage> initiateAuthenticationForTarget(HttpServletRequest request, String target) {
        if (request == null) {
            return Optional.empty();
        }
        String relayState = normalizeTarget(target);
        String storedTarget = relayState;
        if (relayState == null) {
            relayState = request.getRequestURI();
            storedTarget = relayState;
        }
        if (relayState == null || relayState.isBlank()) {
            return Optional.empty();
        }
        if (config.getRelayStateStore() != null && storedTarget != null) {
            config.getRelayStateStore().save(relayState, storedTarget);
        }
        BindingMessage message = config.getSamlServiceProvider().initiateAuthentication(relayState);
        if (auditLogger != null) {
            auditLogger.logAuthnRequestInitiated(message);
        }
        return Optional.of(message);
    }

    public SamlPrincipal handleAcsRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String samlResponse = request.getParameter("SAMLResponse");
        String relayState = normalizeTarget(request.getParameter("RelayState"));
        try {
            SamlPrincipal principal = config.getSamlServiceProvider().processSamlResponse(samlResponse, relayState);
            HttpSession session = request.getSession(true);
            sessionHelper.storePrincipalInSession(session, principal, config.getSessionAttributeKey());
            SamlServerSession serverSession = createServerSession(principal, session);
            issueJwtHeader(response, serverSession);
            String target = relayState;
            if (relayState != null && config.getRelayStateStore() != null) {
                String stored = config.getRelayStateStore().get(relayState);
                if (stored != null) {
                    target = stored;
                    config.getRelayStateStore().invalidate(relayState);
                }
            }
            if (target == null || target.isBlank()) {
                target = normalizeTarget(config.getPostLoginRedirectUrl());
            }
            if (auditLogger != null) {
                auditLogger.logAuthenticationSuccess(principal);
            }
            if (target != null && response != null) {
                response.sendRedirect(target);
            }
            return principal;
        } catch (Exception ex) {
            if (auditLogger != null) {
                auditLogger.logAuthenticationFailure(ex);
            }
            if (errorHandler != null) {
                String target = errorHandler.handleValidationError(ex);
                if (response != null) {
                    response.sendRedirect(target);
                }
            }
            throw ex;
        }
    }

    public void handleSloRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Optional<SamlPrincipal> principal = sessionHelper.retrievePrincipalFromSession(session, config.getSessionAttributeKey());
        principal.ifPresent(p -> {
            if (auditLogger != null) {
                auditLogger.logLogoutInitiated(p);
            }
            BindingMessage logout = config.getSamlServiceProvider().initiateLogout(p, null);
            if (auditLogger != null) {
                auditLogger.logLogoutSuccess(p.getSessionIndex());
            }
            if (response != null) {
                try {
                    if (logout.getBindingType() == BindingType.HTTP_REDIRECT) {
                        String target = logout.getDestination() + "?SAMLRequest=" + urlEncode(logout.getPayload());
                        if (logout.getRelayState() != null) {
                            target += "&RelayState=" + urlEncode(logout.getRelayState());
                        }
                        response.sendRedirect(target);
                    } else {
                        renderPost(response, logout);
                    }
                } catch (IOException ignored) {
                    // nothing to do
                }
            }
            invalidateServerSession(session);
        });
        sessionHelper.invalidateSession(session);
    }

    public Optional<SamlPrincipal> extractPrincipalFromSession(HttpSession session) {
        return sessionHelper.retrievePrincipalFromSession(session, config.getSessionAttributeKey());
    }

    public boolean validateJwtFromRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request == null || response == null) {
            return true;
        }
        if (!protectedRequestPredicate.test(request)) {
            return true;
        }
        String token = request.getHeader(SamlJwtService.DEFAULT_HEADER_NAME);
        if (token == null || token.isBlank()) {
            return true;
        }
        if (jwtService == null || sessionRegistry == null) {
            rejectRequest(response, "JWT non supporte");
            return false;
        }
        try {
            SamlJwtService.JwtClaims claims = jwtService.validate(token);
            SamlServerSession serverSession = sessionRegistry.getSession(claims.getSessionId());
            if (serverSession == null) {
                rejectRequest(response, "Session serveur expiree");
                return false;
            }
            String subject = claims.getSubject();
            if (subject != null && !subject.equals(serverSession.getPrincipal().getNameId())) {
                rejectRequest(response, "JWT sujet invalide");
                return false;
            }
            HttpSession session = request.getSession(true);
            sessionHelper.storePrincipalInSession(session, serverSession.getPrincipal(), config.getSessionAttributeKey());
            session.setAttribute(serverSessionAttributeKey, serverSession.getSessionId());
            return true;
        } catch (SamlException ex) {
            rejectRequest(response, ex.getMessage());
            return false;
        }
    }

    public void attachJwtHeader(HttpServletRequest request, HttpServletResponse response) {
        if (!protectedRequestPredicate.test(request) || jwtService == null || response == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        Optional<SamlPrincipal> principal = sessionHelper.retrievePrincipalFromSession(session, config.getSessionAttributeKey());
        if (principal.isEmpty()) {
            return;
        }
        SamlServerSession serverSession = resolveServerSession(session);
        issueJwtHeader(response, serverSession);
    }

    private boolean isProtected(String requestUri, List<String> protectedPaths) {
        if (requestUri == null || protectedPaths == null) {
            return false;
        }
        return protectedPaths.stream().anyMatch(path -> matches(requestUri, path));
    }

    private boolean matches(String requestUri, String path) {
        if (path.endsWith("/*")) {
            String base = path.substring(0, path.length() - 2);
            return requestUri.startsWith(base);
        }
        return requestUri.equals(path);
    }

    private void renderPost(HttpServletResponse response, BindingMessage message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html");
        String relayStateInput = message.getRelayState() == null ? "" :
                "<input type=\"hidden\" name=\"RelayState\" value=\"" + message.getRelayState() + "\" />";
        String html = """
                <html>
                  <body onload="document.forms[0].submit()">
                    <form method="post" action="%s">

                      <input type="hidden" name="SAMLRequest" value="%s" />

                      %s
                    </form>
                  </body>
                </html>
                """.formatted(message.getDestination(), message.getPayload(), relayStateInput);
        response.getWriter().write(html);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isServerSessionValid(HttpSession session, SamlPrincipal principal) {
        if (sessionRegistry == null || session == null) {
            return true;
        }
        SamlServerSession serverSession = resolveServerSession(session);
        if (serverSession == null) {
            return false;
        }
        String nameId = principal.getNameId();
        return nameId != null && nameId.equals(serverSession.getPrincipal().getNameId());
    }

    private SamlServerSession resolveServerSession(HttpSession session) {
        if (sessionRegistry == null || session == null) {
            return null;
        }
        Object attribute = session.getAttribute(serverSessionAttributeKey);
        if (!(attribute instanceof String sessionId)) {
            return null;
        }
        return sessionRegistry.getSession(sessionId);
    }

    private SamlServerSession createServerSession(SamlPrincipal principal, HttpSession session) {
        if (sessionRegistry == null || session == null) {
            return null;
        }
        Instant now = Instant.now();
        Instant notOnOrAfter = extractInstant(principal.getAttributes().get(SamlAttributeKeys.NOT_ON_OR_AFTER));
        Instant maxExpiry = now.plus(sessionMaxTtl);
        Instant expiresAt = notOnOrAfter == null ? maxExpiry : minInstant(notOnOrAfter, maxExpiry);
        SamlServerSession serverSession = sessionRegistry.createSession(principal, expiresAt);
        session.setAttribute(serverSessionAttributeKey, serverSession.getSessionId());
        return serverSession;
    }

    private void invalidateServerSession(HttpSession session) {
        if (sessionRegistry == null || session == null) {
            return;
        }
        Object attribute = session.getAttribute(serverSessionAttributeKey);
        if (attribute instanceof String sessionId) {
            sessionRegistry.invalidate(sessionId);
        }
    }

    private Instant extractInstant(Object value) {
        if (value instanceof Instant) {
            return (Instant) value;
        }
        if (value instanceof String text) {
            try {
                return Instant.parse(text);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private Instant minInstant(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }

    private void rejectRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/plain");
        response.getWriter().write(message == null ? "Unauthorized" : message);
    }

    private void issueJwtHeader(HttpServletResponse response, SamlServerSession serverSession) {
        if (response == null || jwtService == null || serverSession == null) {
            return;
        }
        List<String> roles = SamlRoleHelper.extractRoles(serverSession.getPrincipal());
        String token = jwtService.issueToken(serverSession, jwtTtl, roles);
        response.setHeader(SamlJwtService.DEFAULT_HEADER_NAME, token);
    }

    private String normalizeTarget(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
