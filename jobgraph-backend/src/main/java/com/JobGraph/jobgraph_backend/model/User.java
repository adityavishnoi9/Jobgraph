package com.JobGraph.jobgraph_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String fname;

    private String lname;

    @Column(unique = true, nullable = false)
    private String email;

    private String linkedinUrl;

    // Stored as JSONB, e.g. {"leetcode": "handle", "github": "handle"}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> codingHandles;

    // Stored as JSONB, e.g. {"framework": ["Spring Boot"], "language": ["Java"]}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, java.util.List<String>> skills;

    @Column(columnDefinition = "text")
    private String resumeRawText;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Getters and setters ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFname() { return fname; }
    public void setFname(String fname) { this.fname = fname; }

    public String getLname() { return lname; }
    public void setLname(String lname) { this.lname = lname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public Map<String, String> getCodingHandles() { return codingHandles; }
    public void setCodingHandles(Map<String, String> codingHandles) { this.codingHandles = codingHandles; }

    public Map<String, java.util.List<String>> getSkills() { return skills; }
    public void setSkills(Map<String, java.util.List<String>> skills) { this.skills = skills; }

    public String getResumeRawText() { return resumeRawText; }
    public void setResumeRawText(String resumeRawText) { this.resumeRawText = resumeRawText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
