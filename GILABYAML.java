/*
gitlab:
  base-url: "https://gitlab.com"
  project-id: "123456"        # ID numérique ou path URL-encoded (ex: group%2Fproject)
  branch: "main"
  file-path: "config/myfile.yaml"  # chemin dans le repo
  token: "${GITLAB_TOKEN}"    # Private token / PAT

*/
package com.example.gitlabyaml;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gitlab")
public class GitLabProperties {
    private String baseUrl;
    private String projectId;
    private String branch;
    private String filePath;
    private String token;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
//---------------------------------------------------------------------------
package com.example.gitlabyaml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GitLabYamlService {

    private final WebClient webClient;
    private final GitLabProperties props;
    private final ObjectMapper yamlMapper;

    public GitLabYamlService(WebClient.Builder builder, GitLabProperties props) {
        this.props = props;

        this.webClient = builder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("PRIVATE-TOKEN", props.getToken())
                .build();

        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /** 1) Lire YAML depuis GitLab (RAW endpoint) -> Map<String, List<Element>> */
    public Mono<Map<String, List<Element>>> loadYamlAsMap() {
        Assert.hasText(props.getProjectId(), "gitlab.project-id is required");
        Assert.hasText(props.getFilePath(), "gitlab.file-path is required");
        Assert.hasText(props.getBranch(), "gitlab.branch is required");

        String encodedPath = UriUtils.encodePath(props.getFilePath(), StandardCharsets.UTF_8);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v4/projects/{id}/repository/files/{filePath}/raw")
                        .queryParam("ref", props.getBranch())
                        .build(props.getProjectId(), encodedPath))
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseYamlToMap);
    }

    /** 2) Ajouter un Element dans une clé (map1/map2/...) */
    public Mono<Map<String, List<Element>>> addElement(String mapKey, Element element) {
        return loadYamlAsMap().map(data -> {
            data.computeIfAbsent(mapKey, k -> new ArrayList<>()).add(element);
            return data;
        });
    }

    /** 3) Supprimer un Element d’une clé selon un critère (ex: path == ...) */
    public Mono<Map<String, List<Element>>> removeByPath(String mapKey, String pathToRemove) {
        return loadYamlAsMap().map(data -> {
            List<Element> list = data.get(mapKey);
            if (list != null) {
                list.removeIf(e -> Objects.equals(e.getPath(), pathToRemove));
            }
            return data;
        });
    }

    /** 4) Sauver (commit) le YAML modifié dans GitLab via PUT repository/files */
    public Mono<Void> commitYaml(Map<String, List<Element>> data, String commitMessage) {
        String newYaml = writeMapToYaml(data);

        String encodedPath = UriUtils.encodePath(props.getFilePath(), StandardCharsets.UTF_8);

        // GitLab Update File API (PUT /projects/:id/repository/files/:file_path)
        // Body: branch, commit_message, content
        return webClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v4/projects/{id}/repository/files/{filePath}")
                        .build(props.getProjectId(), encodedPath))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("branch=" + url(commitSafe(props.getBranch())) +
                           "&commit_message=" + url(commitSafe(commitMessage)) +
                           "&content=" + url(newYaml))
                .retrieve()
                .bodyToMono(String.class)
                .then();
    }

    /** Helper: parse YAML -> Map */
    private Map<String, List<Element>> parseYamlToMap(String yaml) {
        try {
            if (yaml == null || yaml.isBlank()) return new LinkedHashMap<>();
            return yamlMapper.readValue(yaml, new TypeReference<Map<String, List<Element>>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse YAML into Map<String, List<Element>>", e);
        }
    }

    /** Helper: Map -> YAML */
    private String writeMapToYaml(Map<String, List<Element>> data) {
        try {
            // LinkedHashMap pour garder l’ordre (map1, map2, ...)
            Map<String, List<Element>> ordered = new LinkedHashMap<>(data);
            return yamlMapper.writeValueAsString(ordered);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize Map to YAML", e);
        }
    }

    /** URL encode simple pour x-www-form-urlencoded */
    private String url(String s) {
        return UriUtils.encodeQueryParam(s, StandardCharsets.UTF_8);
    }

    private String commitSafe(String s) {
        return (s == null) ? "" : s;
    }
}
//------------------------------------------------------
//Exemple d’utilisation (add puis commit)
// Exemple : ajouter un élément à map3 puis commit
service.addElement("map3", new Element("/new/path/file.txt", "hamdi"))
       .flatMap(updated -> service.commitYaml(updated, "Update YAML: add element to map3"))
       .block();
