package com.JobGraph.jobgraph_backend;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountProfileIntegrationTests {
    @Autowired MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();

    private MockHttpSession signup(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/signup").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"secure-test-password\"}"))
                .andExpect(status().isCreated())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void localFrontendPreflightPassesBeforeAuthentication() throws Exception {
        // A browser sends OPTIONS before a cross-origin JSON request with CSRF.
        mvc.perform(options("/api/gmail/connect")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("x-csrf-token")));

        // Actual API errors also include CORS headers, so the browser can show them.
        mvc.perform(get("/api/profile").header("Origin", "http://localhost:5173"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mvc.perform(options("/api/gmail/connect")
                .header("Origin", "http://example.com")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void authenticationRequiresCsrfAndCreatesRevocableSession() throws Exception {
        String email = "auth-" + UUID.randomUUID() + "@example.com";
        mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/profile/upload").file(new MockMultipartFile("resume", "sample.pdf", "application/pdf", new byte[]{1}))
                .with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/profile").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fname\":\"Anonymous\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"secure-test-password\"}"))
                .andExpect(status().isForbidden());
        MockHttpSession session = signup(email);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"secure-test-password\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/logout").with(csrf()).session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void signupRenewsTheAnonymousSessionId() throws Exception {
        var csrfResponse = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        MockHttpSession anonymousSession = (MockHttpSession) csrfResponse.getRequest().getSession(false);
        String oldId = anonymousSession.getId();
        String token = json.readTree(csrfResponse.getResponse().getContentAsString()).path("token").asText();
        mvc.perform(post("/api/auth/signup").session(anonymousSession).header("X-CSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"renew-" + UUID.randomUUID() + "@example.com\",\"password\":\"secure-test-password\"}"))
                .andExpect(status().isCreated());
        assertNotEquals(oldId, anonymousSession.getId());
    }

    @Test
    void profilesAreEditableAndIsolatedByAccount() throws Exception {
        MockHttpSession first = signup("first-" + UUID.randomUUID() + "@example.com");
        MockHttpSession second = signup("second-" + UUID.randomUUID() + "@example.com");
        String original = """
                {"fname":"Ada","lname":"Example","email":"different-contact@example.com",
                 "codingHandles":{"github":"ada"},"skills":{"languages":["Java"]},
                 "additionalSections":{"certifications":["First"]},
                 "experience":[{"companyName":"Acme","role":"Engineer","startDate":"2024-01-01",
                   "bullets":[{"text":"Built things","tags":["Java"]}]}],
                 "projects":[{"name":"Demo","description":"A test","bullets":[{"text":"Made demo","tags":[]}]}]}
                """;
        mvc.perform(put("/api/profile").session(first).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(original))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("different-contact@example.com"));
        mvc.perform(get("/api/profile").session(first)).andExpect(status().isOk())
                .andExpect(jsonPath("$.experience[0].bullets[0].text").value("Built things"))
                .andExpect(jsonPath("$.additionalSections.certifications[0]").value("First"));
        mvc.perform(get("/api/profile").session(second)).andExpect(status().isNotFound());
        mvc.perform(put("/api/profile").session(second).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"fname\":\"Other\",\"email\":\"different-contact@example.com\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/profile").session(first)).andExpect(status().isOk())
                .andExpect(jsonPath("$.fname").value("Ada"));
        mvc.perform(get("/api/gmail/status").session(first)).andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));

        String updated = """
                {"fname":"Ada","lname":"Example","email":"different-contact@example.com",
                 "skills":{"languages":["Kotlin"]},"experience":[],"projects":[],
                 "additionalSections":{"awards":["New award"]}}
                """;
        mvc.perform(put("/api/profile").session(first).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(updated)).andExpect(status().isOk());
        mvc.perform(get("/api/profile").session(first)).andExpect(status().isOk())
                .andExpect(jsonPath("$.experience").isEmpty())
                .andExpect(jsonPath("$.projects").isEmpty())
                .andExpect(jsonPath("$.skills.languages[0]").value("Kotlin"))
                .andExpect(jsonPath("$.additionalSections.awards[0]").value("New award"));
        mvc.perform(get("/api/profile").session(second)).andExpect(status().isOk())
                .andExpect(jsonPath("$.fname").value("Other"));
    }
}
