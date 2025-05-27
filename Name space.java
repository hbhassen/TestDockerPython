package com.example.saml;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class SamlAuthnRequestGenerator {

    public static String generateFixedAuthnRequest() throws Exception {
        // Initialisation OpenSAML
        InitializationService.initialize();

        // Création Issuer
        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue("idphost");

        // Création AuthnRequest
        AuthnRequest authnRequest = new AuthnRequestBuilder().buildObject();
        authnRequest.setID("fejjjfj");
        authnRequest.setVersion(org.opensaml.saml.common.SAMLVersion.VERSION_20);
        authnRequest.setIssuer(issuer);

        // Sérialisation OpenSAML vers DOM
        Element originalElement = SerializeSupport.nodeToElement(authnRequest);

        // Création d’un nouveau Document DOM pour forcer les namespaces et prefixes
        Document newDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        Element root = newDoc.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "saml2p:AuthnRequest");
        root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ns0", "urn:oasis:names:tc:SAML:2.0:assertion");
        root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:saml2p", "urn:oasis:names:tc:SAML:2.0:protocol");
        root.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:saml2", "urn:oasis:names:tc:SAML:2.0:assertion");
        root.setAttribute("ID", "fejjjfj");
        root.setAttribute("version", "2.0");

        // Issuer
        Element issuerElement = newDoc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "ns0:Issuer");
        issuerElement.setTextContent("idphost");

        // Assemblage
        root.appendChild(issuerElement);
        newDoc.appendChild(root);

        // Sérialisation finale
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(newDoc), new StreamResult(writer));

        return writer.toString();
    }
}
