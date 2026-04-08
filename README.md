# enterprise-app-h2

Application Spring Boot generee depuis l archetype `springboot-enterprise-archetype`.


Ce projet fournit une base de depart structuree pour une application entreprise simple avec :

- une API REST
- une couche service
- une couche data basee sur Spring Data JPA
- une configuration Spring centralisee
- un exemple de pipeline Jenkins
- un `Dockerfile` de build et de runtime

Identite du projet genere :

- `groupId` : `com.company.demo`
- `artifactId` : `enterprise-app-h2`
- `version` : `1.0.0-SNAPSHOT`
- `package racine` : `com.company.demo`
- `pile web` : `webmvc`
- `base de donnees` : `h2`
- `nom de base` : `enterprise_h2`
- `schema` : `app_schema`


Le projet suit le decoupage suivant :

- `controller/rest` : recoit les requetes HTTP et retourne les reponses
- `service` : porte la logique d orchestration applicative
- `repository/data` : accede a la base de donnees
- `config` : centralise la configuration technique Spring
- `domaine` : contient les DTO et entites

Flux recommande :

`Controller -> Service -> Repository -> Base de donnees`


Fichiers et dossiers principaux :

```text
.
|-- Dockerfile
|-- Jenkinsfile
|-- pom.xml
|-- src
|   |-- main
|   |   |-- java
|   |   |   `-- com/company/demo
|   |   |       |-- Application.java
|   |   |       |-- config
|   |   |       |-- domaine
|   |   |       |-- repositories
|   |   |       |-- rest
|   |   |       `-- services
|   |   `-- resources
|   |       |-- application.yml
|   |       `-- schema.sql
|   `-- test
|       |-- java
|       `-- resources
```



Chemin :

- `src/main/java/com/company/demo/rest/`

Fichier genere :

- `ApplicationVersionController.java`

Responsabilite :

- exposer les endpoints HTTP
- convertir une demande HTTP en appel de service
- laisser la logique metier hors du controller

Dans ce projet, le controller expose :

- `GET /api/application/version`

Regle a garder :

- pas de logique metier lourde dans cette couche
- pas d acces direct a la base depuis un controller
- le controller appelle toujours un service

Specificite `webmvc` :

- le controller retourne des `ResponseEntity`
- l execution est basee sur la pile servlet Spring MVC


Chemin :

- `src/main/java/com/company/demo/services/`

Fichiers generes :

- `ApplicationInfoService.java`
- `ApplicationInfoServiceImpl.java`

Responsabilite :

- orchestrer les traitements applicatifs
- appeler un ou plusieurs repositories
- transformer les entites en DTO
- definir les frontieres metier de l application

Regle a garder :

- la couche service ne gere pas directement HTTP
- la couche service ne depend pas de Swagger
- la couche service porte la logique metier et les validations applicatives


Chemin :

- `src/main/java/com/company/demo/repositories/`

Fichier genere :

- `ApplicationMetadataRepository.java`

Responsabilite :

- lire et ecrire les donnees en base
- declarer les methodes Spring Data JPA
- contenir les requetes derivees, JPQL ou natives si necessaire

Regle a garder :

- pas de logique HTTP
- pas d orchestration metier complexe
- garder ici uniquement la logique d acces aux donnees


Chemins :

- `src/main/java/com/company/demo/domaine/entities/`
- `src/main/java/com/company/demo/domaine/dto/`

Fichiers generes :

- `ApplicationMetadataEntity.java`
- `ApplicationVersionResponse.java`

Responsabilite :

- `entities` : modeliser les objets persistants JPA
- `dto` : modeliser les objets echanges avec l exterieur

Bonne pratique :

- ne retournez pas directement vos entites JPA dans les controllers
- passez par des DTO quand l API commence a evoluer


Chemin :

- `src/main/java/com/company/demo/config/`

Fichiers generes :

- `ApplicationMetadataInitializer.java`
- `ApplicationMetadataProperties.java`
- `OpenApiConfig.java`
- `SecurityConfig.java`

Responsabilite :

- configurer la securite
- configurer OpenAPI
- declarer les proprietes applicatives
- initialiser les donnees techniques de depart

Regle a garder :

- les classes de configuration restent techniques
- elles ne remplacent pas la couche service


Chemin :

- `src/main/resources/application.yml`

Ce fichier centralise la configuration runtime du service.

Sections principales :


- configure le port HTTP du service
- valeur par defaut : `8080`


- definit le nom logique de l application
- reutilise dans les logs et l observabilite


- definit le type d application web

Pour `webmvc` :

- `web-application-type: servlet`


- configure la connexion a la base

Dans ce projet genere :

- la datasource pointe vers une base H2 en memoire
- ce choix est adapte au developpement local rapide


- active les reglages Hibernate et JPA
- `ddl-auto: update` facilite le demarrage local
- `open-in-view: false` garde des frontieres de transaction explicites


- execute `schema.sql` au demarrage
- permet de creer le schema avant l execution de JPA


- expose les endpoints Actuator minimaux
- par defaut : `health` et `info`


- fournit les metadonnees exposees par Actuator `info`


- contient les metadonnees lues par le endpoint d exemple
- cette section est reliee a `ApplicationMetadataProperties`


- active OpenAPI
- documentation JSON : `/v3/api-docs`
- interface Swagger UI : `/swagger-ui.html`


Le `Dockerfile` genere est un build multi-stage :


- image utilisee : `maven:3.9.11-eclipse-temurin-21`
- copie `pom.xml` et `src`
- lance `mvn -B clean package -DskipTests`

But :

- construire le jar dans un environnement Maven complet


- image utilisee : `eclipse-temurin:21-jre`
- copie le jar construit depuis l etape builder
- expose le port `8080`
- lance `java -jar /app/app.jar`

But :

- garder une image finale plus legere
- ne pas embarquer Maven dans l image de runtime


Le `Jenkinsfile` fournit un pipeline simple avec les etapes suivantes :


- recupere le code depuis le SCM Jenkins


- lance `mvn -B clean compile`


- lance `mvn -B test`


- lance `mvn -B package -DskipTests`


- construit une image Docker taggee `enterprise-app-h2:latest`

Le pipeline gere les environnements Unix et Windows via `sh` ou `bat`.



Depuis la racine du projet genere :

```powershell
mvn -B spring-boot:run
```


```powershell
mvn -B test
```


```powershell
mvn -B clean package
```


```powershell
docker build -t enterprise-app-h2:latest .
```


```powershell
docker run --rm -p 8080:8080 enterprise-app-h2:latest
```


- API exemple : `http://localhost:8080/api/application/version`
- Swagger UI : `http://localhost:8080/swagger-ui.html`
- Health : `http://localhost:8080/actuator/health`



Ajoutez un fichier dans :

- `src/main/java/com/company/demo/rest/`

Exemple :

- `CustomerController.java`

Regle :

- un controller appelle un service, pas un repository directement


Ajoutez vos fichiers dans :

- `src/main/java/com/company/demo/services/`

Exemple :

- `CustomerService.java`
- `CustomerServiceImpl.java`

Regle :

- le service porte l orchestration metier


Ajoutez un fichier dans :

- `src/main/java/com/company/demo/repositories/`

Exemple :

- `CustomerRepository.java`

Regle :

- etendez `JpaRepository` ou un repository Spring Data approprie


Ajoutez un fichier dans :

- `src/main/java/com/company/demo/domaine/entities/`

Exemple :

- `CustomerEntity.java`

Regle :

- mappez la table, les colonnes et les relations ici


Ajoutez un fichier dans :

- `src/main/java/com/company/demo/domaine/dto/`

Exemple :

- `CustomerResponse.java`
- `CreateCustomerRequest.java`

Regle :

- utilisez les DTO pour l entree et la sortie de l API


Ajoutez un fichier dans :

- `src/main/java/com/company/demo/config/`

Exemple :

- `CustomerProperties.java`
- `CustomerBatchConfig.java`

Regle :

- placez ici les `@Configuration`, `@ConfigurationProperties` et beans techniques


Ajoutez ou modifiez les fichiers dans :

- `src/main/resources/`

Exemple :

- `application.yml`
- `schema.sql`

Regle :

- gardez les proprietes techniques dans `application.yml`
- si vous ajoutez de nouvelles proprietes, mappez-les vers une classe de config dediee


Pour ajouter un nouveau cas d usage `Customer` :

1. creez `CustomerEntity` dans `domaine/entities`
2. creez `CustomerRepository` dans `repositories`
3. creez `CustomerService` et `CustomerServiceImpl` dans `services`
4. creez `CustomerResponse` et vos DTO d entree dans `domaine/dto`
5. creez `CustomerController` dans `rest`
6. ajoutez les proprietes necessaires dans `application.yml`
7. ajoutez les tests associes dans `src/test/java`


- gardez la logique metier dans les services
- gardez l acces base dans les repositories
- gardez les controllers fins
- gardez les classes de configuration purement techniques
- externalisez les secrets de base de donnees pour les environnements reels


Le projet genere est volontairement simple mais deja structure pour evoluer.

Point d entree principal :

- `Application.java`

Fichier de configuration central :

- `src/main/resources/application.yml`

Points de depart pour developper :

- `rest/` pour exposer l API
- `services/` pour la logique applicative
- `repositories/` pour la persistence
- `config/` pour la configuration technique
