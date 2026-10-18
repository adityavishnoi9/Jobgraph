package com.JobGraph.jobgraph_backend.repository;

import com.JobGraph.jobgraph_backend.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    // Login always uses the normalized account email.
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);
}
