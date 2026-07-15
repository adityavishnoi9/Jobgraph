package com.JobGraph.jobgraph_backend.gmail;

import java.time.LocalDateTime;

public class EmailDto {
    private String messageId;
    private String subject;
    private String from;
    private String snippet;
    private String body;
    private LocalDateTime receivedAt;

    public EmailDto(String messageId, String subject, String from, String snippet, String body, LocalDateTime receivedAt) {
        this.messageId = messageId;
        this.subject = subject;
        this.from = from;
        this.snippet = snippet;
        this.body = body;
        this.receivedAt = receivedAt;
    }

    public String getMessageId() { return messageId; }
    public String getSubject() { return subject; }
    public String getFrom() { return from; }
    public String getSnippet() { return snippet; }
    public String getBody() { return body; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
}