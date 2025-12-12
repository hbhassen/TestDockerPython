# XL Deploy REST API – Endpoints utilises (Inputs / Outputs)

Ce document decrit uniquement les endpoints XL Deploy officiellement documentes utilises pour :
- verifier l’existence d’un DAR dans le repository
- connaitre la version deployee sur un environnement PROD

Base URL type : `https://xldeploy.example.com:4517/deployit`

Authentification :
- HTTP Basic Auth (compte technique read-only suffisant)
- HTTPS obligatoire

Headers standards :
- `Authorization: Basic base64(username:password)`
- `Accept: application/xml`
- `Content-Type: application/xml`

---

## 1) Verifier si un DAR existe dans le repository

### Endpoint
`GET /repository/exists/{ciId}`

### Description
Verifie si un Configuration Item (CI) existe dans le repository XL Deploy. Un DAR correspond a un CI de type `udm.DeploymentPackage` (udm.Version).

Convention d’ID : `Applications/{applicationName}/{version}`

### Input
- URL : `GET /repository/exists/Applications/MyApp-ear/1.2.3`
- Body : aucun

### Output (XML natif XL Deploy)
```xml
<boolean>true</boolean>
```
ou
```xml
<boolean>false</boolean>
```

### Output (equivalent JSON logique)
```json
{ "exists": true }
```
ou
```json
{ "exists": false }
```

### Cas d’usage
- Verifier qu’un DAR pousse par Jenkins est bien disponible dans XL Deploy.
- Eviter toute requete de deploiement ou de comparaison sur un package inexistant.

---

## 2) Lire un DeployedApplication (version deployee sur un environnement)

### Endpoint
`GET /repository/ci/{ciId}`

### Description
Lit un Configuration Item depuis le repository XL Deploy. Utilise ici pour un `udm.DeployedApplication`.

Convention d’ID standard : `Environments/{environmentName}/{applicationName}`

Exemple PROD : `Environments/PROD/MyApp-ear`

### Input
- URL : `GET /repository/ci/Environments/PROD/MyApp-ear`
- Body : aucun

### Output (XML natif XL Deploy)
```xml
<udm.DeployedApplication id="Environments/PROD/MyApp-ear">
    <version ref="Applications/MyApp-ear/1.1.0"/>
    <environment ref="Environments/PROD"/>
</udm.DeployedApplication>
```

### Output (equivalent JSON logique)
```json
{
  "id": "Environments/PROD/MyApp-ear",
  "version": { "ref": "Applications/MyApp-ear/1.1.0" },
  "environment": { "ref": "Environments/PROD" }
}
```

### Cas particulier : application non deployee sur PROD
- Reponse HTTP : `404 Not Found`
- Interpretation metier :
```json
{ "deployed": false, "prodVersion": null }
```

---

## 3) Logique metier construite a partir des endpoints

### Entree metier (ex Jenkins)
```json
{
  "applicationName": "MyApp-ear",
  "darName": "myapp-1.2.3.dar",
  "version": "1.2.3"
}
```

### Traitement
1. Verifier existence du DAR : `Applications/MyApp-ear/1.2.3`
2. Lire le DeployedApplication sur PROD : `Environments/PROD/MyApp-ear`
3. Comparer version Jenkins vs version PROD

### Sorties metier
Cas 1 : meme version en PROD
```json
{
  "applicationName": "MyApp-ear",
  "darName": "myapp-1.2.3.dar",
  "version": "1.2.3",
  "isProd": true,
  "versionProd": "1.2.3"
}
```

Cas 2 : version differente en PROD
```json
{
  "applicationName": "MyApp-ear",
  "darName": "myapp-1.2.3.dar",
  "version": "1.2.3",
  "isProd": false,
  "versionProd": "1.1.0"
}
```

Cas 3 : non deployee en PROD
```json
{
  "applicationName": "MyApp-ear",
  "darName": "myapp-1.2.3.dar",
  "version": "1.2.3",
  "isProd": false,
  "versionProd": null
}
```

---

## 4) Resume des endpoints utilises

| Endpoint                         | Methode | Usage                            |
|----------------------------------|---------|----------------------------------|
| `/repository/exists/{ciId}`      | GET     | Verifier existence DAR           |
| `/repository/ci/{ciId}`          | GET     | Lire version deployee            |
| HTTP `404 Not Found`             | —       | Application non deployee sur env |

---

## 5) Sources officielles (Digital.ai Deploy)
- REST API Overview : https://docs.digital.ai/bundle/devops-deploy/page/rest-api-overview.html
- Repository API : https://docs.digital.ai/bundle/devops-deploy/page/deploy_repository_api.html
- UDM Model (DeploymentPackage / DeployedApplication) : https://docs.digital.ai/bundle/devops-deploy/page/deploy_udm_overview.html
