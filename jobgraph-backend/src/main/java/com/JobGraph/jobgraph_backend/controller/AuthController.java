package com.JobGraph.jobgraph_backend.controller;

import com.JobGraph.jobgraph_backend.model.Account;
import com.JobGraph.jobgraph_backend.repository.AccountRepository;
import com.JobGraph.jobgraph_backend.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record Credentials(@NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(min = 8, max = 72) String password) {}
    public record SessionUser(Integer id, String email) {}

    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    private final CsrfTokenRepository csrfTokens;
    private final AccountService accountService;

    public AuthController(AccountRepository accounts, PasswordEncoder passwords, AuthenticationManager manager,
                          SecurityContextRepository contexts, CsrfTokenRepository csrfTokens, AccountService accountService) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.manager = manager;
        this.contexts = contexts;
        this.csrfTokens = csrfTokens;
        this.accountService = accountService;
    }

    @GetMapping("/csrf")
    // The frontend sends this token in the header of each POST, PUT, or DELETE request.
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @PostMapping("/signup")
    // Create the account, then start a login session immediately.
    public ResponseEntity<SessionUser> signup(@Valid @RequestBody Credentials input,
                                               HttpServletRequest request, HttpServletResponse response) {
        validatePasswordBytes(input.password());
        String email = AccountService.normalizeEmail(input.email());
        if (accounts.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        Account account = new Account();
        account.setEmail(email);
        account.setPasswordHash(passwords.encode(input.password()));
        // The database uniqueness check also handles two simultaneous signups.
        try { accounts.saveAndFlush(account); }
        catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        authenticate(email, input.password(), request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SessionUser(account.getId(), email));
    }

    @PostMapping("/login")
    public SessionUser login(@Valid @RequestBody Credentials input,
                             HttpServletRequest request, HttpServletResponse response) {
        validatePasswordBytes(input.password());
        String email = AccountService.normalizeEmail(input.email());
        authenticate(email, input.password(), request, response);
        Account account = accounts.findByEmail(email).orElseThrow();
        return new SessionUser(account.getId(), account.getEmail());
    }

    private void authenticate(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authenticated = manager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            // Give the authenticated user a new session ID to prevent session reuse.
            if (request.getSession(false) != null) request.changeSessionId();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticated);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            // Login changes the session, so the frontend must request a fresh CSRF token.
            csrfTokens.saveToken(null, request, response);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }

    private void validatePasswordBytes(String password) {
        // BCrypt only uses the first 72 bytes; reject longer passwords instead of truncating them.
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }
    }

    @GetMapping("/me")
    public SessionUser me(Authentication authentication) {
        Account account = accountService.current(authentication);
        return new SessionUser(account.getId(), account.getEmail());
    }

    @PostMapping("/logout")
    // Clearing the security context also invalidates the login session.
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }
}
