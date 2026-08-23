package com.example.devassistant.repository;

import com.example.devassistant.model.CodeFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeFileRepository extends JpaRepository<CodeFile, Long> {
    List<CodeFile> findByRepositoryId(Long repositoryId);
    Optional<CodeFile> findByRepositoryIdAndFilePath(Long repositoryId, String filePath);
}
