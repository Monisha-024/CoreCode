package com.example.devassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "commits", indexes = {
        @Index(name = "idx_commits_repo_id", columnList = "repository_id"),
        @Index(name = "idx_commits_hash", columnList = "commit_hash")
})
public class Commit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(name = "commit_hash", nullable = false, length = 64)
    private String commitHash;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String author;

    @Column(name = "commit_date")
    private LocalDateTime commitDate;

    @Column(name = "changed_files", columnDefinition = "TEXT")
    private String changedFiles;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long repositoryId) { this.repositoryId = repositoryId; }
    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public LocalDateTime getCommitDate() { return commitDate; }
    public void setCommitDate(LocalDateTime commitDate) { this.commitDate = commitDate; }
    public String getChangedFiles() { return changedFiles; }
    public void setChangedFiles(String changedFiles) { this.changedFiles = changedFiles; }
}
