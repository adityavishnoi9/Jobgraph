package com.JobGraph.jobgraph_backend.gmail;

import com.JobGraph.jobgraph_backend.model.Account;
import com.JobGraph.jobgraph_backend.service.AccountService;
import com.JobGraph.jobgraph_backend.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/gmail")
public class GmailConnectionController {
    private static final Logger log = LoggerFactory.getLogger(GmailConnectionController.class);
    public record Status(boolean connected, String email) {}
    public record ConnectRequest(@NotBlank @Email @Size(max = 254) String email) {}
    private final GmailAuthService gmail;
    private final AccountService accounts;
    private final ProfileService profiles;
    private final EmailStack stacks;
    private final String frontendUrl;
    private final SecureRandom random = new SecureRandom();

    public GmailConnectionController(GmailAuthService gmail, AccountService accounts, ProfileService profiles,
                                     EmailStack stacks, @Value("${app.frontend-url}") String frontendUrl) {
        this.gmail = gmail;
        this.accounts = accounts;
        this.profiles = profiles;
        this.stacks = stacks;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/status")
    public Status status(Authentication authentication) {
        return gmail.connectedAddress(accounts.current(authentication).getId())
                .map(email -> new Status(true, email)).orElse(new Status(false, null));
    }

    @PostMapping("/connect")
    // Use the entered address as a Google sign-in hint, not as proof of ownership.
    public Map<String, String> connect(@Valid @RequestBody ConnectRequest body,
                                       Authentication authentication, HttpServletRequest request) {
        Account account = accounts.current(authentication);
        if (!profiles.exists(account.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Save a profile first");
        String requestedEmail = body.email().trim();
        byte[] nonce = new byte[32];
        random.nextBytes(nonce);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
        try {
            String url = gmail.authorizationUrl(state, requestedEmail);
            // Remember who started OAuth so the callback cannot attach Gmail to another account.
            request.getSession().setAttribute("gmailOAuthState", state);
            request.getSession().setAttribute("gmailOAuthAccountId", account.getId());
            request.getSession().setAttribute("gmailOAuthEmail", requestedEmail);
            return Map.of("url", url);
        }
        catch (IllegalStateException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage()); }
        catch (Exception e) { log.error("Could not start Gmail OAuth", e); throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not start Gmail connection"); }
    }

    @GetMapping("/callback")
    // Google sends the user here after they approve or cancel access.
    public void callback(@RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false) String error,
                         Authentication authentication, HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || authentication == null || !authentication.isAuthenticated()) {
            response.sendRedirect(frontendUrl + "/login?gmail=session-expired");
            return;
        }
        Object expected = session.getAttribute("gmailOAuthState");
        Object startedFor = session.getAttribute("gmailOAuthAccountId");
        Object requestedEmail = session.getAttribute("gmailOAuthEmail");
        // A state value can only be used once.
        session.removeAttribute("gmailOAuthState");
        session.removeAttribute("gmailOAuthAccountId");
        session.removeAttribute("gmailOAuthEmail");
        Account account = accounts.current(authentication);
        // Reject callbacks from another browser session or account.
        if (!(expected instanceof String expectedState) || state == null ||
                !MessageDigest.isEqual(expectedState.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        state.getBytes(java.nio.charset.StandardCharsets.UTF_8)) ||
                !account.getId().equals(startedFor) || !(requestedEmail instanceof String)) {
            response.sendRedirect(frontendUrl + "/inbox?gmail=invalid-state");
            return;
        }
        if (error != null || code == null) {
            response.sendRedirect(frontendUrl + "/inbox?gmail=cancelled");
            return;
        }
        try {
            gmail.complete(account, code, (String) requestedEmail);
            // Old fetched messages belonged to the previous Gmail connection.
            stacks.clear(account.getId());
            response.sendRedirect(frontendUrl + "/inbox?gmail=connected");
        } catch (GmailAuthService.GmailAddressMismatchException e) {
            response.sendRedirect(frontendUrl + "/inbox?gmail=email-mismatch");
        } catch (Exception e) {
            log.error("Gmail OAuth callback failed for account {}", account.getId(), e);
            response.sendRedirect(frontendUrl + "/inbox?gmail=error");
        }
    }

    @DeleteMapping("/connection")
    // Also clear this account's fetched emails when it disconnects Gmail.
    public ResponseEntity<Void> disconnect(Authentication authentication) {
        Integer accountId = accounts.current(authentication).getId();
        gmail.disconnect(accountId);
        stacks.clear(accountId);
        return ResponseEntity.noContent().build();
    }
}
