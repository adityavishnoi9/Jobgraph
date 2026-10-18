package com.JobGraph.jobgraph_backend.gmail;

import org.springframework.stereotype.Component;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmailStack {
    // Each account has its own temporary inbox; entries disappear on restart.
    private final ConcurrentHashMap<Integer, Deque<EmailDto>> stacks = new ConcurrentHashMap<>();

    public void pushAll(Integer accountId, List<EmailDto> emails) {
        Deque<EmailDto> stack = stacks.computeIfAbsent(accountId, ignored -> new ArrayDeque<>());
        // ArrayDeque needs synchronization when requests for one account overlap.
        synchronized (stack) {
            for (EmailDto email : emails) {
                // Fetching again refreshes a message instead of showing duplicates.
                stack.removeIf(previous -> Objects.equals(previous.getMessageId(), email.getMessageId()));
                stack.push(email);
            }
        }
    }

    public List<EmailDto> viewAll(Integer accountId) {
        Deque<EmailDto> stack = stacks.get(accountId);
        if (stack == null) return List.of();
        synchronized (stack) { return List.copyOf(stack); }
    }

    public EmailDto peek(Integer accountId) {
        Deque<EmailDto> stack = stacks.get(accountId);
        if (stack == null) return null;
        synchronized (stack) { return stack.peek(); }
    }

    public int size(Integer accountId) {
        Deque<EmailDto> stack = stacks.get(accountId);
        if (stack == null) return 0;
        synchronized (stack) { return stack.size(); }
    }

    public void clear(Integer accountId) { stacks.remove(accountId); }
}
