package com.JobGraph.jobgraph_backend.repository;

import com.JobGraph.jobgraph_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}