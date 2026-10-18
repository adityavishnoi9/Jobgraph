package com.JobGraph.jobgraph_backend.service;

import com.JobGraph.jobgraph_backend.model.Account;
import com.JobGraph.jobgraph_backend.repository.AccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class AccountService {
    private final AccountRepository accounts;
    public AccountService(AccountRepository accounts) { this.accounts = accounts; }

    // Use the authenticated email from Spring Security; never trust an account ID from the request.
    public Account current(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("Login required");
        }
        return accounts.findByEmail(authentication.getName())
                .orElseThrow(() -> new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("Account not found"));
    }

    // Store login emails consistently so case changes do not create another account.
    public static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
