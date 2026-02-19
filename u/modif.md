# Modifications SmalLib (GET /login/saml2/sso/acs -> IdP -> POST ACS -> redirect front)

## Fichiers modifies
- `src/main/java/com/hmiso/saml/integration/SamlJakartaFilter.java` : accepte GET sur l'ACS et demarre un AuthnRequest vers l'IdP.
- `src/main/java/com/hmiso/saml/integration/SamlAuthenticationFilterHelper.java` : ajout du flux GET ACS (initiation auth), emission du JWT sur reponse ACS, fallback de redirection vers le front.
- `src/main/java/com/hmiso/saml/integration/SamlAuthenticationFilterConfig.java` : ajoute `postLoginRedirectUrl` (URL front cible apres ACS).
- `src/main/java/com/hmiso/saml/integration/SamlBootstrapListener.java` : propage l'URL front dans la config du filtre.
- `src/main/java/com/hmiso/saml/integration/SamlAppConfiguration.java` : nouvelle propriete `frontRedirectUrl`.
- `src/main/java/com/hmiso/saml/integration/SamlAppYamlConfigLoader.java` : lecture de `app.front-redirect-url` dans le YAML.
- `src/test/java/com/hmiso/saml/integration/SamlJakartaFilterTest.java` : test du GET ACS.
- `src/test/java/com/hmiso/saml/integration/SamlAppYamlConfigLoaderTest.java` : test du parsing de `front-redirect-url`.
- `docs/LIBRARY.md` : documentation de `front-redirect-url`.
- `examples/demo2/src/main/resources/saml-config.yml` : exemple avec `front-redirect-url`.

## Exemple commente de `saml-config.yml`
```yaml
# Configuration applicative (Servlet / SmalLib)
app:
  # Chemins proteges (prefixes automatiquement par le context path du WAR)
  protected-paths:
    - "/api/*"

  # URL front cible apres ACS (GET /login/saml2/sso/acs -> IdP -> POST ACS -> redirect)
  front-redirect-url: "http://localhost:4200"

  # Cle de session pour le principal
  session-attribute-key: "saml.principal"
  server-session-attribute-key: "saml.server.session"

  # Durée max de session (minutes)
  session-max-minutes: 60

  # Page d'erreur SAML (prefixe par context path)
  error-path: "/saml/error"

  # TTL relay-state (minutes)
  relay-state-ttl-minutes: 5

  # CORS (optionnel)
  cors-enabled: true
  cors-allowed-origins:
    - "http://localhost:4200"
  cors-allowed-methods:
    - "GET"
    - "POST"
    - "OPTIONS"
  cors-allowed-headers:
    - "Authorization"
    - "Content-Type"
  cors-expose-headers:
    - "X-Auth-Token"
  cors-allow-credentials: true

  # Bloquer la navigation navigateur sur l'API (optionnel)
  block-browser-navigation: true
  # Activer JASPIC (optionnel)
  jaspic-enabled: true

service-provider:
  entity-id: "saml-sp"
  base-url: "http://localhost:8080"
  acs-path: "/login/saml2/sso/acs"
  slo-path: "/logout/saml"
  name-id-format: "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"
  authn-request-binding: "HTTP_POST"
  want-assertions-signed: true
  supported-name-id-formats:
    - "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"

identity-provider:
  entity-id: "saml-realm"
  single-sign-on-service-url: "https://localhost:8443/realms/saml-realm/protocol/saml"
  single-logout-service-url: "https://localhost:8443/realms/saml-realm/protocol/saml"
  metadata-url: "https://localhost:8443/realms/saml-realm/protocol/saml/descriptor"
  want-assertions-signed: true
  want-messages-signed: true
  supported-bindings:
    - "HTTP_POST"
    - "HTTP_REDIRECT"

security:
  clock-skew: "PT2M"
  signature-algorithm: "rsa-sha256"
  digest-algorithm: "sha256"
  encryption-algorithm: "aes256"
  force-https-redirect: false
  enable-detailed-logging: true
  jwt-secret: "change-me"
  jwt-ttl-seconds: 10
```
