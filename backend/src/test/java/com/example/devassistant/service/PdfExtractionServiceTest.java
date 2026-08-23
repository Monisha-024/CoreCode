package com.example.devassistant.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfExtractionServiceTest {

    private final PdfExtractionService service = new PdfExtractionService();

    @Test
    void splitsLongTextIntoMultipleChunks() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            longText.append("This is paragraph number ").append(i)
                    .append(" of a long sample policy document used purely for testing chunking behavior.\n\n");
        }

        List<String> chunks = service.splitIntoChunks(longText.toString());

        assertTrue(chunks.size() > 1, "Long text should be split into more than one chunk");
        for (String chunk : chunks) {
            assertFalse(chunk.isBlank());
        }
    }

    @Test
    void keepsShortTextAsSingleChunk() {
        String shortText = "Password must contain at least 16 characters.";
        List<String> chunks = service.splitIntoChunks(shortText);
        assertTrue(chunks.size() == 1);
    }
}
