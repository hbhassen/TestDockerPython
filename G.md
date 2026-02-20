# guide-devops.md

But
- Guide complet (dev + ops) pour integrer SmalLib dans une application JEE (WAR ou EAR) sur WildFly.
- JASPIC est obligatoire pour toutes les applications.
- La librairie est fournie par le JFrog de l'organisation (pas de recompilation SmalLib).

Description rapide de SmalLib
- SP SAML 2.0 minimaliste.
- Helpers Servlet pour WAR (listener + filtres) et flux SAML (AuthnRequest, Response, SLO).
- Chargement YAML unique `saml-config.yml`.
- JASPIC (Bearer JWT) pour l'integration roles/identity cote conteneur.

Prerequis
- Java 17.
- Maven.
- WildFly 31.
- IdP SAML 2.0 configure (Keycloak ou autre).
- Acces au repository JFrog de l'organisation.

--------------------------------------------------------------------------------

## 1) Dependances Maven (compile)

Le WAR compile contre SmalLib via Maven, avec scope `provided` (runtime via module WildFly).

Exemple dans `pom.xml` :
```xml
<dependency>
  <groupId>com.hmiso</groupId>
  <artifactId>smalib</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
```

Configurer aussi l'acces au repository JFrog dans votre `settings.xml` ou `pom.xml` (pas d'URL ici).

--------------------------------------------------------------------------------

## 2) Installation du module WildFly (runtime)

Recuperer les artefacts depuis JFrog puis creer le module :

1) Copier les JARs dans le module :
```powershell
New-Item -ItemType Directory -Force C:\wildfly-31.0.1.Final\modules\com\hmiso\smalib\main | Out-Null
# Remplacer <JFROG_JAR_PATH> par le chemin du jar telecharge depuis JFrog
Copy-Item <JFROG_JAR_PATH> C:\wildfly-31.0.1.Final\modules\com\hmiso\smalib\main\smalib-0.1.0-SNAPSHOT.jar -Force
Copy-Item C:\Users\hamdi\.m2\repository\org\yaml\snakeyaml\2.2\snakeyaml-2.2.jar C:\wildfly-31.0.1.Final\modules\com\hmiso\smalib\main\snakeyaml-2.2.jar -Force
```

2) Creer `module.xml` :
```xml
<?xml version="1.0" encoding="UTF-8"?>
<module name="com.hmiso.smalib" xmlns="urn:jboss:module:1.9">
  <resources>
    <resource-root path="smalib-0.1.0-SNAPSHOT.jar"/>
    <resource-root path="snakeyaml-2.2.jar"/>
  </resources>
  <dependencies>
    <module name="jakarta.servlet.api"/>
    <module name="org.slf4j"/>
    <module name="java.desktop"/>
  </dependencies>
</module>
```

--------------------------------------------------------------------------------

## 3) Declaration du module dans le WAR

Ajouter la dependance module WildFly :

`WEB-INF/jboss-deployment-structure.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jboss-deployment-structure>
  <deployment>
    <dependencies>
      <module name="com.hmiso.smalib"/>
    </dependencies>
  </deployment>
</jboss-deployment-structure>
```

--------------------------------------------------------------------------------

## 4) Declaration des filtres et listener (web.xml)

Exemple minimal :
```xml
<listener>
  <listener-class>com.hmiso.saml.integration.SamlBootstrapListener</listener-class>
</listener>

<filter>
  <filter-name>SamlServerSessionFilter</filter-name>
  <filter-class>com.hmiso.saml.integration.SamlServerSessionFilter</filter-class>
</filter>
<filter-mapping>
  <filter-name>SamlServerSessionFilter</filter-name>
  <url-pattern>/*</url-pattern>
</filter-mapping>

<filter>
  <filter-name>SamlJakartaFilter</filter-name>
  <filter-class>com.hmiso.saml.integration.SamlJakartaFilter</filter-class>
</filter>
<filter-mapping>
  <filter-name>SamlJakartaFilter</filter-name>
  <url-pattern>/*</url-pattern>
</filter-mapping>

<!-- JASPIC exige une auth-method pour activer l'authentification proactive -->
<login-config>
  <auth-method>BASIC</auth-method>
</login-config>
```

Optionnel selon besoins : `CorsFilter`, `ApiNavigationFilter`, `SamlErrorServlet`.

--------------------------------------------------------------------------------

## 5) Configuration JASPIC WildFly (standalone.xml)

JASPIC est obligatoire pour toutes les applications. Les sections suivantes doivent etre modifiees.

### 5.1 Undertow : activer JASPIC et l'auth proactive
Dans le subsystem Undertow :
```xml
<application-security-domain name="other"
                             http-authentication-factory="application-http-authentication"
                             enable-jaspi="true"
                             integrated-jaspi="true"/>

<servlet-container name="default" proactive-authentication="true">
  <jsp-config/>
  <websockets/>
</servlet-container>
```

### 5.2 Elytron : HTTP authentication factory
Dans le subsystem Elytron :
```xml
<http-authentication-factory name="application-http-authentication"
                             security-domain="ApplicationDomain"
                             http-server-mechanism-factory="global">
  <mechanism-configuration>
    <mechanism mechanism-name="BASIC">
      <mechanism-realm realm-name="ApplicationRealm"/>
    </mechanism>
  </mechanism-configuration>
</http-authentication-factory>
```

### 5.3 Elytron : configuration JASPIC SmalLib
Toujours dans Elytron :
```xml
<jaspi>
  <jaspi-configuration name="smalib-jaspi"
                       layer="HttpServlet"
                       application-context="*"
                       description="SmalLib JWT JASPIC">
    <server-auth-modules>
      <server-auth-module class-name="com.hmiso.saml.integration.SamlJwtServerAuthModule"
                          module="com.hmiso.smalib"
                          flag="REQUIRED"/>
    </server-auth-modules>
  </jaspi-configuration>
</jaspi>
```

Apres modification : redemarrer WildFly.

--------------------------------------------------------------------------------

## 6) Fichier `saml-config.yml`

### Exemple commente
```yaml
app:
  protected-paths:
    - "/api/*"

  # URL front cible apres ACS (si RelayState absent)
  front-redirect-url: "http://localhost:4200"

  session-attribute-key: "saml.principal"
  server-session-attribute-key: "saml.server.session"

  # Duree session max (minutes)
  session-max-minutes: 60

  # Page erreur SAML (prefixee par context path)
  error-path: "/saml/error"

  # TTL relay-state (minutes)
  relay-state-ttl-minutes: 5

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

  block-browser-navigation: true
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
  single-sign-on-service-url: "https://idp.example.com/protocol/saml"
  single-logout-service-url: "https://idp.example.com/protocol/saml"
  metadata-url: "https://idp.example.com/descriptor"
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

### Description complete des proprietes

Section `app`
- `protected-paths` (requis) : liste de chemins proteges (exact ou prefixe `/*`). Si une requete matche, le filtre declenche l'auth SAML et verifie le JWT.
- `front-redirect-url` (optionnel) : URL absolue (schema + host) vers le front apres ACS si aucun RelayState valide. Eviter de pointer vers l'ACS pour ne pas boucler.
- `session-attribute-key` (optionnel) : cle de session HTTP sous laquelle le `SamlPrincipal` est stocke. Utilisee par les ressources applicatives.
- `server-session-attribute-key` (optionnel) : cle de session pour l'identifiant de session serveur SAML, utilise pour le JWT et l'expiration.
- `session-max-minutes` (optionnel, minutes) : duree max de session serveur. L'expiration effective est le minimum entre cette valeur et `NotOnOrAfter` de l'assertion.
- `error-path` (optionnel) : path local de l'endpoint d'erreur SAML. Le context path est ajoute automatiquement.
- `relay-state-ttl-minutes` (optionnel, minutes) : duree de vie d'un RelayState cote serveur. Au dela, la redirection vers l'URL initiale est ignoree.
- `cors-enabled` (optionnel) : active l'emission des en-tetes CORS. Si absent, il est active automatiquement quand `cors-allowed-origins` est defini.
- `cors-allowed-origins` (optionnel) : liste d'origines autorisees (schema + host + port). Si vide, CORS est desactive.
- `cors-allowed-methods` (optionnel) : methodes HTTP autorisees en CORS. Defaut `GET, POST, OPTIONS`.
- `cors-allowed-headers` (optionnel) : headers autorises en CORS.
- `cors-expose-headers` (optionnel) : headers exposes au navigateur (utile pour lire `X-Auth-Token`).
- `cors-allow-credentials` (optionnel) : permet l'envoi de cookies/credentials. Si `true`, eviter `*` dans `cors-allowed-origins`.
- `block-browser-navigation` (optionnel) : si `true`, bloque la navigation navigateur (GET/HEAD HTML) sur l'API et renvoie `401` JSON.
- `jaspic-enabled` (requis dans ce guide) : enregistre le provider JASPIC au demarrage. Necessite la config JASPIC dans `standalone.xml`.

Section `service-provider`
- `entity-id` (requis) : identifiant SP (Issuer). Doit correspondre au client declare cote IdP.
- `base-url` (requis) : URL publique du SP (schema + host + port). Utilisee pour construire ACS/SLO. Mettre l'URL externe si reverse-proxy.
- `acs-path` (requis) : path ACS relatif (commence par `/`). Combine avec le context path du WAR.
- `slo-path` (requis) : path SLO relatif (commence par `/`). Combine avec le context path.
- `name-id-format` (requis) : format NameID demande dans l'AuthnRequest (ex: emailAddress).
- `authn-request-binding` (requis) : binding utilise pour envoyer l'AuthnRequest (`HTTP_POST` ou `HTTP_REDIRECT`). Doit etre supporte par l'IdP.
- `want-assertions-signed` (requis) : indique que le SP attend des assertions signees. Valeur de contrat partagee avec l'IdP.
- `supported-name-id-formats` (optionnel) : liste de formats acceptes par le SP. Si absent, `name-id-format` est utilise.

Section `identity-provider`
- `entity-id` (requis) : identifiant IdP attendu dans l'Issuer des reponses.
- `single-sign-on-service-url` (requis) : endpoint SSO IdP utilise pour les AuthnRequest.
- `single-logout-service-url` (optionnel) : endpoint SLO IdP si vous utilisez le logout SAML.
- `metadata-url` (optionnel) : URL de metadata IdP (utile pour audit/diagnostic ou loaders alternatifs).
- `want-assertions-signed` (requis) : si `true`, SmalLib rejettera les assertions non signees.
- `want-messages-signed` (requis) : si `true`, SmalLib exigera une signature sur la reponse SAML ou l'assertion selon la politique.
- `supported-bindings` (requis) : bindings supportes par l'IdP. Doit inclure celui utilise dans `authn-request-binding`.

Section `security`
- `clock-skew` (requis, duree ISO-8601) : tolere les ecarts d'horloge dans `NotBefore/NotOnOrAfter`. Ex: `PT30S` (30 s), `PT2M` (2 min), `PT1H` (1 h).
- `signature-algorithm` (requis) : algorithme de signature attendu/annonce (ex: `rsa-sha256`). Doit etre coherent avec l'IdP.
- `digest-algorithm` (requis) : algorithme de hachage associe (ex: `sha256`).
- `encryption-algorithm` (optionnel) : algorithme de chiffrement (ex: `aes256`) si vous utilisez l'encryption.
- `force-https-redirect` (optionnel) : si `true`, SmalLib exige que les URLs ACS/SLO soient en HTTPS.
- `enable-detailed-logging` (optionnel) : active des logs plus verbeux pour debug SAML.
- `jwt-secret` (optionnel) : secret HMAC pour le JWT. A externaliser hors du code en prod.
- `jwt-ttl-seconds` (optionnel, secondes) : duree de validite du JWT (s). Ex: `10` = 10 s.

--------------------------------------------------------------------------------

## 7) Surcharge du YAML sans recompilation

Ordre de resolution :
- `context-param` `saml.config.path` (par application).
- Propriete systeme `-Dsaml.config.path=...` ou `standalone.xml`.
- Variable d'environnement `SAML_CONFIG_PATH`.
- Ressource embarquee `/saml-config.yml` dans le WAR.

Exemple `web.xml` :
```xml
<context-param>
  <param-name>saml.config.path</param-name>
  <param-value>C:\config\saml-config.yml</param-value>
</context-param>
```

Exemple `standalone.xml` :
```xml
<system-properties>
  <property name="saml.config.path" value="C:\config\saml-config.yml"/>
</system-properties>
```

Note : un redemarrage ou redeploiement est requis pour recharger la configuration.

--------------------------------------------------------------------------------

## 8) Validation rapide
- `GET /demo2/login/saml2/sso/acs` redirige vers l'IdP.
- Apres login IdP, POST ACS puis redirection vers `front-redirect-url`.
- Si JWT actif, le header `X-Auth-Token` est ajoute.
