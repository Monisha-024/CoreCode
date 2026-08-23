package com.example.devassistant.service;

import com.example.devassistant.model.QuestionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionClassifierServiceTest {

    private final QuestionClassifierService classifier = new QuestionClassifierService();

    @Test
    void classifiesPasswordQuestionAsPolicy() {
        assertEquals(QuestionType.POLICY, classifier.classify("What is the password policy?", false));
    }

    @Test
    void classifiesExplainMethodAsCode() {
        assertEquals(QuestionType.CODE, classifier.classify("What does this method do?", true));
    }

    @Test
    void classifiesWhyChangedAsGitHistory() {
        assertEquals(QuestionType.GIT_HISTORY, classifier.classify("Why was this function changed?", false));
    }

    @Test
    void classifiesPolicyPlusCodeAsCombined() {
        assertEquals(QuestionType.COMBINED,
                classifier.classify("Can I modify this authentication function according to company security policy?", true));
    }

    @Test
    void classifiesUnrelatedQuestionAsGeneral() {
        assertEquals(QuestionType.GENERAL, classifier.classify("What is the weather today?", false));
    }
}
