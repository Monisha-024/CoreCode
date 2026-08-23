package com.example.devassistant.exception;

/**
 * Raised when an external dependency (Gemini API, GitHub API) fails
 * or is not configured. Carries the name of the service for clean
 * error reporting to the frontend.
 */
public class ExternalServiceException extends RuntimeException {
    private final String service;

    public ExternalServiceException(String service, String message) {
        super(message);
        this.service = service;
    }

    public String getService() { return service; }
}
