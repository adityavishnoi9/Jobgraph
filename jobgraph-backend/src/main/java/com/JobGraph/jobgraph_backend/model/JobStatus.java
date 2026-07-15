//package com.JobGraph.Entities;
//
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Entity
//@Table(name = "job_status")
//public class JobStatus {
//
//    public enum Status {
//        fetched, matched, tailored, approved, applied, rejected
//    }
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Integer id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
//
//    @Column(nullable = false)
//    private String companyName;
//
//    private String role;
//
//    @Column(columnDefinition = "text")
//    private String rawJdText;
//
//    private List<String> parsedSkills;
//
//    private String parsedExperienceRequired;
//
//    private Float matchScore;
//
//    @Enumerated(EnumType.STRING)
//    private Status status = Status.fetched;
//
//    @Column(columnDefinition = "text")
//    private String tailoredResumeDiff;
//
//    private String sourceEmailId;
//
//    private LocalDateTime jobReceivedAt;
//
//    @Column(updatable = false)
//    private LocalDateTime createdAt = LocalDateTime.now();
//
//    private LocalDateTime updatedAt = LocalDateTime.now();
//
//    // --- Getters and setters ---
//
//    public Integer getId() { return id; }
//    public void setId(Integer id) { this.id = id; }
//
//    public User getUser() { return user; }
//    public void setUser(User user) { this.user = user; }
//
//    public String getCompanyName() { return companyName; }
//    public void setCompanyName(String companyName) { this.companyName = companyName; }
//
//    public String getRole() { return role; }
//    public void setRole(String role) { this.role = role; }
//
//    public String getRawJdText() { return rawJdText; }
//    public void setRawJdText(String rawJdText) { this.rawJdText = rawJdText; }
//
//    public List<String> getParsedSkills() { return parsedSkills; }
//    public void setParsedSkills(List<String> parsedSkills) { this.parsedSkills = parsedSkills; }
//
//    public String getParsedExperienceRequired() { return parsedExperienceRequired; }
//    public void setParsedExperienceRequired(String parsedExperienceRequired) { this.parsedExperienceRequired = parsedExperienceRequired; }
//
//    public Float getMatchScore() { return matchScore; }
//    public void setMatchScore(Float matchScore) { this.matchScore = matchScore; }
//
//    public Status getStatus() { return status; }
//    public void setStatus(Status status) { this.status = status; }
//
//    public String getTailoredResumeDiff() { return tailoredResumeDiff; }
//    public void setTailoredResumeDiff(String tailoredResumeDiff) { this.tailoredResumeDiff = tailoredResumeDiff; }
//
//    public String getSourceEmailId() { return sourceEmailId; }
//    public void setSourceEmailId(String sourceEmailId) { this.sourceEmailId = sourceEmailId; }
//
//    public LocalDateTime getJobReceivedAt() { return jobReceivedAt; }
//    public void setJobReceivedAt(LocalDateTime jobReceivedAt) { this.jobReceivedAt = jobReceivedAt; }
//
//    public LocalDateTime getCreatedAt() { return createdAt; }
//    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
//
//    public LocalDateTime getUpdatedAt() { return updatedAt; }
//    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
//}
