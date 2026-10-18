package com.JobGraph.jobgraph_backend.repository;

import com.JobGraph.jobgraph_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    // Profiles are looked up by their owner, not by a profile ID supplied by the browser.
    Optional<User> findByAccount_Id(Integer accountId);
}
