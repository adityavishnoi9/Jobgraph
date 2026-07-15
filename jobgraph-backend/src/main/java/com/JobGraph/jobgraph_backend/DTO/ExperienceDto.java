package com.JobGraph.jobgraph_backend.DTO;

import java.time.LocalDate;
import java.util.List;

public class ExperienceDto {
    private String companyName;
    private String role;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<BulletDto> bullets;

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<BulletDto> getBullets() { return bullets; }
    public void setBullets(List<BulletDto> bullets) { this.bullets = bullets; }
}