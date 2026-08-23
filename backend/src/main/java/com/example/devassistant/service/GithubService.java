package com.example.devassistant.service;

import com.example.devassistant.dto.CreateRepositoryRequest;
import com.example.devassistant.exception.BadRequestException;
import com.example.devassistant.exception.ExternalServiceException;
import com.example.devassistant.exception.ResourceNotFoundException;
import com.example.devassistant.model.CodeFile;
import com.example.devassistant.model.Commit;
import com.example.devassistant.model.Repository;
import com.example.devassistant.repository.CodeFileRepository;
import com.example.devassistant.repository.CommitRepository;
import com.example.devassistant.repository.RepositoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Integrates with the real GitHub REST API to pull repository structure,
 * supported source files, and commit history. No GitHub data is ever
 * fabricated - if the API call fails (bad token, repo not found, rate
 * limit) an ExternalServiceException is thrown with a clear message
 * instead of returning fake data.
 *
 * All GitHub access here is READ-ONLY: only GET requests are issued against
 * the Contents and Commits APIs. Nothing in this service pushes, commits,
 * modifies, creates branches/PRs, or deletes anything in the connected repo.
 */
@Service
public class GithubService {

    /**
     * File extensions the Code Explorer / RAG pipeline can index, mapped to
     * their display/language label. Extend this map to support additional
     * languages without touching the traversal or storage logic.
     */
    private static final Map<String, String> SUPPORTED_EXTENSIONS = Map.of(
            "java", "java",
            "py", "python",
            "js", "javascript",
            "jsx", "javascript",
            "ts", "typescript",
            "tsx", "typescript"
    );

    private final RestClient restClient;
    private final RepositoryRepository repositoryRepository;
    private final CodeFileRepository codeFileRepository;
    private final CommitRepository commitRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${github.api.token}")
    private String githubToken;

    @Value("${github.api.base-url}")
    private String githubBaseUrl;

    public GithubService(RestClient restClient,
                          RepositoryRepository repositoryRepository,
                          CodeFileRepository codeFileRepository,
                          CommitRepository commitRepository) {
        this.restClient = restClient;
        this.repositoryRepository = repositoryRepository;
        this.codeFileRepository = codeFileRepository;
        this.commitRepository = commitRepository;
    }

    public Repository connectRepository(CreateRepositoryRequest request, Long connectedByUserId) {
        if (githubToken == null || githubToken.isBlank()) {
            throw new ExternalServiceException("GitHub",
                    "GITHUB_TOKEN is not configured. Set it in your .env file to connect repositories.");
        }

        String owner = request.getOwner();
        String name = request.getName();
        JsonNode repoJson = get("/repos/" + owner + "/" + name);

        Repository repository = new Repository();
        repository.setOwner(owner);
        repository.setName(name);
        repository.setGithubUrl(repoJson.path("html_url").asText("https://github.com/" + owner + "/" + name));
        String branch = request.getBranch() != null && !request.getBranch().isBlank()
                ? request.getBranch()
                : repoJson.path("default_branch").asText("main");
        repository.setDefaultBranch(branch);
        repository.setConnectedBy(connectedByUserId);
        repository = repositoryRepository.save(repository);

        syncFiles(repository, "");
        syncCommits(repository);

        return repository;
    }

    private void syncFiles(Repository repository, String path) {
        String url = "/repos/" + repository.getOwner() + "/" + repository.getName()
                + "/contents/" + path + "?ref=" + repository.getDefaultBranch();
        JsonNode contents = get(url);

        if (contents.isArray()) {
            for (JsonNode entry : contents) {
                String type = entry.path("type").asText();
                String entryPath = entry.path("path").asText();
                if ("dir".equals(type)) {
                    syncFiles(repository, entryPath);
                } else if ("file".equals(type) && isSupportedCodeFile(entryPath)) {
                    fetchAndStoreFile(repository, entryPath);
                }
            }
        }
    }

    /**
     * Returns true if the given file path has one of the supported source
     * code extensions (case-insensitive - handles ".PY", ".JS", etc.).
     * Everything else (binaries, images, docs, build artifacts, unrelated
     * text files) is skipped, matching the previous Java-only behavior but
     * generalized to the full supported set.
     */
    boolean isSupportedCodeFile(String filePath) {
        String extension = extractExtension(filePath);
        return extension != null && SUPPORTED_EXTENSIONS.containsKey(extension);
    }

    /** Maps a file's extension to its language label (e.g. "py" -> "python"). Defaults to "text" if unknown. */
    String detectLanguage(String filePath) {
        String extension = extractExtension(filePath);
        return extension != null ? SUPPORTED_EXTENSIONS.getOrDefault(extension, "text") : "text";
    }

    private String extractExtension(String filePath) {
        if (filePath == null) return null;
        int lastDot = filePath.lastIndexOf('.');
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastDot <= lastSlash || lastDot == filePath.length() - 1) {
            return null; // no extension, or a dotfile with nothing after the dot
        }
        return filePath.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private void fetchAndStoreFile(Repository repository, String filePath) {
        try {
            JsonNode fileJson = get("/repos/" + repository.getOwner() + "/" + repository.getName()
                    + "/contents/" + filePath + "?ref=" + repository.getDefaultBranch());
            String base64Content = fileJson.path("content").asText("");
            String content = new String(Base64.getMimeDecoder().decode(base64Content));

            CodeFile codeFile = codeFileRepository
                    .findByRepositoryIdAndFilePath(repository.getId(), filePath)
                    .orElse(new CodeFile());
            codeFile.setRepositoryId(repository.getId());
            codeFile.setFilePath(filePath);
            codeFile.setLanguage(detectLanguage(filePath));
            codeFile.setContent(content);
            codeFile.setLastUpdated(LocalDateTime.now());
            codeFileRepository.save(codeFile);
        } catch (Exception e) {
            // Skip files GitHub can't return content for (e.g. > 1MB); do not fabricate content.
        }
    }

    private void syncCommits(Repository repository) {
        JsonNode commitsJson = get("/repos/" + repository.getOwner() + "/" + repository.getName()
                + "/commits?sha=" + repository.getDefaultBranch() + "&per_page=30");

        if (!commitsJson.isArray()) return;

        for (JsonNode c : commitsJson) {
            String sha = c.path("sha").asText();
            JsonNode commitDetailJson = get("/repos/" + repository.getOwner() + "/" + repository.getName()
                    + "/commits/" + sha);

            Commit commit = new Commit();
            commit.setRepositoryId(repository.getId());
            commit.setCommitHash(sha);
            commit.setMessage(commitDetailJson.path("commit").path("message").asText(""));
            commit.setAuthor(commitDetailJson.path("commit").path("author").path("name").asText("unknown"));

            String dateStr = commitDetailJson.path("commit").path("author").path("date").asText(null);
            if (dateStr != null) {
                commit.setCommitDate(OffsetDateTime.parse(dateStr).toLocalDateTime());
            }

            List<String> changedFiles = new ArrayList<>();
            JsonNode files = commitDetailJson.path("files");
            if (files.isArray()) {
                for (JsonNode f : files) {
                    changedFiles.add(f.path("filename").asText());
                }
            }
            commit.setChangedFiles(String.join(", ", changedFiles));

            commitRepository.save(commit);
        }
    }

    public List<CodeFile> listFiles(Long repositoryId) {
        getRepository(repositoryId);
        return codeFileRepository.findByRepositoryId(repositoryId);
    }

    public CodeFile getFile(Long repositoryId, String filePath) {
        return codeFileRepository.findByRepositoryIdAndFilePath(repositoryId, filePath)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + filePath));
    }

    public List<Commit> listCommits(Long repositoryId) {
        getRepository(repositoryId);
        return commitRepository.findByRepositoryIdOrderByCommitDateDesc(repositoryId);
    }

    /** Commits that touched a given file - the evidence base for "why was this changed?" */
    public List<Commit> commitsForFile(Long repositoryId, String filePath) {
        return commitRepository.findByRepositoryIdAndChangedFilesContainingOrderByCommitDateDesc(repositoryId, filePath);
    }

    public Repository getRepository(Long repositoryId) {
        return repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + repositoryId));
    }

    public List<Repository> listRepositories() {
        return repositoryRepository.findAll();
    }

    private JsonNode get(String path) {
        try {
            String body = restClient.get()
                    .uri(githubBaseUrl + path)
                    .headers(h -> {
                        h.set(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken);
                        h.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                        h.set("X-GitHub-Api-Version", "2022-11-28");
                    })
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(body);
        } catch (RestClientException e) {
            throw new ExternalServiceException("GitHub", "Request failed for " + path + " - " + e.getMessage());
        } catch (Exception e) {
            throw new ExternalServiceException("GitHub", "Unexpected error reading GitHub response: " + e.getMessage());
        }
    }
}
