📄 README – Application Batch MFT

📌 Description

Cette application est un batch Java 17 / Spring Boot permettant de communiquer avec un système MFT (Managed File Transfer) afin de :

Télécharger des fichiers (Download mode).

Uploader des fichiers (Upload mode).

Assurer un mode hybride combinant upload et download.


Elle est hautement configurable via un fichier YAML personnalisé et prend en charge :

Plusieurs flows (flowName) enregistrés côté MFT.

Une configuration de proxy si le MFT est externe (hors réseau bancaire).

Un chiffrement de bout en bout via certificats (SSL/TLS).

La configuration des répertoires de stockage sur le serveur batch.



---

⚙️ Modes d’exécution

L’application supporte 3 modes distincts :

1. Download only

Téléchargement de fichiers depuis le MFT vers les répertoires locaux configurés.

Prise en charge de plusieurs flowName.



2. Upload only

Envoi de fichiers locaux vers le MFT.



3. Hybride

Téléchargement et envoi dans un même batch.





---

🛠️ Configuration (application.yaml)

Exemple de configuration :

batch:
  mode: download   # valeurs possibles: download | upload | hybrid
  proxy:
    enabled: true
    host: proxy.mybank.local
    port: 8080
    username: myuser
    password: mypassword

  mft:
    baseUrl: https://mft.mybank.com/api
    certificate: classpath:certs/mft-client-cert.p12
    certificatePassword: changeit

  flows:
    - name: PAYMENTS_FLOW
      downloadDir: /opt/batch/download/payments
      uploadDir: /opt/batch/upload/payments
    - name: REPORTS_FLOW
      downloadDir: /opt/batch/download/reports
      uploadDir: /opt/batch/upload/reports

🔑 Paramètres principaux

Paramètre	Description

batch.mode	Mode du batch (download, upload, hybrid)
batch.proxy.enabled	Active/désactive le proxy
batch.proxy.host	Adresse du proxy
batch.proxy.port	Port du proxy
batch.proxy.username	Utilisateur proxy (optionnel)
batch.proxy.password	Mot de passe proxy (optionnel)
batch.mft.baseUrl	URL du service MFT
batch.mft.certificate	Chemin du certificat client
batch.mft.certificatePassword	Mot de passe du certificat
batch.flows[].name	Nom du flow MFT
batch.flows[].downloadDir	Répertoire local de téléchargement
batch.flows[].uploadDir	Répertoire local d’upload



---

🚀 Exécution du batch

1️⃣ En ligne de commande

java -jar batch-mft-app.jar --spring.config.location=application.yaml

2️⃣ Avec Maven (plugin spring-boot-maven-plugin)

mvn spring-boot:run -Dspring-boot.run.arguments="--spring.config.location=application.yaml"


---

🏗️ Intégration dans un projet Java EE

L’application peut être packagée et intégrée dans une application Java EE existante via le plugin Maven maven-assembly-plugin.

Exemple de configuration pom.xml :

<build>
  <plugins>
    <!-- Plugin Spring Boot -->
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
    </plugin>

    <!-- Assembly Plugin pour intégration -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-assembly-plugin</artifactId>
      <version>3.6.0</version>
      <configuration>
        <descriptorRefs>
          <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
        <archive>
          <manifest>
            <mainClass>com.mybank.batch.Application</mainClass>
          </manifest>
        </archive>
      </configuration>
      <executions>
        <execution>
          <id>make-assembly</id>
          <phase>package</phase>
          <goals>
            <goal>single</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>

L’artefact généré (batch-mft-app-jar-with-dependencies.jar) peut ensuite être intégré dans l’application Java EE et exécuté comme un composant batch.


---

🔒 Sécurité

Communication sécurisée via SSL/TLS.

Authentification par certificat client (.p12 / .jks).

Support proxy sécurisé (authentification basique).



---

📂 Arborescence projet

batch-mft-app/
├── src/
│   ├── main/java/com/mybank/batch/...
│   └── main/resources/application.yaml
├── certs/
│   └── mft-client-cert.p12
├── target/
│   └── batch-mft-app.jar
└── pom.xml


---

✅ Prérequis

Java 17 installé

Maven 3.8+

Accès au système MFT (certificat + credentials si nécessaire)

Accès réseau au MFT (direct ou via proxy)



