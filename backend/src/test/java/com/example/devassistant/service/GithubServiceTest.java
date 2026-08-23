package com.example.devassistant.service;

import com.example.devassistant.repository.CodeFileRepository;
import com.example.devassistant.repository.CommitRepository;
import com.example.devassistant.repository.RepositoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the multi-language Code Explorer upgrade: file-type detection and
 * language mapping in GithubService. GitHub calls themselves are not
 * exercised here (that would require a live token / network), but the
 * pure extension-matching logic is fully covered.
 */
@ExtendWith(MockitoExtension.class)
class GithubServiceTest {

    @Mock private RestClient restClient;
    @Mock private RepositoryRepository repositoryRepository;
    @Mock private CodeFileRepository codeFileRepository;
    @Mock private CommitRepository commitRepository;

    private GithubService newService() {
        return new GithubService(restClient, repositoryRepository, codeFileRepository, commitRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "src/main/java/App.java, true",
            "app/models/risk.py, true",
            "web/src/index.js, true",
            "web/src/Widget.jsx, true",
            "web/src/utils.ts, true",
            "web/src/Component.tsx, true",
            "README.md, false",
            "assets/logo.png, false",
            "build/output.class, false",
            "Dockerfile, false"
    })
    void identifiesSupportedFilesByExtension(String path, boolean expected) {
        assertEquals(expected, newService().isSupportedCodeFile(path));
    }

    @Test
    void treatsUppercaseExtensionsAsSupported() {
        GithubService service = newService();
        assertTrue(service.isSupportedCodeFile("scripts/train.PY"));
        assertTrue(service.isSupportedCodeFile("src/App.JS"));
    }

    @ParameterizedTest
    @CsvSource({
            "Service.java, java",
            "risk_engine.py, python",
            "index.js, javascript",
            "Widget.jsx, javascript",
            "types.ts, typescript",
            "App.tsx, typescript"
    })
    void detectsLanguageFromExtension(String path, String expectedLanguage) {
        assertEquals(expectedLanguage, newService().detectLanguage(path));
    }

    @Test
    void unknownExtensionDefaultsToTextLanguage() {
        assertEquals("text", newService().detectLanguage("notes.txt"));
    }

    @Test
    void javaFilesStillSupportedAfterUpgrade() {
        GithubService service = newService();
        assertTrue(service.isSupportedCodeFile("com/example/AuthenticationService.java"));
        assertEquals("java", service.detectLanguage("com/example/AuthenticationService.java"));
    }
}
