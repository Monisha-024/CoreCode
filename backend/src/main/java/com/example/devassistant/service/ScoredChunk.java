package com.example.devassistant.service;

import com.example.devassistant.model.PolicyChunk;

/** A policy chunk paired with its keyword-match relevance score for a given question. */
public class ScoredChunk {
    private final PolicyChunk chunk;
    private final double score;

    public ScoredChunk(PolicyChunk chunk, double score) {
        this.chunk = chunk;
        this.score = score;
    }

    public PolicyChunk getChunk() { return chunk; }
    public double getScore() { return score; }
}
