package com.JobGraph.jobgraph_backend.repository;

import com.JobGraph.jobgraph_backend.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
    List<Project> findAllByUser_IdOrderByIdAsc(Integer userId);
    // Used when replacing this user's edited projects in one transaction.
    @Modifying @Query("delete from Project p where p.user.id = :userId")
    void deleteAllForUser(Integer userId);
}
