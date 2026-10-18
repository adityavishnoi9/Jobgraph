package com.JobGraph.jobgraph_backend.gmail;

import com.JobGraph.jobgraph_backend.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/emails")
public class EmailController {
    private static final Logger log = LoggerFactory.getLogger(EmailController.class);
    private final GmailFetchService fetcher;
    private final GmailAuthService gmail;
    private final EmailStack stack;
    private final AccountService accounts;

    public EmailController(GmailFetchService fetcher, GmailAuthService gmail,
                           EmailStack stack, AccountService accounts) {
        this.fetcher = fetcher;
        this.gmail = gmail;
        this.stack = stack;
        this.accounts = accounts;
    }

    @PostMapping("/fetch")
    // Fetch only from the Gmail account linked to the current login.
    public ResponseEntity<?> fetchEmails(
            @RequestParam(defaultValue = "newer_than:10d (subject:(job OR opening OR hiring OR position OR vacancy OR recruitment OR interview) OR from:(glassdoor.com OR indeed.com OR linkedin.com OR naukri.com))") String query,
            @RequestParam(defaultValue = "10") int maxResults,
            Authentication authentication) {
        Integer accountId = accounts.current(authentication).getId();
        if (gmail.connectedAddress(accountId).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Connect Gmail first");
        try {
            List<EmailDto> emails = fetcher.fetchEmails(accountId, query, Math.max(1, Math.min(maxResults, 50)));
            // Keep fetched messages under this account's own stack.
            stack.pushAll(accountId, emails);
            return ResponseEntity.ok(java.util.Map.of("fetched", emails.size(), "stackSize", stack.size(accountId)));
        } catch (Exception e) {
            log.error("Gmail fetch failed for account {}", accountId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Could not fetch Gmail messages. Reconnect Gmail if access was revoked.");
        }
    }

    @GetMapping("/stack")
    public List<EmailDto> viewStack(Authentication authentication) {
        return stack.viewAll(accounts.current(authentication).getId());
    }

    @GetMapping("/stack/latest")
    public ResponseEntity<EmailDto> peekLatest(Authentication authentication) {
        EmailDto latest = stack.peek(accounts.current(authentication).getId());
        return latest == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(latest);
    }
}
