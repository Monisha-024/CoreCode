package com.example.devassistant.service;

import com.example.devassistant.model.PolicyChunk;
import com.example.devassistant.model.PolicyStatus;
import com.example.devassistant.model.PolicyVersion;
import com.example.devassistant.repository.PolicyChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyRetrievalServiceTest {

    @Mock
    private PolicyChunkRepository policyChunkRepository;

    @Test
    void ranksMoreRelevantChunkHigher() {
        PolicyRetrievalService retrievalService = new PolicyRetrievalService(policyChunkRepository);

        PolicyVersion version = new PolicyVersion();
        version.setId(1L);
        version.setVersionNumber(2);
        version.setStatus(PolicyStatus.ACTIVE);

        PolicyChunk relevantChunk = new PolicyChunk();
        relevantChunk.setId(1L);
        relevantChunk.setPolicyVersionId(1L);
        relevantChunk.setChunkIndex(0);
        relevantChunk.setContent("Password must contain at least 16 characters and MFA is required.");

        PolicyChunk irrelevantChunk = new PolicyChunk();
        irrelevantChunk.setId(2L);
        irrelevantChunk.setPolicyVersionId(1L);
        irrelevantChunk.setChunkIndex(1);
        irrelevantChunk.setContent("Remote employees must use the company VPN.");

        when(policyChunkRepository.findByPolicyVersionIdIn(List.of(1L)))
                .thenReturn(List.of(relevantChunk, irrelevantChunk));

        List<ScoredChunk> results = retrievalService.retrieveRelevantChunks(
                "What is the current password requirement?", List.of(version));

        assertFalse(results.isEmpty());
        assertEquals(relevantChunk.getId(), results.get(0).getChunk().getId());
    }

    @Test
    void returnsEmptyWhenNoActiveVersions() {
        PolicyRetrievalService retrievalService = new PolicyRetrievalService(policyChunkRepository);
        List<ScoredChunk> results = retrievalService.retrieveRelevantChunks("What is the password policy?", List.of());
        assertEquals(0, results.size());
    }
}
