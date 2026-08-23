package com.example.devassistant.service;

import com.example.devassistant.dto.AskRequest;
import com.example.devassistant.dto.AskResponse;
import com.example.devassistant.dto.SourceDTO;
import com.example.devassistant.model.*;
import com.example.devassistant.repository.CodeFileRepository;
import com.example.devassistant.repository.QueryRepository;
import com.example.devassistant.repository.QuerySourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AssistantService {

    private final QuestionClassifierService classifierService;
    private final PolicyRetrievalService policyRetrievalService;
    private final PolicyService policyService;
    private final GithubService githubService;
    private final GeminiService geminiService;
    private final CodeFileRepository codeFileRepository;
    private final QueryRepository queryRepository;
    private final QuerySourceRepository querySourceRepository;

    public AssistantService(
            QuestionClassifierService classifierService,
            PolicyRetrievalService policyRetrievalService,
            PolicyService policyService,
            GithubService githubService,
            GeminiService geminiService,
            CodeFileRepository codeFileRepository,
            QueryRepository queryRepository,
            QuerySourceRepository querySourceRepository) {

        this.classifierService = classifierService;
        this.policyRetrievalService = policyRetrievalService;
        this.policyService = policyService;
        this.githubService = githubService;
        this.geminiService = geminiService;
        this.codeFileRepository = codeFileRepository;
        this.queryRepository = queryRepository;
        this.querySourceRepository = querySourceRepository;
    }

    @Transactional
    public AskResponse ask(AskRequest request, Long userId) {

        boolean hasCodeContext =
        request.getRepositoryId() != null;
        QuestionType type =
                classifierService.classify(request.getQuestion(), hasCodeContext);

        StringBuilder context = new StringBuilder();
        List<SourceDTO> sources = new ArrayList<>();

        if (type == QuestionType.POLICY ||
                type == QuestionType.COMBINED) {

            appendPolicyContext(
                    request.getQuestion(),
                    context,
                    sources
            );
        }

        if (type == QuestionType.CODE ||
                type == QuestionType.COMBINED) {

            appendCodeContext(
                    request,
                    context,
                    sources
            );
        }

        if (type == QuestionType.GIT_HISTORY ||
                type == QuestionType.COMBINED) {

            appendGitContext(
                    request,
                    context,
                    sources
            );
        }

        String answer;

        if (context.length() == 0) {

            answer =
                    "I could not find any company policy, source code, or Git history relevant to this question. "
                    + "Please connect a repository and/or ask about an active company policy.";

        } else {

            answer = geminiService.generateGroundedAnswer(
                    context.toString(),
                    request.getQuestion()
            );
        }

        Query query = new Query();

        query.setUserId(userId);
        query.setQuestion(request.getQuestion());
        query.setQuestionType(type);
        query.setAnswer(answer);

        query = queryRepository.save(query);

        for (SourceDTO source : sources) {

            QuerySource querySource = new QuerySource();

            querySource.setQueryId(query.getId());
            querySource.setSourceType(
                    SourceType.valueOf(source.getSourceType())
            );
            querySource.setSourceId(source.getSourceId());
            querySource.setSourceLabel(source.getSourceLabel());

            querySourceRepository.save(querySource);
        }

        return new AskResponse(
                type.name(),
                answer,
                sources
        );
    }

    private void appendPolicyContext(
            String question,
            StringBuilder context,
            List<SourceDTO> sources) {

        List<PolicyVersion> activeVersions =
                policyService.getAllActiveVersions();

        List<ScoredChunk> relevant =
                policyRetrievalService.retrieveRelevantChunks(
                        question,
                        activeVersions
                );

        if (relevant.isEmpty()) {
            return;
        }

        context.append("\n[ACTIVE POLICY EXCERPTS]\n");

        for (ScoredChunk sc : relevant) {

            PolicyChunk chunk = sc.getChunk();

            PolicyVersion version =
                    activeVersions.stream()
                            .filter(v -> v.getId().equals(
                                    chunk.getPolicyVersionId()))
                            .findFirst()
                            .orElse(null);

            if (version == null) {
                continue;
            }

            String label =
                    "Policy Version #" +
                    version.getVersionNumber() +
                    " (ACTIVE), chunk " +
                    chunk.getChunkIndex();

            context.append("- [")
                    .append(label)
                    .append("]: ")
                    .append(chunk.getContent())
                    .append("\n");

            sources.add(
                    new SourceDTO(
                            "POLICY",
                            version.getId(),
                            label
                    )
            );
        }
    }

    /**
     * Code retrieval:
     *
     * 1. If a specific file is selected, use that file.
     * 2. Otherwise search ALL indexed files in the repository.
     * 3. Rank files using simple keyword matching.
     * 4. Send the best matching files to Gemini.
     */
    private void appendCodeContext(
            AskRequest request,
            StringBuilder context,
            List<SourceDTO> sources) {

        if (request.getRepositoryId() == null) {
            return;
        }

        /*
         * CASE 1:
         * User selected a specific file.
         */
        if (request.getFilePath() != null &&
                !request.getFilePath().isBlank()) {

            try {

                CodeFile file =
                        githubService.getFile(
                                request.getRepositoryId(),
                                request.getFilePath()
                        );

                context.append("\n[SOURCE CODE: ")
                        .append(file.getFilePath())
                        .append("]\n")
                        .append(file.getContent())
                        .append("\n");

                sources.add(
                        new SourceDTO(
                                "CODE",
                                file.getId(),
                                file.getFilePath()
                        )
                );

            } catch (Exception e) {

                // Do not fabricate missing code.
            }

            return;
        }

        /*
         * CASE 2:
         * No file selected.
         *
         * Search the complete repository.
         */

        List<CodeFile> files =
                codeFileRepository.findByRepositoryId(
                        request.getRepositoryId()
                );

        if (files.isEmpty()) {
            return;
        }

        List<String> keywords =
                extractKeywords(request.getQuestion());

        List<CodeFile> rankedFiles =
                files.stream()
.sorted(
        Comparator.comparingInt(
                (CodeFile file) -> scoreFile(file, keywords)
        ).reversed()
).limit(5)
                        .collect(Collectors.toList());

        context.append(
                "\n[REPOSITORY CODE EVIDENCE]\n"
        );

        for (CodeFile file : rankedFiles) {

            int score =
                    scoreFile(file, keywords);

            /*
             * If the question contains useful keywords,
             * skip files with zero relevance.
             *
             * If nothing matches, use the first few files
             * so Gemini can still determine whether evidence
             * is insufficient.
             */
            if (!keywords.isEmpty() &&
                    score == 0) {
                continue;
            }

            context.append(
                    "\n[SOURCE CODE: "
            )
                    .append(file.getFilePath())
                    .append("]\n")
                    .append(file.getContent())
                    .append("\n");

            sources.add(
                    new SourceDTO(
                            "CODE",
                            file.getId(),
                            file.getFilePath()
                    )
            );
        }
    }

    /**
     * Converts a natural-language question into simple
     * search keywords.
     */
    private List<String> extractKeywords(String question) {

        if (question == null || question.isBlank()) {
            return List.of();
        }

        return List.of(
                        question
                                .toLowerCase(Locale.ROOT)
                                .replaceAll(
                                        "[^a-z0-9_]+",
                                        " "
                                )
                                .split("\\s+")
                )
                .stream()
                .filter(word -> word.length() >= 3)
                .filter(word -> !isStopWord(word))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Simple relevance score.
     *
     * Matches both file path and source-code content.
     */
    private int scoreFile(
            CodeFile file,
            List<String> keywords) {

        if (keywords.isEmpty()) {
            return 1;
        }

        String path =
                file.getFilePath() == null
                        ? ""
                        : file.getFilePath()
                              .toLowerCase(Locale.ROOT);

        String content =
                file.getContent() == null
                        ? ""
                        : file.getContent()
                              .toLowerCase(Locale.ROOT);

        int score = 0;

        for (String keyword : keywords) {

            if (path.contains(keyword)) {
                score += 5;
            }

            if (content.contains(keyword)) {
                score += 1;
            }
        }

        return score;
    }

    private boolean isStopWord(String word) {

        return switch (word) {

            case "the",
                 "and",
                 "for",
                 "with",
                 "what",
                 "where",
                 "when",
                 "which",
                 "does",
                 "this",
                 "that",
                 "from",
                 "into",
                 "about",
                 "how",
                 "why",
                 "are",
                 "was",
                 "can",
                 "could",
                 "would",
                 "should",
                 "file",
                 "project",
                 "code" -> true;

            default -> false;
        };
    }

    private void appendGitContext(
            AskRequest request,
            StringBuilder context,
            List<SourceDTO> sources) {

        if (request.getRepositoryId() == null) {
            return;
        }

        List<Commit> commits;

        if (request.getFilePath() != null) {

            commits =
                    githubService.commitsForFile(
                            request.getRepositoryId(),
                            request.getFilePath()
                    );

        } else {

            commits =
                    githubService.listCommits(
                            request.getRepositoryId()
                    );
        }

        if (commits.isEmpty()) {
            return;
        }

        context.append("\n[GIT HISTORY]\n");

        int limit =
                Math.min(commits.size(), 5);

        for (int i = 0; i < limit; i++) {

            Commit c = commits.get(i);

            String hash =
                    c.getCommitHash();

            String label =
                    "Commit " +
                    hash.substring(
                            0,
                            Math.min(7, hash.length())
                    );

            context.append("- ")
                    .append(label)
                    .append(" | Author: ")
                    .append(c.getAuthor())
                    .append(" | Date: ")
                    .append(c.getCommitDate())
                    .append(" | Message: ")
                    .append(c.getMessage())
                    .append(" | Changed files: ")
                    .append(c.getChangedFiles())
                    .append("\n");

            sources.add(
                    new SourceDTO(
                            "COMMIT",
                            c.getId(),
                            label + " - " + c.getMessage()
                    )
            );
        }
    }
}