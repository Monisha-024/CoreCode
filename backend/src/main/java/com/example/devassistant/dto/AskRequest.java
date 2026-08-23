package com.example.devassistant.dto;

import jakarta.validation.constraints.NotBlank;

public class AskRequest {
    @NotBlank
    private String question;

    /** Optional: id of a repository the question relates to. */
    private Long repositoryId;

    /** Optional: path of a specific file the question relates to (Code Explorer context). */
    private String filePath;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public Long getRepositoryId() { return repositoryId; }
    public void setRepositoryId(Long repositoryId) { this.repositoryId = repositoryId; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
