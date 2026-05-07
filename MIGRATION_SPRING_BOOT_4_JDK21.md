# Guide de migration des applications vers Spring Boot 4.x et JDK 21

## 1) Contexte

Ce document décrit la migration des projets de l’organisme depuis **Spring Boot 3.x / JDK 17** vers **Spring Boot 4.x / JDK 21**, en s’appuyant sur notre **BOM parent interne**.

Objectifs :
- standardiser les versions de dépendances via le parent BOM,
- garantir la conformité sécurité (pas de dépendances vulnérables / non autorisées),
- faciliter la migration applicative des équipes projet.

---

## 2) Liens de référence (à adapter)

> Remplacer ces URLs par vos liens réels.

- **BOM parent interne sur Artifactory** :  
  `https://artifactory.example.com/artifactory/maven-release-local/com/organisme/platform-parent-bom/4.0.6/platform-parent-bom-4.0.6.pom`
- **Release Notes du BOM interne sur Confluence** :  
  `https://confluence.example.com/display/PLATFORM/Release+Notes+BOM+4.0.6`

Références officielles Spring Boot utilisées pour ce guide :
- BOM officiel Spring Boot 4.0.6 :  
  `https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.6/spring-boot-dependencies-4.0.6.pom`

---

## 3) Récapitulatif des dépendances et spécifications cibles

### 3.1 Plateforme cible

- **Java cible** : **JDK 21** (standard interne de migration).
- **Framework cible** : **Spring Boot 4.0.6**.
- **Gestion des versions** : centralisée dans le **BOM parent interne**, lui-même aligné sur `spring-boot-dependencies:4.0.6`.

### 3.2 Exemples de versions clés (issues du BOM officiel Spring Boot 4.0.6)

> Liste non exhaustive, à compléter avec vos dépendances internes et socle de test.

- Spring Framework : `7.0.7`
- Spring Security : `7.0.5`
- Spring Data BOM : `2025.1.5`
- Hibernate ORM : `7.2.12.Final`
- Jakarta Servlet : `6.1.0`
- Jakarta JMS : `3.1.0`
- Jakarta Persistence : `3.2.0`
- Jakarta Validation : `3.1.1`
- Jackson BOM : `3.1.2`
- Tomcat (embarqué) : `11.0.21`
- Jetty : `12.1.8`
- Micrometer : `1.16.5`
- JUnit Jupiter : `6.0.3`
- Mockito : `5.20.0`
- Testcontainers : `2.0.5`

### 3.3 Impacts techniques structurants

- Passage à l’écosystème **Spring 7** (API, comportements, auto-configurations).
- Alignement sur les API **Jakarta** gérées par Boot 4.
- Montée de versions majeure des bibliothèques de test (JUnit 6, etc.).
- Potentiels impacts de compatibilité sur frameworks annexes (cloud, sécurité, persistence, starters internes).

---

## 4) Préparation de la migration

### 4.1 Prérequis

- Installer JDK 21 sur les postes développeurs et agents CI.
- Vérifier Maven (version compatible avec Java 21 et votre usine logicielle).
- Vérifier pipelines CI/CD, images Docker de build et runtime (base JDK 21).
- S’assurer de la disponibilité du BOM interne cible dans Artifactory.

### 4.2 Inventaire projet (avant changement)

- Version actuelle Spring Boot.
- Version Java (source/target/release).
- Dépendances déclarées **avec version explicite** (à réduire au minimum).
- Dépendances interdites/non validées sécurité.
- Tests existants (unitaires, intégration, non-régression).

---

## 5) Étapes de migration (pas à pas)

## 5.1 Mettre à jour le parent POM

Dans le `pom.xml` projet :

```xml
<parent>
  <groupId>com.organisme</groupId>
  <artifactId>platform-parent-bom</artifactId>
  <version>4.0.6</version>
  <relativePath/>
</parent>
```

> Adapter `groupId/artifactId/version` selon votre nomenclature interne.

## 5.2 Forcer Java 21

Vérifier/positionner :

```xml
<properties>
  <java.version>21</java.version>
  <maven.compiler.release>21</maven.compiler.release>
</properties>
```

## 5.3 Nettoyer les versions explicites de dépendances

- Retirer les `<version>` sur les dépendances déjà gérées par le BOM.
- Conserver une version explicite seulement si **exception validée** par la gouvernance technique.

## 5.4 Adapter les dépendances de test

- S’aligner sur le socle test fourni par le BOM (JUnit, Mockito, Testcontainers, etc.).
- Vérifier extensions custom (base de données de test, wiremock, sécurité).

## 5.5 Mettre à jour plugins Maven

- S’assurer que `maven-compiler-plugin`, `maven-surefire-plugin`, `maven-failsafe-plugin`, `spring-boot-maven-plugin` sont compatibles Java 21 / Boot 4.
- Prioriser les versions pilotées par le parent BOM.

---

## 6) Points d’attention de compatibilité

- API dépréciées/supprimées entre Spring Boot 3.x et 4.x.
- Changements potentiels de configuration auto des starters.
- Évolutions Hibernate 7 (mapping, dialectes, requêtes natives).
- Évolutions Spring Security 7 (config HTTP, filtres, tests sécurité).
- Comportement Actuator / Observabilité (Micrometer, endpoints exposés).

---

## 7) Validation après migration

Checklist minimale :

- Build Maven complet sans erreur.
- Tous les tests unitaires/intégration passent.
- Démarrage local OK avec profil standard.
- Vérification endpoints critiques (santé, auth, métier).
- Vérification logs, métriques et traces.
- Scan sécurité dépendances (SCA) conforme.
- Vérification image Docker runtime JDK 21.

Commandes recommandées :

```bash
mvn -U -DskipTests clean verify
mvn test
mvn -DskipTests spring-boot:run
```

---

## 8) Stratégie de déploiement recommandée

- Migration par lot d’applications (pilotage progressif).
- Exécuter d’abord sur environnements de qualification.
- Prévoir fenêtre d’observation renforcée post-MEP.
- Prévoir rollback applicatif (version précédente artefact + config).

---

## 9) Modèle de communication projet

Pour chaque application migrée, publier :
- version source (Boot 3.x / JDK 17),
- version cible (Boot 4.x / JDK 21),
- version BOM interne utilisée,
- écarts de dépendances notables,
- incidents rencontrés et correctifs,
- statut final (OK / KO / sous surveillance).

---

## 10) Annexe — Template court pour équipes projet

1. Mettre à jour parent BOM vers `4.0.6`.
2. Passer Java en `21`.
3. Supprimer versions explicites redondantes dans `dependencies`.
4. Lancer `mvn clean verify`.
5. Corriger incompatibilités compilation/tests.
6. Valider démarrage + endpoints critiques.
7. Publier le compte-rendu de migration.

