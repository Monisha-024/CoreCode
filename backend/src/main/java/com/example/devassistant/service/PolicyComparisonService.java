package com.example.devassistant.service;

import com.example.devassistant.dto.PolicyCompareResponse;
import com.example.devassistant.model.PolicyVersion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple line-based text comparison between two policy versions.
 * Deliberately NOT using NLP/ML - a straightforward line diff is enough
 * to let the assistant describe "what changed" between versions.
 */
@Service
public class PolicyComparisonService {

    public PolicyCompareResponse compare(PolicyVersion from, PolicyVersion to) {
        List<String> fromLines = splitLines(from.getContent());
        List<String> toLines = splitLines(to.getContent());

        List<String> added = new ArrayList<>(toLines);
        added.removeAll(fromLines);

        List<String> removed = new ArrayList<>(fromLines);
        removed.removeAll(toLines);

        PolicyCompareResponse response = new PolicyCompareResponse();
        response.setFromVersion(from.getVersionNumber());
        response.setToVersion(to.getVersionNumber());
        response.setAddedLines(added);
        response.setRemovedLines(removed);
        response.setUnchangedSummaryNote(List.of(
                added.isEmpty() && removed.isEmpty()
                        ? "No textual differences were detected between these versions."
                        : (added.size() + " line(s) added, " + removed.size() + " line(s) removed.")
        ));
        return response;
    }

    private List<String> splitLines(String content) {
        List<String> lines = new ArrayList<>();
        if (content == null) return lines;
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }
}
