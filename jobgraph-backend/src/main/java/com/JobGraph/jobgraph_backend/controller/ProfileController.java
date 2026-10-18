package com.JobGraph.jobgraph_backend.controller;

import tools.jackson.databind.JsonNode;
import com.JobGraph.jobgraph_backend.DTO.ParsedProfileDto;
import com.JobGraph.jobgraph_backend.service.AccountService;
import com.JobGraph.jobgraph_backend.service.ProfileParsingOrchestratorService;
import com.JobGraph.jobgraph_backend.service.ProfileService;
import com.JobGraph.jobgraph_backend.service.ResumeParsingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private final ResumeParsingService resumeParsingService;
    private final ProfileParsingOrchestratorService orchestratorService;
    private final ProfileService profileService;
    private final AccountService accounts;

    public ProfileController(ResumeParsingService resumeParsingService,
                             ProfileParsingOrchestratorService orchestratorService,
                             ProfileService profileService, AccountService accounts) {
        this.resumeParsingService = resumeParsingService;
        this.orchestratorService = orchestratorService;
        this.profileService = profileService;
        this.accounts = accounts;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    // Parsing returns an editable draft; it does not save a profile yet.
    public ResponseEntity<?> uploadResume(@RequestParam("resume") MultipartFile resume,
                                          @RequestParam(value = "additionalInfo", required = false) String additionalInfo) {
        if (resume.isEmpty()) return ResponseEntity.badRequest().body("Resume file is required.");
        try {
            String extractedText = resumeParsingService.extractText(resume);
            JsonNode normalizedProfile = orchestratorService.parseAndNormalize(extractedText, additionalInfo);
            return ResponseEntity.ok(normalizedProfile);
        } catch (Exception e) {
            log.error("Failed to process uploaded resume", e);
            return ResponseEntity.internalServerError().body("Failed to process resume. See backend logs.");
        }
    }

    @GetMapping
    // Always find the profile through the signed-in account, not a client-supplied ID.
    public ResponseEntity<ParsedProfileDto> get(Authentication authentication) {
        return profileService.get(accounts.current(authentication).getId())
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping
    // Save edits to this account's one profile.
    public ParsedProfileDto save(@RequestBody ParsedProfileDto dto, Authentication authentication) {
        return profileService.save(accounts.current(authentication), dto);
    }
}
