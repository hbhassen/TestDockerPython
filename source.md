# Documentation d’Intégration – Librairie de Sécurité SAML JASPIC (Wildfly 31 / JDK 17)

## Objectif

Cette documentation explique comment intégrer une librairie de sécurité SAML v2 basée sur JASPIC dans un projet Java EE/Jakarta EE tournant sous Wildfly 31.0.1.Final avec JDK 17.

Elle fournit une solution complète pour :
- L’authentification via un IdP SAML v2.
- La sécurisation des requêtes via JWT (`SAML-Token`) injecté en header.
- La configuration complète de Wildfly et du projet.

## Vue d’Ensemble

La librairie repose sur :
- JASPIC (`javax.security.auth.message`) pour intercepter les requêtes.
- Deux servlets auto-enregistrés :
  - `/saml/login` – redirection vers l’IdP.
  - `/saml/consume` – réception des assertions SAML.
- Validation de l’assertion SAML → génération d’un JWT (`SAML-Token`) injecté dans un header HTTP.
- Le front récupère ce token et l’ajoute aux requêtes suivantes.
- Si le JWT est invalide ou expiré, redirection vers `/saml/login`.

## Fichier de Configuration saml.properties

Placer dans le classpath du projet ou dans `${jboss.server.config.dir}/saml/`.

Exemple :

```
idp.metadata.url=https://idp.example.com/metadata
sp.login.url=https://app.example.com/saml/login
sp.consume.url=https://app.example.com/saml/consume
keystore.path=${jboss.server.config.dir}/saml/sp-keystore.jks
keystore.password=changeit
keystore.alias=spkey
```

## Configuration de Wildfly 31

### 1. Module personnalisé

Créer : `$WILDFLY_HOME/modules/com/example/saml/main/`

Fichier `module.xml` :

```xml
<module xmlns="urn:jboss:module:1.5" name="com.example.saml">
  <resources>
    <resource-root path="opensaml-core-4.0.1.jar"/>
    <resource-root path="opensaml-saml-impl-4.0.1.jar"/>
    <resource-root path="jjwt-api-0.11.2.jar"/>
    <resource-root path="jjwt-impl-0.11.2.jar"/>
    <resource-root path="jjwt-jackson-0.11.2.jar"/>
  </resources>
  <dependencies>
    <module name="javax.api"/>
    <module name="javax.security.auth.message.api"/>
  </dependencies>
</module>
```

### 2. Configuration standalone.xml

Ajouter le security-domain :

```xml
<security-domain name="saml-security" cache-type="default">
  <authentication-jaspi>
    <login-module-stack name="saml-stack">
      <login-module code="com.example.security.saml.SamlLoginModule" flag="required"/>
    </login-module-stack>
    <auth-module code="com.example.security.saml.SamlServerAuthModule" login-module-stack-ref="saml-stack"/>
  </authentication-jaspi>
</security-domain>
```

Puis lier le domaine à votre application :

```xml
<application-security-domains>
  <application-security-domain name="saml-domain" security-domain="saml-security"/>
</application-security-domains>
```

## Configuration de l'application (WAR/EAR)

Si vous utilisez `web.xml` :

```xml
<login-config>
  <auth-method>JASPIC</auth-method>
  <realm-name>saml-security</realm-name>
</login-config>
```

Sinon, vous pouvez utiliser les annotations `@DeclareRoles` et `@RolesAllowed`.

## Dépendances Maven

Ces dépendances doivent être fournies par Wildfly (modules) ou packagées manuellement :

```xml
<dependency>
  <groupId>org.opensaml</groupId>
  <artifactId>opensaml-core</artifactId>
  <version>4.0.1</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.11.2</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.11.2</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.11.2</version>
  <scope>provided</scope>
</dependency>
```

## Cycle d’Authentification SAML

| Étape | Description |
|-------|-------------|
| 1     | L’utilisateur accède à `/saml/login` |
| 2     | Redirection vers l’IdP |
| 3     | Authentification via l’IdP |
| 4     | L’IdP redirige vers `/saml/consume` avec la réponse |
| 5     | Le module valide la réponse, génère un JWT (`SAML-Token`) |
| 6     | Redirection vers `/` avec le header JWT |
| 7     | Le front l’enregistre et le renvoie dans chaque requête |
| 8     | `ServerAuthModule` valide ou redirige si token invalide |

## Test Local

1. Générer un keystore local :

```bash
keytool -genkeypair -alias spkey -keyalg RSA -keystore sp-keystore.jks -keysize 2048
```

2. Copier `sp-keystore.jks` dans `${jboss.server.config.dir}/saml/`

3. Définir correctement les chemins dans `saml.properties`

4. Démarrer Wildfly et déployer le WAR

5. Accéder à `http://localhost:8080/app/saml/login`

## Bonnes Pratiques de Sécurité

- Utiliser HTTPS uniquement.
- Ne pas inclure le keystore dans le WAR.
- Signer les assertions SAML et les JWT.
- Définir une durée courte pour les JWT (ex: 5 minutes).
- Implémenter une vérification de révocation côté serveur si nécessaire.

## Résultat Attendu

- Authentification SAML transparente via un fournisseur externe.
- Sécurisation de session via JWT.
- Interception centralisée avec `ServerAuthModule` compatible JASPIC.
- Projet fonctionnel sous JDK 17 + Wildfly 31.

## Support

Équipe Sécurité Applicative  
Email : security-team@entreprise.com