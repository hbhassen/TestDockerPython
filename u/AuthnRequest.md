# AuthnRequest dans SmalLib

## 1) Ce que contient `samlRequest` dans SmalLib

Dans le code SmalLib, la methode `initiateAuthentication(...)` construit une `AuthnRequest` XML puis l'encode dans `BindingMessage.payload`.

Concretement:
- `samlRequest` commence comme un XML `samlp:AuthnRequest` brut.
- Si le binding est `HTTP_REDIRECT`, le payload est `DEFLATE + Base64` du XML.
- Si le binding est `HTTP_POST`, le payload est `Base64` du XML.
- Le `relayState` est transporte a cote du `samlRequest`.

Donc, `samlRequest` n'est pas un objet metier separe: c'est le message SAML XML encode pour le transport HTTP selon le binding choisi.

## 2) Elements de `AuthnRequest` et role de chaque element

Le builder SmalLib produit une requete avec les elements suivants:

- `samlp:AuthnRequest`
  - Racine du message d'authentification SAML 2.0.
- `ID`
  - Identifiant unique de la requete (utilise pour correlation `InResponseTo`).
- `Version="2.0"`
  - Version du protocole SAML.
- `IssueInstant`
  - Horodatage UTC de creation de la requete.
- `Destination`
  - URL SSO de l'IdP cible.
- `ProtocolBinding`
  - Binding souhaite pour la reponse (`HTTP-POST` ou `HTTP-Redirect`).
- `AssertionConsumerServiceURL`
  - Endpoint ACS du SP ou l'IdP doit poster la `SAMLResponse`.
- `saml:Issuer`
  - Identite du SP (entity ID).
- `samlp:NameIDPolicy`
  - Politique d'identifiant utilisateur demandee (`Format`, `AllowCreate`).
- `samlp:RequestedAuthnContext` (ajoute dans cette evolution)
  - Expression du niveau/type d'authentification demande cote IdP.
  - `Comparison="exact"` impose une correspondance exacte avec au moins une classe demandee.

## 3) Pourquoi definir `RequestedAuthnContext` est important

`RequestedAuthnContext` n'est pas strictement obligatoire dans toutes les integrations, mais il est souvent necessaire pour:

- Aligner le niveau d'authentification avec la politique securite de l'application.
- Eviter des refus IdP dans certains environnements exigeant un `Comparison="exact"`.
- Rendre explicite les mecanismes d'authentification acceptes par le SP.
- Stabiliser l'interoperabilite entre SP et IdP lors des migrations (Keycloak, ADFS, Entra ID, etc.).

Sans `RequestedAuthnContext`, l'IdP choisit librement sa methode, ce qui peut etre insuffisant pour certaines exigences de conformite.

## 4) Liste des `AuthnContextClassRef` standard SAML 2.0 utilises

SmalLib inclut les classes standard suivantes:

- `urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocol`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocolPassword`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:Kerberos`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:MobileOneFactorContract`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:MobileOneFactorUnregistered`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorContract`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorUnregistered`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:Password`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:PreviousSession`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:X509`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:PGP`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:SPKI`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:SmartcardPKI`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:SoftwarePKI`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:Telephony`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:NomadTelephony`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:PersonalizedTelephony`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:SecureRemotePassword`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:TLSClient`
- `urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken`

## 5) Exemple simplifie de `AuthnRequest` generee

```xml
<samlp:AuthnRequest ...>
  <saml:Issuer>sp-entity</saml:Issuer>
  <samlp:NameIDPolicy AllowCreate="true" Format="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress" />
  <samlp:RequestedAuthnContext Comparison="exact">
    <saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml:AuthnContextClassRef>
    <!-- ... autres classes ... -->
  </samlp:RequestedAuthnContext>
</samlp:AuthnRequest>
```

## 6) Reference schema

Les classes ci-dessus sont basees sur les schemas officiels OASIS SAML 2.0 (fichiers `saml-schema-authn-context-*.xsd`):
- https://docs.oasis-open.org/security/saml/v2.0/saml-2.0-os-xsd.zip
