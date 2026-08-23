package com.example.devassistant.repository;

import com.example.devassistant.model.PolicyChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyChunkRepository extends JpaRepository<PolicyChunk, Long> {
    List<PolicyChunk> findByPolicyVersionIdOrderByChunkIndexAsc(Long policyVersionId);
    List<PolicyChunk> findByPolicyVersionIdIn(List<Long> policyVersionIds);
}
