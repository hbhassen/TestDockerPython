une solution complète pour développer une application Spring Boot 3 (JDK 17) simulant un système MFT .
L’application utilisera Spring Web (REST Controller) et Spring Boot Starter JSON pour gérer les API, le stockage local des fichiers et métadonnées, et générer les réponses conformes aux besoins.

1️⃣ Exigences minimales
JDK : 17

Spring Boot : 3.2.x

Dépendances Maven :

spring-boot-starter-web

spring-boot-starter-json

spring-boot-starter-validation

Port d’écoute : 9999

Répertoire racine stockage : ./mft-storage (créé automatiquement à la racine du projet)

2️⃣ Structure du projet
```css
mft-mock/
 ├── src/main/java/com/example/mftmock
 │    ├── controller/
 │    │     └── MftController.java
 │    ├── model/
 │    │     └── FileMetadata.java
 │    ├── service/
 │    │     └── MftService.java
 │    └── MftMockApplication.java
 └── pom.xml
```
3️⃣ pom.xml
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>mft-mock</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <spring.boot.version>3.2.7</spring.boot.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```
4️⃣ Classe FileMetadata.java (modèle)
```java
package com.example.mftmock.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileMetadata {
    private String id;
    private String fileName;
    private OffsetDateTime creationDate;
    private OffsetDateTime expirationDate;
    private String operationType; // UPLOAD ou DOWNLOAD
    private int downloadCont;
    private String spReference;
    private String spFlowName;
    private String spMessage;
    private List<Audit> audit = new ArrayList<>();

    public static class Audit {
        public String username;
        public OffsetDateTime date;
        public String action;
        public String detail;
    }

    // Getters et Setters
    // ...
} ```
5️⃣ Classe MftService.java
Cette classe gère toute la logique : stockage, lecture, mise à jour des fichiers et métadonnées.

```java
package com.example.mftmock.service;

import com.example.mftmock.model.FileMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class MftService {

    private final Path storageDir = Paths.get("mft-storage");
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public MftService() throws IOException {
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }
    }

    public FileMetadata uploadFile(String flowName, String fileName, byte[] content) throws IOException {
        Path flowDir = storageDir.resolve(flowName);
        if (!Files.exists(flowDir)) Files.createDirectories(flowDir);

        int id = idCounter.getAndIncrement();
        OffsetDateTime now = OffsetDateTime.now();

        // Stocker le fichier
        Files.write(flowDir.resolve(fileName), content);

        // Créer métadonnées
        FileMetadata meta = new FileMetadata();
        meta.setId(String.valueOf(id));
        meta.setFileName(fileName);
        meta.setCreationDate(now);
        meta.setExpirationDate(now.plusHours(24));
        meta.setOperationType("UPLOAD");
        meta.setDownloadCont(0);
        meta.setSpReference(UUID.randomUUID().toString());
        meta.setSpFlowName(flowName);
        meta.setSpMessage("document uploader par : " + flowName);

        FileMetadata.Audit audit = new FileMetadata.Audit();
        audit.username = "testeur1";
        audit.date = now;
        audit.action = "upload";
        audit.detail = "document uploader par l'equipe: " + flowName;
        meta.getAudit().add(audit);

        // Sauvegarder fichier JSON
        mapper.writeValue(flowDir.resolve(fileName + ".json").toFile(), meta);

        return meta;
    }

    public List<FileMetadata> listFiles(String operation) throws IOException {
        List<FileMetadata> result = new ArrayList<>();
        Files.walk(storageDir, 2)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(p -> {
                    try {
                        FileMetadata meta = mapper.readValue(p.toFile(), FileMetadata.class);
                        if (meta.getOperationType().equalsIgnoreCase(operation)
                                && meta.getExpirationDate().isAfter(OffsetDateTime.now())) {
                            result.add(meta);
                        }
                    } catch (Exception ignored) {}
                });
        return result;
    }

    public FileMetadata sendFile(String flowName, String fileId, String message) throws IOException {
        Path flowDir = storageDir.resolve(flowName);
        Optional<Path> fileMetaPath = Files.list(flowDir)
                .filter(p -> p.toString().endsWith(".json"))
                .filter(p -> {
                    try {
                        FileMetadata meta = mapper.readValue(p.toFile(), FileMetadata.class);
                        return meta.getId().equals(fileId);
                    } catch (Exception e) {return false;}
                }).findFirst();

        if (fileMetaPath.isEmpty()) return null;

        FileMetadata meta = mapper.readValue(fileMetaPath.get().toFile(), FileMetadata.class);
        OffsetDateTime now = OffsetDateTime.now();

        meta.setOperationType("DOWNLOAD");
        meta.setSpMessage(message);

        FileMetadata.Audit audit = new FileMetadata.Audit();
        audit.username = "testeur1";
        audit.date = now;
        audit.action = "submited";
        audit.detail = message;
        meta.getAudit().add(audit);

        mapper.writeValue(fileMetaPath.get().toFile(), meta);
        return meta;
    }

    public byte[] downloadFile(String flowName, String fileId) throws IOException {
        Path flowDir = storageDir.resolve(flowName);
        Optional<Path> metaPath = Files.list(flowDir)
                .filter(p -> p.toString().endsWith(".json"))
                .filter(p -> {
                    try {
                        return mapper.readValue(p.toFile(), FileMetadata.class)
                                .getId().equals(fileId);
                    } catch (Exception e) {return false;}
                }).findFirst();

        if (metaPath.isEmpty()) return null;

        FileMetadata meta = mapper.readValue(metaPath.get().toFile(), FileMetadata.class);
        meta.setDownloadCont(meta.getDownloadCont() + 1);

        FileMetadata.Audit audit = new FileMetadata.Audit();
        audit.username = "testeur1";
        audit.date = OffsetDateTime.now();
        audit.action = "DOWNLOAD";
        audit.detail = "document downloded par l'equipe: " + meta.getSpFlowName();
        meta.getAudit().add(audit);

        mapper.writeValue(metaPath.get().toFile(), meta);

        return Files.readAllBytes(flowDir.resolve(meta.getFileName()));
    }

    public FileMetadata getFileDetails(String flowName, String fileId) throws IOException {
        Path flowDir = storageDir.resolve(flowName);
        return Files.list(flowDir)
                .filter(p -> p.toString().endsWith(".json"))
                .map(p -> {
                    try {return mapper.readValue(p.toFile(), FileMetadata.class);}
                    catch (Exception e) {return null;}
                })
                .filter(Objects::nonNull)
                .filter(meta -> meta.getId().equals(fileId))
                .findFirst().orElse(null);
    }
}
```
6️⃣ Classe MftController.java
Cette classe expose les endpoints REST.

```java
package com.example.mftmock.controller;

import com.example.mftmock.model.FileMetadata;
import com.example.mftmock.service.MftService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gw/transferFile/v1")
public class MftController {

    private final MftService service;

    public MftController(MftService service) {
        this.service = service;
    }

    @PostMapping("/file")
    public ResponseEntity<FileMetadata> uploadFile(
            @RequestHeader("File_Name") String fileName,
            @RequestHeader("Flow_Name") String flowName,
            @RequestBody byte[] content) throws IOException {
        return ResponseEntity.ok(service.uploadFile(flowName, fileName, content));
    }

    @GetMapping("/files")
    public ResponseEntity<List<FileMetadata>> listFiles(@RequestParam String operation,
                                                        @RequestParam boolean include_processed) throws IOException {
        return ResponseEntity.ok(service.listFiles(operation));
    }

    @PostMapping("/file/{id}/send")
    public ResponseEntity<FileMetadata> sendFile(@PathVariable String id,
                                                 @RequestBody Map<String,String> body) throws IOException {
        return ResponseEntity.ok(service.sendFile(body.get("spFlowName"), id, body.get("spMessage")));
    }

    @GetMapping("/file/{id}/data")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String id,
                                               @RequestHeader("Flow_Name") String flowName) throws IOException {
        byte[] data = service.downloadFile(flowName, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/file/{id}")
    public ResponseEntity<FileMetadata> getFileDetails(@PathVariable String id,
                                                       @RequestHeader("Flow_Name") String flowName) throws IOException {
        return ResponseEntity.ok(service.getFileDetails(flowName, id));
    }
}
```
