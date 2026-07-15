package com.JobGraph.jobgraph_backend.gmail;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Holds fetched emails as a stack (LIFO) - the most recently fetched
 * email is always on top, so peek()/pop() always gives you the latest one.
 *
 * Uses ArrayDeque as the stack implementation (Java's recommended choice
 * over the old java.util.Stack class, which is legacy/synchronized and
 * slower for this use case).
 *
 * Note: this is in-memory only - it resets if the app restarts. Your
 * job_status table (already in the DB schema) is the actual persistent
 * store; this stack is a lightweight structure for whatever needs
 * "most recent first" access at runtime, e.g. showing the latest
 * job alert to a user immediately after fetching.
 */
@Component
public class EmailStack {

    private final Deque<EmailDto> stack = new ArrayDeque<>();

    /** Pushes a new email onto the top of the stack. */
    public void push(EmailDto email) {
        stack.push(email);
    }

    /** Pushes multiple emails at once, preserving "most recent ends up on top". */
    public void pushAll(List<EmailDto> emails) {
        // emails are oldest-to-newest from GmailFetchService, so push in
        // that order - the last one pushed (most recent) ends up on top.
        for (EmailDto email : emails) {
            stack.push(email);
        }
    }

    /** Returns the most recently added email without removing it. */
    public EmailDto peek() {
        return stack.peek();
    }

    /** Removes and returns the most recently added email. */
    public EmailDto pop() {
        return stack.pop();
    }

    /** Returns all emails, top (most recent) first, without modifying the stack. */
    public List<EmailDto> viewAll() {
        return stack.stream().toList();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }
}