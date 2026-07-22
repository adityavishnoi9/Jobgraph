package com.JobGraph.jobgraph_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class ProfileParsingOrchestratorService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String geminiApiKey;
    private final String geminiModel;

    public ProfileParsingOrchestratorService(
            @Value("${gemini.api.key}") String geminiApiKey,
            @Value("${gemini.model:gemini-3.5-flash}") String geminiModel
    ) {
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    /**
     * Sends the raw extracted resume text (+ any extra user-provided context)
     * straight to Gemini, and gets back JSON shaped exactly like ParsedProfileDto.
     * No Python service involved - this is the only hop.
     */
    public JsonNode parseAndNormalize(String resumeText, String additionalInfo) {
        String prompt = buildPrompt(resumeText, additionalInfo == null ? "" : additionalInfo);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.1
                )
        );

        JsonNode response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", geminiApiKey)
                        .build(geminiModel))
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        String rawJsonText = response
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText();

        try {
            return objectMapper.readTree(rawJsonText);
        } catch (Exception e) {
            throw new RuntimeException("Gemini returned invalid JSON: " + rawJsonText, e);
        }
    }

    /**
     * The schema is enforced through instruction + example rather than Gemini's
     * strict responseSchema mode, because ParsedProfileDto has dynamic-key maps
     * (codingHandles, skills, additionalSections) that responseSchema can't express.
     */
    private String buildPrompt(String resumeText, String additionalInfo) {
        return """
                You are a resume parser. Read the RESUME TEXT and ADDITIONAL INFO below and
                return ONLY a single JSON object - no markdown, no commentary, no code fences.

                The JSON object MUST use exactly this shape and these key names:

                {
                  "fname": string,
                  "lname": string,
                  "email": string,
                  "linkedinUrl": string,
                  "codingHandles": { "<platform>": "<url or handle>" },
                  "skills": { "<category>": ["<skill>", "..."] },
                  "experience": [
                    {
                      "companyName": string,
                      "role": string,
                      "startDate": "YYYY-MM-DD",
                      "endDate": "YYYY-MM-DD or null if current",
                      "bullets": [ { "text": string, "tags": ["<keyword>", "..."] } ]
                    }
                  ],
                  "projects": [
                    {
                      "name": string,
                      "description": string,
                      "bullets": [ { "text": string, "tags": ["<keyword>", "..."] } ]
                    }
                  ],
                  "additionalSections": {
                    "<normalized section name, e.g. achievements, certifications, publications>": [
                      "<line item as plain text>"
                    ]
                  }
                }

                Rules:
                - Use null for any single string field you cannot find (never omit the key).
                - Use empty object {} or empty array [] for collections you cannot find (never omit the key).
                - Only use "additionalSections" for content that does NOT fit fname/lname/email/
                  linkedinUrl/codingHandles/skills/experience/projects. Examples: Achievements,
                  Certifications, Publications, Awards, Extracurriculars.
                - Do not invent information that is not present in the resume text or additional info.
                - "tags" under bullets should be short keywords extracted from that bullet
                  (e.g. technologies or skills mentioned in it), or an empty array if none apply.
                - Dates must be ISO format (YYYY-MM-DD). If only a month/year is given, use the 1st
                  of that month. If a role is current/ongoing, set "endDate" to null.

                RESUME TEXT:
                %s

                ADDITIONAL INFO (may be empty):
                %s
                """.formatted(resumeText, additionalInfo);
    }
}