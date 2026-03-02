package com.hmiso.saml.saml;

import com.hmiso.saml.config.BindingType;
import com.hmiso.saml.config.SamlConfiguration;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Construction minimale d'une AuthnRequest SAML valide (namespaces, Issuer, NameIDPolicy).
 */
public class AuthnRequestBuilder {
    private static final List<String> AUTHN_CONTEXT_CLASS_REFS = List.of(
            "urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocol",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocolPassword",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:Kerberos",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileOneFactorContract",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileOneFactorUnregistered",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorContract",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorUnregistered",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:Password",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:PreviousSession",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:X509",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:PGP",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:SPKI",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:SmartcardPKI",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:SoftwarePKI",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:Telephony",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:NomadTelephony",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:PersonalizedTelephony",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:SecureRemotePassword",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:TLSClient",
            "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken"
    );

    private final SamlConfiguration configuration;

    public AuthnRequestBuilder(SamlConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public String build(String requestId, Instant issueInstant) {
        String protocolBinding = bindingUrn(configuration.getServiceProvider().getAuthnRequestBinding());
        String requestedAuthnContext = requestedAuthnContextBlock();
        return """
                <samlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                                    xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
                                    ID="%s"
                                    Version="2.0"
                                    IssueInstant="%s"
                                    Destination="%s"
                                    ProtocolBinding="%s"
                                    AssertionConsumerServiceURL="%s">
                  <saml:Issuer>%s</saml:Issuer>
                  <samlp:NameIDPolicy
                      AllowCreate="true"
                      Format="%s" />
                %s
                </samlp:AuthnRequest>
                """.formatted(
                requestId,
                issueInstant,
                configuration.getIdentityProvider().getSingleSignOnServiceUrl(),
                protocolBinding,
                configuration.getServiceProvider().getAssertionConsumerServiceUrl(),
                configuration.getServiceProvider().getEntityId(),
                configuration.getServiceProvider().getNameIdFormat(),
                requestedAuthnContext
        );
    }

    private String requestedAuthnContextBlock() {
        StringBuilder xml = new StringBuilder();
        xml.append("  <samlp:RequestedAuthnContext Comparison=\"exact\">").append('\n');
        for (String classRef : AUTHN_CONTEXT_CLASS_REFS) {
            xml.append("    <saml:AuthnContextClassRef>")
                    .append(classRef)
                    .append("</saml:AuthnContextClassRef>")
                    .append('\n');
        }
        xml.append("  </samlp:RequestedAuthnContext>");
        return xml.toString();
    }

    private String bindingUrn(BindingType bindingType) {
        if (bindingType == BindingType.HTTP_REDIRECT) {
            return "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";
        }
        return "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";
    }
}
