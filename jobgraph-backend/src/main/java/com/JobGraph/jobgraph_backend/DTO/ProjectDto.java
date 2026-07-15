package com.JobGraph.jobgraph_backend.DTO;

import java.util.List;

public class ProjectDto {
    private String name;
    private String description;
    private List<BulletDto> bullets;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<BulletDto> getBullets() { return bullets; }
    public void setBullets(List<BulletDto> bullets) { this.bullets = bullets; }
}