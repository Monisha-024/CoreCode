package com.example.devassistant.service;

import com.example.devassistant.model.QuestionType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Simple rule-based / keyword classifier for incoming questions.
 * Deliberately NOT machine-learned - a transparent, explainable set of
 * keyword rules is enough for this project and keeps behavior predictable.
 */
@Service
public class QuestionClassifierService {

    private static final List<String> POLICY_KEYWORDS = List.of(
            "policy", "policies", "allowed", "compliance", "rule", "rules", "requirement",
            "must", "mfa", "password", "security policy", "coding standard", "remote work",
            "data handling", "regulation", "permitted", "prohibited"
    );

    private static final List<String> GIT_KEYWORDS = List.of(
            "why was", "why did", "changed", "change history", "commit", "git history",
            "who changed", "when was", "previous implementation", "used to", "history of"
    );

    private static final List<String> CODE_KEYWORDS = List.of(
            "what does", "explain", "method", "function", "class", "this code",
            "how does", "logic", "implementation", "parameter", "return", "bug"
    );

    public QuestionType classify(String question, boolean hasCodeContext) {
        String q = question.toLowerCase(Locale.ROOT);

        boolean isPolicy = containsAny(q, POLICY_KEYWORDS);
        boolean isGit = containsAny(q, GIT_KEYWORDS);
        boolean isCode = containsAny(q, CODE_KEYWORDS) || (hasCodeContext && !isGit);

        // A question that touches both policy AND (code or git) is COMBINED -
        // this is the project's key differentiating feature.
        if (isPolicy && (isCode || isGit || hasCodeContext)) {
            return QuestionType.COMBINED;
        }
        if (isGit) {
            return QuestionType.GIT_HISTORY;
        }
        if (isPolicy) {
            return QuestionType.POLICY;
        }
        if (isCode || hasCodeContext) {
            return QuestionType.CODE;
        }
        return QuestionType.GENERAL;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
