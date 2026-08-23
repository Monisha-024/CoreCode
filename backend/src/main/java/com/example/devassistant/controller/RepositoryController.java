package com.example.devassistant.controller;

import com.example.devassistant.dto.CodeFileDTO;
import com.example.devassistant.dto.CommitDTO;
import com.example.devassistant.dto.CreateRepositoryRequest;
import com.example.devassistant.dto.RepositoryDTO;
import com.example.devassistant.model.CodeFile;
import com.example.devassistant.model.Commit;
import com.example.devassistant.model.Repository;
import com.example.devassistant.repository.UserRepository;
import com.example.devassistant.service.GithubService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

    private final GithubService githubService;
    private final UserRepository userRepository;

    public RepositoryController(GithubService githubService, UserRepository userRepository) {
        this.githubService = githubService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<RepositoryDTO> connect(@Valid @RequestBody CreateRepositoryRequest request,
                                                   Authentication authentication) {
        Long userId = userRepository.findByEmail(authentication.getName())
                .map(u -> u.getId()).orElse(null);
        Repository repo = githubService.connectRepository(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(repo));
    }

    @GetMapping
    public ResponseEntity<List<RepositoryDTO>> list() {
        return ResponseEntity.ok(githubService.listRepositories().stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<List<CodeFileDTO>> files(@PathVariable Long id) {
        List<CodeFileDTO> files = githubService.listFiles(id).stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}/files/content")
    public ResponseEntity<CodeFileDTO> fileContent(@PathVariable Long id, @RequestParam String path) {
        CodeFile file = githubService.getFile(id, path);
        return ResponseEntity.ok(toDTO(file));
    }

    @GetMapping("/{id}/commits")
    public ResponseEntity<List<CommitDTO>> commits(@PathVariable Long id,
                                                     @RequestParam(required = false) String path) {
        List<Commit> commits = (path == null || path.isBlank())
                ? githubService.listCommits(id)
                : githubService.commitsForFile(id, path);
        return ResponseEntity.ok(commits.stream().map(this::toDTO).collect(Collectors.toList()));
    }

    private RepositoryDTO toDTO(Repository r) {
        RepositoryDTO dto = new RepositoryDTO();
        dto.setId(r.getId());
        dto.setName(r.getName());
        dto.setOwner(r.getOwner());
        dto.setGithubUrl(r.getGithubUrl());
        dto.setDefaultBranch(r.getDefaultBranch());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }

    private CodeFileDTO toDTO(CodeFile f) {
        CodeFileDTO dto = new CodeFileDTO();
        dto.setId(f.getId());
        dto.setFilePath(f.getFilePath());
        dto.setLanguage(f.getLanguage());
        dto.setContent(f.getContent());
        return dto;
    }

    private CommitDTO toDTO(Commit c) {
        CommitDTO dto = new CommitDTO();
        dto.setId(c.getId());
        dto.setCommitHash(c.getCommitHash());
        dto.setMessage(c.getMessage());
        dto.setAuthor(c.getAuthor());
        dto.setCommitDate(c.getCommitDate());
        dto.setChangedFiles(c.getChangedFiles());
        return dto;
    }
}
