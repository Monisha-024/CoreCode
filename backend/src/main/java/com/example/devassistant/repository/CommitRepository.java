package com.example.devassistant.repository;

import com.example.devassistant.model.Commit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommitRepository extends JpaRepository<Commit, Long> {
    List<Commit> findByRepositoryIdOrderByCommitDateDesc(Long repositoryId);
    List<Commit> findByRepositoryIdAndChangedFilesContainingOrderByCommitDateDesc(Long repositoryId, String filePathFragment);
}
