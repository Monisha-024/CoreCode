package com.example.devassistant.service;

import com.example.devassistant.dto.CreatePolicyRequest;
import com.example.devassistant.dto.PolicyDTO;
import com.example.devassistant.dto.PolicyVersionDTO;
import com.example.devassistant.exception.BadRequestException;
import com.example.devassistant.exception.DuplicateResourceException;
import com.example.devassistant.exception.ResourceNotFoundException;
import com.example.devassistant.model.*;
import com.example.devassistant.repository.PolicyChunkRepository;
import com.example.devassistant.repository.PolicyRepository;
import com.example.devassistant.repository.PolicyVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages policies and their versions:
 * - a Policy groups all versions of one document (e.g. "Information Security Policy")
 * - each PolicyVersion is either ACTIVE or ARCHIVED, and only one version per
 *   policy can be ACTIVE at a time.
 */
@Service
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository policyVersionRepository;
    private final PolicyChunkRepository policyChunkRepository;
    private final PdfExtractionService pdfExtractionService;

    public PolicyService(PolicyRepository policyRepository,
                          PolicyVersionRepository policyVersionRepository,
                          PolicyChunkRepository policyChunkRepository,
                          PdfExtractionService pdfExtractionService) {
        this.policyRepository = policyRepository;
        this.policyVersionRepository = policyVersionRepository;
        this.policyChunkRepository = policyChunkRepository;
        this.pdfExtractionService = pdfExtractionService;
    }

    @Transactional
    public PolicyDTO createPolicy(CreatePolicyRequest request) {
        boolean duplicateName = policyRepository.findAll().stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(request.getName()));
        if (duplicateName) {
            throw new DuplicateResourceException("A policy named '" + request.getName() + "' already exists");
        }

        Policy policy = new Policy();
        policy.setName(request.getName());
        policy.setDescription(request.getDescription());
        policy = policyRepository.save(policy);
        return toDTO(policy);
    }

    public List<PolicyDTO> listPolicies() {
        return policyRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public PolicyDTO getPolicy(Long id) {
        return toDTO(getPolicyEntity(id));
    }

    public Policy getPolicyEntity(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id));
    }

    @Transactional
    public PolicyVersionDTO uploadVersion(Long policyId, MultipartFile file, LocalDate effectiveDate) {
        Policy policy = getPolicyEntity(policyId);

        String text = pdfExtractionService.extractText(file);

        List<PolicyVersion> existing = policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policyId);
        int nextVersionNumber = existing.isEmpty() ? 1 : existing.get(0).getVersionNumber() + 1;

        PolicyVersion version = new PolicyVersion();
        version.setPolicyId(policyId);
        version.setVersionNumber(nextVersionNumber);
        version.setFileName(file.getOriginalFilename());
        version.setEffectiveDate(effectiveDate != null ? effectiveDate : LocalDate.now());
        version.setContent(text);
        // First version uploaded for a policy is activated automatically;
        // subsequent versions start ARCHIVED until an admin explicitly activates them.
        version.setStatus(existing.isEmpty() ? PolicyStatus.ACTIVE : PolicyStatus.ARCHIVED);
        version = policyVersionRepository.save(version);

        storeChunks(version.getId(), text);

        if (version.getStatus() == PolicyStatus.ACTIVE) {
            policy.setCurrentVersionId(version.getId());
        }
        policy.setUpdatedAt(LocalDateTime.now());
        policyRepository.save(policy);

        return toDTO(version);
    }

    private void storeChunks(Long policyVersionId, String text) {
        List<String> chunks = pdfExtractionService.splitIntoChunks(text);
        int index = 0;
        for (String chunkText : chunks) {
            PolicyChunk chunk = new PolicyChunk();
            chunk.setPolicyVersionId(policyVersionId);
            chunk.setChunkIndex(index++);
            chunk.setContent(chunkText);
            policyChunkRepository.save(chunk);
        }
    }

    public List<PolicyVersionDTO> listVersions(Long policyId) {
        getPolicyEntity(policyId); // ensures existence
        return policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policyId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public PolicyVersionDTO activateVersion(Long policyId, Integer versionNumber) {
        Policy policy = getPolicyEntity(policyId);
        PolicyVersion target = policyVersionRepository.findByPolicyIdAndVersionNumber(policyId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " not found for policy " + policyId));

        // Archive whichever version is currently ACTIVE.
        policyVersionRepository.findByPolicyIdAndStatus(policyId, PolicyStatus.ACTIVE)
                .ifPresent(current -> {
                    current.setStatus(PolicyStatus.ARCHIVED);
                    policyVersionRepository.save(current);
                });

        target.setStatus(PolicyStatus.ACTIVE);
        target = policyVersionRepository.save(target);

        policy.setCurrentVersionId(target.getId());
        policy.setUpdatedAt(LocalDateTime.now());
        policyRepository.save(policy);

        return toDTO(target);
    }

    @Transactional
    public void archiveVersion(Long policyId, Integer versionNumber) {
        PolicyVersion version = policyVersionRepository.findByPolicyIdAndVersionNumber(policyId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found"));
        if (version.getStatus() == PolicyStatus.ACTIVE) {
            throw new BadRequestException("Cannot archive the currently ACTIVE version. Activate another version first.");
        }
        version.setStatus(PolicyStatus.ARCHIVED);
        policyVersionRepository.save(version);
    }

    @Transactional
    public void deletePolicy(Long policyId) {
        getPolicyEntity(policyId);
        List<PolicyVersion> versions = policyVersionRepository.findByPolicyIdOrderByVersionNumberDesc(policyId);
        for (PolicyVersion v : versions) {
            policyChunkRepository.deleteAll(policyChunkRepository.findByPolicyVersionIdOrderByChunkIndexAsc(v.getId()));
        }
        policyVersionRepository.deleteAll(versions);
        policyRepository.deleteById(policyId);
    }

    public PolicyVersion getActiveVersion(Long policyId) {
        return policyVersionRepository.findByPolicyIdAndStatus(policyId, PolicyStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No ACTIVE version found for policy " + policyId));
    }

    public List<PolicyVersion> getAllActiveVersions() {
        return policyVersionRepository.findByStatus(PolicyStatus.ACTIVE);
    }

    public PolicyVersion getVersion(Long policyId, Integer versionNumber) {
        return policyVersionRepository.findByPolicyIdAndVersionNumber(policyId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found"));
    }

    private PolicyDTO toDTO(Policy policy) {
        PolicyDTO dto = new PolicyDTO();
        dto.setId(policy.getId());
        dto.setName(policy.getName());
        dto.setDescription(policy.getDescription());
        dto.setCreatedAt(policy.getCreatedAt());
        dto.setUpdatedAt(policy.getUpdatedAt());

        if (policy.getCurrentVersionId() != null) {
            policyVersionRepository.findById(policy.getCurrentVersionId()).ifPresent(v -> {
                dto.setCurrentVersionNumber(v.getVersionNumber());
                dto.setCurrentVersionStatus(v.getStatus().name());
            });
        }
        return dto;
    }

    private PolicyVersionDTO toDTO(PolicyVersion version) {
        PolicyVersionDTO dto = new PolicyVersionDTO();
        dto.setId(version.getId());
        dto.setPolicyId(version.getPolicyId());
        dto.setVersionNumber(version.getVersionNumber());
        dto.setFileName(version.getFileName());
        dto.setEffectiveDate(version.getEffectiveDate());
        dto.setStatus(version.getStatus().name());
        dto.setContent(version.getContent());
        dto.setCreatedAt(version.getCreatedAt());
        return dto;
    }
}
