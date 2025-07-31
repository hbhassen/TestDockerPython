📦 MFT Mock Server
Cette application est un serveur mock développé avec Spring Boot 3 et JDK 17, permettant de simuler un système MFT (Managed File Transfer).
Elle expose plusieurs endpoints REST pour générer un token d’accès OAuth2, uploader, lister, télécharger et envoyer des fichiers, ainsi que consulter leurs détails.

🚀 1. Prérequis
Avant de commencer, assurez-vous d'avoir les éléments suivants installés :

Java JDK 17 (Télécharger ici)

Maven 3.8+ (Télécharger ici)

Git (optionnel, pour cloner le projet)

cURL ou Postman pour tester les endpoints

⚙️ 2. Installation
2.1. Cloner le projet
bash
Copier
Modifier
git clone https://github.com/votre-repo/mft-mock-server.git
cd mft-mock-server
2.2. Compiler le projet
bash
Copier
Modifier
mvn clean package
▶️ 3. Exécution
Démarrer l’application Spring Boot :

bash
Copier
Modifier
mvn spring-boot:run
Ou lancer le JAR généré :

bash
Copier
Modifier
java -jar target/mft-mock-server-0.0.1-SNAPSHOT.jar
L’application démarre sur http://localhost:9999

📂 4. Endpoints disponibles
4.1. Générer un Access Token
URL : POST /invoke/oauth2/getAccessToken

Body (JSON) :

json
Copier
Modifier
{
  "client_id": "my-client",
  "client_secret": "my-secret",
  "grant_type": "client_credentials"
}
Réponse (200) :

json
Copier
Modifier
{
  "access_token": "2f4c6f2b-5243-4e13-a7c9-8789e9845c5e",
  "token_type": "Bearer",
  "expires_in": 3600,
  "issued_at": "2025-07-31T10:30:12.345+02:00"
}
4.2. Upload d'un fichier
URL : POST /gw/transferFile/v1/file

Headers :

Content-Type: application/octet-stream

File_Name: test.zip

Body : contenu binaire du fichier

Réponse (201) :

json
Copier
Modifier
{
  "id": "1",
  "fileName": "test.zip",
  "creationDate": "2025-07-31T10:30:12.345+02:00",
  "expirationDate": "2025-08-01T10:30:12.345+02:00",
  "operationType": "UPLOAD",
  "downloadCont": 0,
  "spReference": "f8bce59b-3824-4b4a-9e36-d5a7d534ea3b",
  "spFlowName": "equipeTest",
  "spMessage": "document uploader par : equipeTest",
  "audit": [
    {
      "username": "testeur1",
      "date": "2025-07-31T10:30:12.345+02:00",
      "action": "upload",
      "detail": "document uploader par l'equipe: equipeTest"
    }
  ]
}
Le fichier et son fichier .json sont stockés dans un dossier correspondant à spFlowName.

4.3. Lister les fichiers
URL : GET /gw/transferFile/v1/files?operation=upload&include_processed=true

Réponse : tableau de JSON représentant les fichiers valides et non expirés.

4.4. Envoyer un fichier (Submit)
URL : POST /gw/transferFile/v1/file/{file-id}/send

Body (JSON) :

json
Copier
Modifier
{
  "spFlowName": "equipeTest",
  "spMessage": "document submited par : equipeTest",
  "fileObjectId": "1"
}
4.5. Télécharger un fichier
URL : GET /gw/transferFile/v1/file/{file-id}/data

Réponse : fichier binaire et incrémentation du compteur downloadCont.

4.6. Détails d'un fichier
URL : GET /gw/transferFile/v1/file/{file-id}

Réponse : JSON détaillant les informations du fichier.

🛠️ 5. Configuration
Fichier application.yml :

yaml
Copier
Modifier
server:
  port: 9999

spring:
  application:
    name: mft-mock-server
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
✅ 6. Notes
Le stockage est fait en local dans des dossiers portant le nom spFlowName.

Chaque fichier uploaded crée également un fichier .json contenant ses métadonnées.

Les tokens générés sont uniquement mockés, sans réelle authentification OAuth2.
