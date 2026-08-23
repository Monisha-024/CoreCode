package com.example.devassistant.service;

import com.example.devassistant.model.PolicyChunk;
import com.example.devassistant.model.PolicyVersion;
import com.example.devassistant.repository.PolicyChunkRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple Retrieval-Augmented-Generation (RAG) retrieval step.
 *
 * No vector database / embeddings are used for this first version - relevance
 * is scored with a plain keyword-overlap ratio:
 *
 *     score = (number of matching meaningful keywords) / (total meaningful query keywords)
 *
 * This keeps the retrieval logic transparent and fully explainable for a
 * student project, while isolating it behind this service so it could later
 * be swapped for semantic/embedding-based search without touching callers.
 */
@Service
public class PolicyRetrievalService {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were", "what", "which", "who",
            "of", "to", "in", "on", "for", "and", "or", "my", "i", "can", "do", "does",
            "this", "that", "it", "be", "should", "would", "could", "with", "about",
            "current", "policy", "company"
    );

    private static final double MIN_SCORE_THRESHOLD = 0.12;
    private static final int TOP_N = 5;

    private final PolicyChunkRepository policyChunkRepository;

    public PolicyRetrievalService(PolicyChunkRepository policyChunkRepository) {
        this.policyChunkRepository = policyChunkRepository;
    }

    /** Retrieve the most relevant chunks across the given (active) policy versions for a question. */
    public List<ScoredChunk> retrieveRelevantChunks(String question, List<PolicyVersion> candidateVersions) {
        Set<String> queryKeywords = extractKeywords(question);
        if (queryKeywords.isEmpty() || candidateVersions.isEmpty()) {
            return List.of();
        }

        List<Long> versionIds = candidateVersions.stream().map(PolicyVersion::getId).collect(Collectors.toList());
        List<PolicyChunk> chunks = policyChunkRepository.findByPolicyVersionIdIn(versionIds);

        List<ScoredChunk> scored = new ArrayList<>();
        for (PolicyChunk chunk : chunks) {
            double score = score(chunk.getContent(), queryKeywords);
            if (score >= MIN_SCORE_THRESHOLD) {
                scored.add(new ScoredChunk(chunk, score));
            }
        }

        scored.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return scored.size() > TOP_N ? scored.subList(0, TOP_N) : scored;
    }

    private double score(String chunkContent, Set<String> queryKeywords) {
        Set<String> chunkKeywords = extractKeywords(chunkContent);
        long matches = queryKeywords.stream().filter(chunkKeywords::contains).count();
        return (double) matches / queryKeywords.size();
    }

    private Set<String> extractKeywords(String text) {
        if (text == null) return Set.of();
        String[] tokens = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+");
        Set<String> keywords = new HashSet<>();
        for (String token : tokens) {
            if (token.length() > 2 && !STOP_WORDS.contains(token)) {
                keywords.add(token);
            }
        }
        return keywords;
    }
}
