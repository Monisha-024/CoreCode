package com.example.devassistant.controller;

import com.example.devassistant.dto.*;
import com.example.devassistant.exception.BadRequestException;
import com.example.devassistant.model.PolicyVersion;
import com.example.devassistant.service.PolicyComparisonService;
import com.example.devassistant.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyComparisonService comparisonService;

    public PolicyController(PolicyService policyService, PolicyComparisonService comparisonService) {
        this.policyService = policyService;
        this.comparisonService = comparisonService;
    }

    @PostMapping
    public ResponseEntity<PolicyDTO> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createPolicy(request));
    }

    @GetMapping
    public ResponseEntity<List<PolicyDTO>> listPolicies() {
        return ResponseEntity.ok(policyService.listPolicies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyDTO> getPolicy(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicy(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/versions", consumes = "multipart/form-data")
    public ResponseEntity<PolicyVersionDTO> uploadVersion(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "effectiveDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A PDF file is required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.uploadVersion(id, file, effectiveDate));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<PolicyVersionDTO>> listVersions(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.listVersions(id));
    }

    @PutMapping("/{id}/versions/{version}/activate")
    public ResponseEntity<PolicyVersionDTO> activateVersion(@PathVariable Long id, @PathVariable Integer version) {
        return ResponseEntity.ok(policyService.activateVersion(id, version));
    }

    @PutMapping("/{id}/versions/{version}/archive")
    public ResponseEntity<Void> archiveVersion(@PathVariable Long id, @PathVariable Integer version) {
        policyService.archiveVersion(id, version);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/compare")
    public ResponseEntity<PolicyCompareResponse> compareVersions(
            @PathVariable Long id,
            @RequestParam Integer from,
            @RequestParam Integer to) {
        PolicyVersion fromVersion = policyService.getVersion(id, from);
        PolicyVersion toVersion = policyService.getVersion(id, to);
        return ResponseEntity.ok(comparisonService.compare(fromVersion, toVersion));
    }
}
