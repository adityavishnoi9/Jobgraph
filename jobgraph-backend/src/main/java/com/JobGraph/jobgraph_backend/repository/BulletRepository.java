package com.JobGraph.jobgraph_backend.repository;

import com.JobGraph.jobgraph_backend.model.Bullet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BulletRepository extends JpaRepository<Bullet, Integer> {
}