package com.example.devassistant.service;

import com.example.devassistant.dto.PolicyCompareResponse;
import com.example.devassistant.model.PolicyVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyComparisonServiceTest {

    private final PolicyComparisonService comparisonService = new PolicyComparisonService();

    @Test
    void detectsAddedAndRemovedLines() {
        PolicyVersion v1 = new PolicyVersion();
        v1.setVersionNumber(1);
        v1.setContent("Password must contain at least 8 characters.");

        PolicyVersion v2 = new PolicyVersion();
        v2.setVersionNumber(2);
        v2.setContent("Password must contain at least 16 characters.\nMFA is required for authentication systems.");

        PolicyCompareResponse result = comparisonService.compare(v1, v2);

        assertEquals(1, result.getFromVersion());
        assertEquals(2, result.getToVersion());
        assertTrue(result.getRemovedLines().contains("Password must contain at least 8 characters."));
        assertTrue(result.getAddedLines().contains("Password must contain at least 16 characters."));
        assertTrue(result.getAddedLines().contains("MFA is required for authentication systems."));
    }

    @Test
    void reportsNoDifferenceForIdenticalContent() {
        PolicyVersion v1 = new PolicyVersion();
        v1.setVersionNumber(1);
        v1.setContent("Same text.");

        PolicyVersion v2 = new PolicyVersion();
        v2.setVersionNumber(2);
        v2.setContent("Same text.");

        PolicyCompareResponse result = comparisonService.compare(v1, v2);

        assertTrue(result.getAddedLines().isEmpty());
        assertTrue(result.getRemovedLines().isEmpty());
    }
}
