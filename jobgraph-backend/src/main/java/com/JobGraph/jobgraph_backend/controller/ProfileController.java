package com.JobGraph.jobgraph_backend.controller;

import com.JobGraph.jobgraph_backend.DTO.ParsedProfileDto;
import com.JobGraph.jobgraph_backend.DTO.ProfileUploadResponse;
import com.JobGraph.jobgraph_backend.model.User;
import com.JobGraph.jobgraph_backend.service.ProfileService;
import com.JobGraph.jobgraph_backend.service.ResumeParsingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/profile")
@CrossOrigin(origins = "http://localhost:5173")
public class ProfileController {

    private final ResumeParsingService resumeParsingService;
    private final ProfileService profileService;

    public ProfileController(ResumeParsingService resumeParsingService, ProfileService profileService) {
        this.resumeParsingService = resumeParsingService;
        this.profileService = profileService;
    }

    /**
     * Accepts a resume file + any additional free-text info the user wants
     * to add. For now, only extracts and returns the raw resume text —
     * no LLM structuring/parsing yet. Nothing is saved to the DB yet either;
     * that comes once we add the "review and confirm" step.
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadResume(
            @RequestParam("resume") MultipartFile resume,
            @RequestParam(value = "additionalInfo", required = false) String additionalInfo
    ) {
        if (resume.isEmpty()) {
            return ResponseEntity.badRequest().body("Resume file is required.");
        }

        try {
            String extractedText = resumeParsingService.extractText(resume);
            ProfileUploadResponse response = new ProfileUploadResponse(extractedText, additionalInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to extract text from resume: " + e.getMessage());
        }
    }

    /**
     * Accepts the final, user-confirmed profile JSON (same shape as what
     * the Python /parse-resume service returns) and saves it into the
     * users/experience/projects/bullets tables. This is called once the
     * user has reviewed the LLM's output on the frontend and clicked
     * "Confirm" - nothing is auto-saved before this point.
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
