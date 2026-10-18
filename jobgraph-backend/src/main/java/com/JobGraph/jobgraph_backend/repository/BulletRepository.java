package com.JobGraph.jobgraph_backend.repository;

import com.JobGraph.jobgraph_backend.model.Bullet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BulletRepository extends JpaRepository<Bullet, Integer> {
    List<Bullet> findAllByUser_IdOrderByIdAsc(Integer userId);
    // Remove old highlights before rebuilding experience and projects.
    @Modifying @Query("delete from Bullet b where b.user.id = :userId")
    void deleteAllForUser(Integer userId);
}
