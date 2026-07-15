package com.JobGraph.jobgraph_backend.gmail;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/emails")
public class EmailController {

    private final GmailFetchService gmailFetchService;
    private final EmailStack emailStack;

    public EmailController(GmailFetchService gmailFetchService, EmailStack emailStack) {
        this.gmailFetchService = gmailFetchService;
        this.emailStack = emailStack;
    }

    /**
     * Fetches emails matching a query and pushes them onto the stack.
     * First call will open a browser window for Google login/consent -
     * this only happens once (token gets cached in the "tokens" folder).
     *
     * Default query: looks for job-related keywords in the subject, OR
     * emails from known job portals (Glassdoor, Indeed, LinkedIn), AND
     * restricts to the last 10 days (Gmail's "newer_than:Nd" syntax).
     * Combined with maxResults=10, this naturally gives you "latest 10
     * emails in the last 10 days, or fewer if that's all there is" -
     * Gmail already returns results newest-first, so no extra filtering
     * logic is needed on the Java side.
     */
    @PostMapping("/fetch")
    public ResponseEntity<?> fetchEmails(
            @RequestParam(defaultValue =
                    "newer_than:10d (subject:(job OR opening OR hiring OR position OR vacancy OR recruitment OR interview) " +
                            "OR from:(glassdoor.com OR indeed.com OR linkedin.com OR naukri.com))"
            ) String query,
            @RequestParam(defaultValue = "10") int maxResults
    ) {
        try {
            List<EmailDto> emails = gmailFetchService.fetchEmails(query, maxResults);
            emailStack.pushAll(emails);
            return ResponseEntity.ok("Fetched and stacked " + emails.size() + " emails. Stack size: " + emailStack.size());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to fetch emails: " + e.getMessage());
        }
    }

    /** View all emails currently in the stack, most recent first. */
    @GetMapping("/stack")
    public ResponseEntity<List<EmailDto>> viewStack() {
        return ResponseEntity.ok(emailStack.viewAll());
    }

    /** Peek at just the most recent email without removing it. */
    @GetMapping("/stack/latest")
    public ResponseEntity<EmailDto> peekLatest() {
        if (emailStack.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(emailStack.peek());
    }
}