package com.example.devassistant.dto;

import java.time.LocalDateTime;

public class CommitDTO {
    private Long id;
    private String commitHash;
    private String message;
    private String author;
    private LocalDateTime commitDate;
    private String changedFiles;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
