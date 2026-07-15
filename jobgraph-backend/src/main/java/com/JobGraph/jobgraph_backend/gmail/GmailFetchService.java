package com.JobGraph.jobgraph_backend.gmail;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class GmailFetchService {

    private final GmailAuthService gmailAuthService;

    public GmailFetchService(GmailAuthService gmailAuthService) {
        this.gmailAuthService = gmailAuthService;
    }

    /**
     * Fetches recent emails matching a Gmail search query (same syntax as
     * the Gmail search bar, e.g. "subject:job OR subject:opening").
     * Returns them oldest-to-newest; the caller decides how to order them
     * further (e.g. into a stack).
     */
    public List<EmailDto> fetchEmails(String query, int maxResults) throws Exception {
        Gmail service = gmailAuthService.getGmailService();
        List<EmailDto> results = new ArrayList<>();

        var response = service.users().messages().list("me")
                .setQ(query)
                .setMaxResults((long) maxResults)
                .execute();

        var messages = response.getMessages();
        if (messages == null) return results;

        for (var msgRef : messages) {
            Message message = service.users().messages().get("me", msgRef.getId()).execute();
            results.add(toEmailDto(message));
        }

        // Guarantee oldest-to-newest order explicitly (rather than relying
        // on Gmail API's default ordering) - this matters because
        // EmailStack.pushAll() expects oldest-first so the newest ends up
        // on top after pushing.
        results.sort(java.util.Comparator.comparing(EmailDto::getReceivedAt));

        return results;
    }

    private EmailDto toEmailDto(Message message) {
        String subject = "";
        String from = "";

        MessagePart payload = message.getPayload();
        if (payload != null && payload.getHeaders() != null) {
            for (MessagePartHeader header : payload.getHeaders()) {
                if ("Subject".equalsIgnoreCase(header.getName())) subject = header.getValue();
                if ("From".equalsIgnoreCase(header.getName())) from = header.getValue();
            }
        }

        String body = extractBody(payload);
        LocalDateTime receivedAt = Instant.ofEpochMilli(message.getInternalDate())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return new EmailDto(message.getId(), subject, from, message.getSnippet(), body, receivedAt);
    }

    private String extractBody(MessagePart payload) {
        if (payload == null) return "";

        // Simple case: body directly on this part
        if (payload.getBody() != null && payload.getBody().getData() != null) {
            return decodeBase64(payload.getBody().getData());
        }

        // Multipart: look for a text/plain part
        if (payload.getParts() != null) {
            for (MessagePart part : payload.getParts()) {
                if ("text/plain".equalsIgnoreCase(part.getMimeType())
                        && part.getBody() != null && part.getBody().getData() != null) {
                    return decodeBase64(part.getBody().getData());
                }
            }
            // fallback: recurse into nested parts
            for (MessagePart part : payload.getParts()) {
                String nested = extractBody(part);
                if (!nested.isEmpty()) return nested;
            }
        }

        return "";
    }

    private String decodeBase64(String data) {
        byte[] decoded = Base64.getUrlDecoder().decode(data);
        return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
    }
}