package com.JobGraph.jobgraph_backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class ProfileParsingOrchestratorService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String openAiApiKey;
    private final String openAiModel;

    public ProfileParsingOrchestratorService(
            @Value("${OPENAI_API_KEY}") String openAiApiKey,
            @Value("${OPENAI_MODEL:gpt-6-luna}") String openAiModel
    ) {
        this.openAiApiKey = openAiApiKey;
        this.openAiModel = openAiModel;
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    /**
     * Sends the raw extracted resume text (+ any extra user-provided context)
     * straight to OpenAI, and gets back JSON shaped exactly like ParsedProfileDto.
     * No Python service involved - this is the only hop.
     */
    public JsonNode parseAndNormalize(String resumeText, String additionalInfo) {
        String prompt = buildPrompt(resumeText, additionalInfo == null ? "" : additionalInfo);

        Map<String, Object> requestBody = Map.of(
                "model", openAiModel,
                "input", prompt,
                "text", Map.of("format", Map.of("type", "json_object")),
                "reasoning", Map.of("effort", "none"),
                "store", false
        );

        JsonNode response = restClient.post()
                .uri("/responses")
                .header("Authorization", "Bearer " + openAiApiKey)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !"completed".equals(response.path("status").asText())) {
            throw new IllegalStateException("OpenAI did not complete the response.");
        }

        String rawJsonText = null;
        for (JsonNode item : response.path("output")) {
            for (JsonNode content : item.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    rawJsonText = content.path("text").asText();
                    break;
                }
            }
            if (rawJsonText != null) break;
        }
        if (rawJsonText == null || rawJsonText.isBlank()) {
            throw new IllegalStateException("OpenAI response did not contain profile JSON.");
        }

        try {
            return objectMapper.readTree(rawJsonText);
        } catch (Exception e) {
            throw new RuntimeException("OpenAI returned invalid profile JSON.", e);
        }
    }

    /**
     * JSON mode keeps the output parseable while the prompt describes the
     * dynamic-key maps used by ParsedProfileDto.
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
