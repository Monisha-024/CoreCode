package com.example.devassistant.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateRepositoryRequest {
    @NotBlank
    private String owner;

    @NotBlank
    private String name;

    private String branch;

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
}
