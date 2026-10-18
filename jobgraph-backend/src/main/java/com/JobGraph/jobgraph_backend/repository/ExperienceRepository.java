package com.JobGraph.jobgraph_backend.repository;

import com.JobGraph.jobgraph_backend.model.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ExperienceRepository extends JpaRepository<Experience, Integer> {
    List<Experience> findAllByUser_IdOrderByIdAsc(Integer userId);
    // Used when replacing this user's edited experience list in one transaction.
    @Modifying @Query("delete from Experience e where e.user.id = :userId")
    void deleteAllForUser(Integer userId);
}
