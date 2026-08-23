package com.example.devassistant.service;

import com.example.devassistant.dto.CreatePolicyRequest;
import com.example.devassistant.dto.PolicyDTO;
import com.example.devassistant.model.Policy;
import com.example.devassistant.model.PolicyStatus;
import com.example.devassistant.model.PolicyVersion;
import com.example.devassistant.repository.PolicyChunkRepository;
import com.example.devassistant.repository.PolicyRepository;
import com.example.devassistant.repository.PolicyVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers policy version activation/archiving business rules -
 * "only one version can be ACTIVE at a time".
 */
@ExtendWith(MockitoExtension.class)
class PolicyVersionServiceTest {

    @Mock private PolicyRepository policyRepository;
    @Mock private PolicyVersionRepository policyVersionRepository;
    @Mock private PolicyChunkRepository policyChunkRepository;
    @Mock private PdfExtractionService pdfExtractionService;

    @Test
    void createPolicyRejectsDuplicateName() {
        PolicyService service = new PolicyService(policyRepository, policyVersionRepository, policyChunkRepository, pdfExtractionService);

        Policy existing = new Policy();
        existing.setName("Information Security Policy");
        when(policyRepository.findAll()).thenReturn(List.of(existing));

        CreatePolicyRequest request = new CreatePolicyRequest();
        request.setName("Information Security Policy");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.example.devassistant.exception.DuplicateResourceException.class,
                () -> service.createPolicy(request));
    }

    @Test
    void activatingNewVersionArchivesThePreviousActiveVersion() {
        PolicyService service = new PolicyService(policyRepository, policyVersionRepository, policyChunkRepository, pdfExtractionService);

        Policy policy = new Policy();
        policy.setId(1L);
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));

        PolicyVersion currentActive = new PolicyVersion();
        currentActive.setId(10L);
        currentActive.setPolicyId(1L);
        currentActive.setVersionNumber(1);
        currentActive.setStatus(PolicyStatus.ACTIVE);

        PolicyVersion target = new PolicyVersion();
        target.setId(11L);
        target.setPolicyId(1L);
        target.setVersionNumber(2);
        target.setStatus(PolicyStatus.ARCHIVED);

        when(policyVersionRepository.findByPolicyIdAndVersionNumber(1L, 2)).thenReturn(Optional.of(target));
        when(policyVersionRepository.findByPolicyIdAndStatus(1L, PolicyStatus.ACTIVE)).thenReturn(Optional.of(currentActive));
        when(policyVersionRepository.save(any(PolicyVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

        service.activateVersion(1L, 2);

        assertEquals(PolicyStatus.ARCHIVED, currentActive.getStatus());
        assertEquals(PolicyStatus.ACTIVE, target.getStatus());
        verify(policyVersionRepository, times(2)).save(any(PolicyVersion.class));
    }
}
