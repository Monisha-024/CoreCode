package com.example.devassistant.config;

import com.example.devassistant.model.*;
import com.example.devassistant.repository.*;
import com.example.devassistant.service.PdfExtractionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Seeds the database with sample accounts and sample policies on first
 * startup so the app can be demoed immediately. Only runs when the
 * relevant tables are empty - never overwrites real data.
 *
 * Sample accounts:
 *   admin@devassistant.com     / Admin@123   (ADMIN)
 *   developer@devassistant.com / Dev@12345   (DEVELOPER)
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final PolicyVersionRepository policyVersionRepository;
    private final PolicyChunkRepository policyChunkRepository;
    private final PasswordEncoder passwordEncoder;
    private final PdfExtractionService pdfExtractionService;

    public DataSeeder(UserRepository userRepository,
                       PolicyRepository policyRepository,
                       PolicyVersionRepository policyVersionRepository,
                       PolicyChunkRepository policyChunkRepository,
                       PasswordEncoder passwordEncoder,
                       PdfExtractionService pdfExtractionService) {
        this.userRepository = userRepository;
        this.policyRepository = policyRepository;
        this.policyVersionRepository = policyVersionRepository;
        this.policyChunkRepository = policyChunkRepository;
        this.passwordEncoder = passwordEncoder;
        this.pdfExtractionService = pdfExtractionService;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedInformationSecurityPolicy();
        seedCodingStandards();
        seedRemoteWorkPolicy();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;

        User admin = new User();
        admin.setName("Admin User");
        admin.setEmail("admin@devassistant.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        User developer = new User();
        developer.setName("Developer User");
        developer.setEmail("developer@devassistant.com");
        developer.setPasswordHash(passwordEncoder.encode("Dev@12345"));
        developer.setRole(Role.DEVELOPER);
        userRepository.save(developer);
    }

    private void seedInformationSecurityPolicy() {
        if (policyRepository.findAll().stream().anyMatch(p -> p.getName().equals("Information Security Policy"))) {
            return;
        }

        Policy policy = new Policy();
        policy.setName("Information Security Policy");
        policy.setDescription("Rules governing password strength, authentication, and source code handling.");
        policy = policyRepository.save(policy);

        String v1Content = """
                Information Security Policy - Version 1

                Section 1: Password Requirements
                Password must contain at least 8 characters.

                Section 2: Source Code Handling
                Company source code must not be uploaded to personal repositories.
                """;
        PolicyVersion v1 = createVersion(policy.getId(), 1, "information-security-policy-v1.pdf",
                LocalDate.now().minusMonths(6), PolicyStatus.ARCHIVED, v1Content);

        String v2Content = """
                Information Security Policy - Version 2

                Section 1: Password Requirements
                Password must contain at least 16 characters.

                Section 2: Multi-Factor Authentication
                MFA is required for all authentication systems.

                Section 3: Source Code Handling
                Company source code must not be uploaded to personal repositories.
                """;
        PolicyVersion v2 = createVersion(policy.getId(), 2, "information-security-policy-v2.pdf",
                LocalDate.now().minusMonths(1), PolicyStatus.ACTIVE, v2Content);

        policy.setCurrentVersionId(v2.getId());
        policyRepository.save(policy);
    }

    private void seedCodingStandards() {
        if (policyRepository.findAll().stream().anyMatch(p -> p.getName().equals("Coding Standards"))) {
            return;
        }
        Policy policy = new Policy();
        policy.setName("Coding Standards");
        policy.setDescription("Baseline engineering conventions for all Java codebases.");
        policy = policyRepository.save(policy);

        String content = """
                Coding Standards - Version 1

                Section 1: Naming
                Use meaningful variable names.

                Section 2: Method Design
                Methods should have a single responsibility.

                Section 3: Error Handling
                Exceptions must be handled appropriately and never silently swallowed.

                Section 4: Logging
                Sensitive information must not be logged.
                """;
        PolicyVersion v1 = createVersion(policy.getId(), 1, "coding-standards-v1.pdf",
                LocalDate.now().minusMonths(4), PolicyStatus.ACTIVE, content);
        policy.setCurrentVersionId(v1.getId());
        policyRepository.save(policy);
    }

    private void seedRemoteWorkPolicy() {
        if (policyRepository.findAll().stream().anyMatch(p -> p.getName().equals("Remote Work Policy"))) {
            return;
        }
        Policy policy = new Policy();
        policy.setName("Remote Work Policy");
        policy.setDescription("Guidelines for employees working outside the office.");
        policy = policyRepository.save(policy);

        String content = """
                Remote Work Policy - Version 1

                Section 1: Eligibility
                Employees may work remotely up to three days per week with manager approval.

                Section 2: Equipment
                Company laptops must have full-disk encryption enabled at all times.

                Section 3: Network Security
                Employees must use the company VPN when accessing internal systems remotely.
                """;
        PolicyVersion v1 = createVersion(policy.getId(), 1, "remote-work-policy-v1.pdf",
                LocalDate.now().minusMonths(3), PolicyStatus.ACTIVE, content);
        policy.setCurrentVersionId(v1.getId());
        policyRepository.save(policy);
    }

    private PolicyVersion createVersion(Long policyId, int versionNumber, String fileName,
                                         LocalDate effectiveDate, PolicyStatus status, String content) {
        PolicyVersion version = new PolicyVersion();
        version.setPolicyId(policyId);
        version.setVersionNumber(versionNumber);
        version.setFileName(fileName);
        version.setEffectiveDate(effectiveDate);
        version.setStatus(status);
        version.setContent(content);
        version = policyVersionRepository.save(version);

        List<String> chunks = pdfExtractionService.splitIntoChunks(content);
        int idx = 0;
        for (String c : chunks) {
            PolicyChunk chunk = new PolicyChunk();
            chunk.setPolicyVersionId(version.getId());
            chunk.setChunkIndex(idx++);
            chunk.setContent(c);
            policyChunkRepository.save(chunk);
        }
        return version;
    }
}
