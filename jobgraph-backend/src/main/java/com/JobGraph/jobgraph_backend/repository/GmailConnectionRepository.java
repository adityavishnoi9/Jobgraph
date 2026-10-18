package com.JobGraph.jobgraph_backend.repository;

import com.JobGraph.jobgraph_backend.model.GmailConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GmailConnectionRepository extends JpaRepository<GmailConnection, Integer> {
    // The account ID keeps Gmail tokens separate for each user.
    Optional<GmailConnection> findByAccount_Id(Integer accountId);
    void deleteByAccount_Id(Integer accountId);
}
