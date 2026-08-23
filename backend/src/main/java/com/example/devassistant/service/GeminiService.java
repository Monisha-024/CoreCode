package com.example.devassistant.service;

import com.example.devassistant.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Centralizes all Gemini API communication.
 *
 * The API key is sent through the x-goog-api-key header rather than
 * being appended to the URL.
 */
@Service
public class GeminiService {

    private static final String SYSTEM_INSTRUCTION = """
            You are an internal Developer Knowledge Assistant.

            Use only the context provided below. Never invent:
            - company policies
            - policy versions or section numbers
            - Git commits or commit messages
            - pull requests
            - bugs
            - historical reasons for a code change
            - code behavior that is not visible in the supplied source code

            If the evidence provided is insufficient to answer confidently,
            explicitly say so instead of guessing.

            Clearly distinguish between:
            - Direct evidence
            - Reasonable inference
            - Unknown information

            Always end your answer by identifying which supplied sources
            you relied on.

            Keep answers concise and professional, suitable for a software engineer.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public GeminiService(RestClient restClient) {
        this.restClient = restClient;
    }

    public String generateGroundedAnswer(String context, String question) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new ExternalServiceException(
                    "Gemini",
                    "GEMINI_API_KEY is not configured. Set it in your environment."
            );
        }

        if (apiUrl == null || apiUrl.isBlank()) {
            throw new ExternalServiceException(
                    "Gemini",
                    "GEMINI_API_URL is not configured."
            );
        }

        String prompt = SYSTEM_INSTRUCTION
                + "\n\n--- CONTEXT (retrieved evidence) ---\n"
                + (context == null || context.isBlank()
                    ? "(No relevant evidence was found.)"
                    : context)
                + "\n\n--- USER QUESTION ---\n"
                + question;

        try {

            ObjectNode body = objectMapper.createObjectNode();

            ArrayNode contents = body.putArray("contents");

            ObjectNode userTurn = contents.addObject();
            userTurn.put("role", "user");

            ArrayNode parts = userTurn.putArray("parts");

            ObjectNode textPart = parts.addObject();
            textPart.put("text", prompt);

            ObjectNode generationConfig =
                    body.putObject("generationConfig");

            generationConfig.put("temperature", 0.2);
            generationConfig.put("maxOutputTokens", 1024);

            String responseBody = restClient.post()
                    .uri(apiUrl)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            JsonNode json = objectMapper.readTree(responseBody);

            JsonNode textNode = json
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (textNode.isMissingNode()
                    || textNode.asText().isBlank()) {

                JsonNode errorNode = json.path("error");

                String errorMessage =
                        errorNode.isMissingNode()
                        ? "Empty response from Gemini"
                        : errorNode.path("message")
                                   .asText("Unknown Gemini error");

                throw new ExternalServiceException(
                        "Gemini",
                        errorMessage
                );
            }

            return textNode.asText();

        } catch (ExternalServiceException e) {

            throw e;

        } catch (RestClientException e) {

            throw new ExternalServiceException(
                    "Gemini",
                    "Request failed: " + e.getMessage()
            );

        } catch (Exception e) {

            throw new ExternalServiceException(
                    "Gemini",
                    "Unexpected error calling Gemini: "
                    + e.getMessage()
            );
        }
    }
}