package com.JobGraph.jobgraph_backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.JobGraph.jobgraph_backend.DTO.ParsedProfileDto;
import com.JobGraph.jobgraph_backend.model.User;
import com.JobGraph.jobgraph_backend.service.ProfileParsingOrchestratorService;
import com.JobGraph.jobgraph_backend.service.ProfileService;
import com.JobGraph.jobgraph_backend.service.ResumeParsingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ResumeParsingService resumeParsingService;
    private final ProfileParsingOrchestratorService orchestratorService;
    private final ProfileService profileService;

    public ProfileController(ResumeParsingService resumeParsingService,
                             ProfileParsingOrchestratorService orchestratorService,
                             ProfileService profileService) {
        this.resumeParsingService = resumeParsingService;
        this.orchestratorService = orchestratorService;
        this.profileService = profileService;
    }

    /**
     * Full pipeline in one call:
     *  1. Extract resume text (Tika)
     *  2. Parse into structured JSON via Gemini
     *  3. Normalize keys/structure to exactly match ParsedProfileDto
     * Returns the normalized JSON for the frontend to show/edit -
     * nothing is saved to the DB yet.
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadResume(
            @RequestParam("resume") MultipartFile resume,
            @RequestParam(value = "additionalInfo", required = false) String additionalInfo
    ) {
//        System.out.println("is this working");
        if (resume.isEmpty()) {
            return ResponseEntity.badRequest().body("Resume file is required.");
        }

        try {
            String extractedText = resumeParsingService.extractText(resume);
            JsonNode normalizedProfile = orchestratorService.parseAndNormalize(extractedText, additionalInfo);
//            System.out.println("hi baby");
//            return ResponseEntity.ok("hi");
            return ResponseEntity.ok(normalizedProfile);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to process resume: " + e.getMessage());
        }
    }

    /**
     * Saves the user-confirmed/edited profile into the DB.
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveProfile(@RequestBody ParsedProfileDto dto) {
        try {
            User savedUser = profileService.saveProfile(dto);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to save profile: " + e.getMessage());
        }
    }
}