package com.JobGraph.jobgraph_backend.repository;

import com.JobGraph.jobgraph_backend.model.Experience;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<Experience, Integer> {
}