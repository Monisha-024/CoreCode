package com.example.devassistant.service;

import com.example.devassistant.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts and cleans text from uploaded PDF policy documents using Apache PDFBox,
 * then splits it into meaningful chunks for storage/retrieval.
 */
@Service
public class PdfExtractionService {

    private static final int MAX_CHUNK_CHARS = 800;

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded PDF file is empty");
        }
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.isEncrypted()) {
                throw new BadRequestException("Encrypted PDFs are not supported");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            String raw = stripper.getText(document);
            String cleaned = cleanText(raw);
            if (cleaned.isBlank()) {
                throw new BadRequestException("No extractable text found in PDF (it may be a scanned image)");
            }
            return cleaned;
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded PDF: " + e.getMessage());
        }
    }

    private String cleanText(String text) {
        return text
                .replace("\r\n", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * Splits cleaned policy text into paragraph-sized chunks (roughly
     * MAX_CHUNK_CHARS characters each) so each chunk stays "meaningful"
     * for keyword-based retrieval, without cutting mid-sentence when avoidable.
     */
    public List<String> splitIntoChunks(String text) {
        List<String> paragraphs = new ArrayList<>();
        for (String p : text.split("\n\n")) {
            String trimmed = p.trim();
            if (!trimmed.isBlank()) {
                paragraphs.add(trimmed);
            }
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (current.length() + paragraph.length() > MAX_CHUNK_CHARS && current.length() > 0) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (paragraph.length() > MAX_CHUNK_CHARS) {
                // Very long paragraph: hard-split on sentence boundaries.
                for (String sentence : paragraph.split("(?<=[.!?])\\s+")) {
                    if (current.length() + sentence.length() > MAX_CHUNK_CHARS && current.length() > 0) {
                        chunks.add(current.toString().trim());
                        current = new StringBuilder();
                    }
                    current.append(sentence).append(" ");
                }
            } else {
                current.append(paragraph).append("\n\n");
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        if (chunks.isEmpty()) {
            chunks.add(text.trim());
        }
        return chunks;
    }
}
