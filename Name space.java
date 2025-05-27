package com.example.saml;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.time.Instant;
import java.util.UUID;

public class SamlRequestBuilder {

    public static String generateAuthnRequest() throws Exception {
        // Initialisation du contexte OpenSAML
        InitializationService.initialize();

        // Construction de l'objet Issuer
        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue("saml2");

        // Construction de l'objet AuthnRequest
        AuthnRequest authnRequest = new AuthnRequestBuilder().buildObject(
                new QName("urn:oasis:names:tc:SAML:2.0:protocol", "AuthnRequest", "saml2p")
        );

        authnRequest.setID("_" + UUID.randomUUID());
        authnRequest.setVersion(SAMLVersion.VERSION_20);
        authnRequest.setIssueInstant(Instant.parse("2025-05-27T11:51:18Z"));
        authnRequest.setDestination("https://idqhost/test/idp/samlv20");
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        authnRequest.setAssertionConsumerServiceURL("https://hi/saml/consume");
        authnRequest.setForceAuthn(Boolean.FALSE);
        authnRequest.setIsPassive(Boolean.FALSE);
        authnRequest.setIssuer(issuer);

        // Sérialisation en XML (avec namespace visible)
        Element element = SerializeSupport.nodeToElement(authnRequest);
        return SerializeSupport.prettyPrintXML(element);
    }
}