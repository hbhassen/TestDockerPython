# enterprise-app-h2

Application Spring Boot generee depuis l archetype `springboot-enterprise-archetype`.


- `groupId` : `com.company.demo`
- `artifactId` : `enterprise-app-h2`
- `version` : `1.0.0-SNAPSHOT`
- `package` : `com.company.demo`


- pile web : `webmvc`
- base de donnees : `h2`
- nom de base : `enterprise_h2`
- schema : `app_schema`


```powershell
mvn spring-boot:run
```


- endpoint version : `GET /api/application/version`
- documentation OpenAPI : `/swagger-ui.html`
