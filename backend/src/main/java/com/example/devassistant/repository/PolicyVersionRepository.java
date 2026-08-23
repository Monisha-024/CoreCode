package com.example.devassistant.repository;

import com.example.devassistant.model.PolicyStatus;
import com.example.devassistant.model.PolicyVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyVersionRepository extends JpaRepository<PolicyVersion, Long> {
    List<PolicyVersion> findByPolicyIdOrderByVersionNumberDesc(Long policyId);
    Optional<PolicyVersion> findByPolicyIdAndStatus(Long policyId, PolicyStatus status);
    Optional<PolicyVersion> findByPolicyIdAndVersionNumber(Long policyId, Integer versionNumber);
    List<PolicyVersion> findByStatus(PolicyStatus status);
}
