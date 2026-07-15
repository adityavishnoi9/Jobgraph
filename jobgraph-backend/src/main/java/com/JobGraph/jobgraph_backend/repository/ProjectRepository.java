package com.JobGraph.jobgraph_backend.repository;

import com.JobGraph.jobgraph_backend.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
}