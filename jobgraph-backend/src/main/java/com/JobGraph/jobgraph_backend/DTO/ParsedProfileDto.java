package com.JobGraph.jobgraph_backend.DTO;

import java.util.List;
import java.util.Map;

public class ParsedProfileDto {
    private String fname;
    private String lname;
    private String email;
    private String linkedinUrl;
    private Map<String, String> codingHandles;
    private Map<String, List<String>> skills;
    private List<ExperienceDto> experience;
    private List<ProjectDto> projects;

    // Catch-all for resume sections with no dedicated DTO/table yet
    // (e.g. "achievements", "certifications", "publications").
    // Key = normalized section name, Value = list of line items.
    private Map<String, List<String>> additionalSections;

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

    public Map<String, List<String>> getSkills() { return skills; }
    public void setSkills(Map<String, List<String>> skills) { this.skills = skills; }

    public List<ExperienceDto> getExperience() { return experience; }
    public void setExperience(List<ExperienceDto> experience) { this.experience = experience; }

    public List<ProjectDto> getProjects() { return projects; }
    public void setProjects(List<ProjectDto> projects) { this.projects = projects; }

    public Map<String, List<String>> getAdditionalSections() { return additionalSections; }
    public void setAdditionalSections(Map<String, List<String>> additionalSections) { this.additionalSections = additionalSections; }
}