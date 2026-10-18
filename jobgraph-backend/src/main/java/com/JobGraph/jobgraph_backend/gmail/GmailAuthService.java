package com.JobGraph.jobgraph_backend.gmail;

import com.JobGraph.jobgraph_backend.model.Account;
import com.JobGraph.jobgraph_backend.model.GmailConnection;
import com.JobGraph.jobgraph_backend.repository.GmailConnectionRepository;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GmailAuthService {
    public static class GmailAddressMismatchException extends IOException {
        public GmailAddressMismatchException() {
            super("The Google account did not match the Gmail address entered in JobGraph");
        }
    }

    private final GmailConnectionRepository connections;
    private final TokenCipher cipher;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GmailAuthService(GmailConnectionRepository connections, TokenCipher cipher,
                            @Value("${app.gmail.client-id:}") String clientId,
                            @Value("${app.gmail.client-secret:}") String clientSecret,
                            @Value("${app.gmail.redirect-uri}") String redirectUri) {
        this.connections = connections;
        this.cipher = cipher;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    private GoogleAuthorizationCodeFlow flow() throws IOException, GeneralSecurityException {
        // These credentials identify the JobGraph web app to Google.
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("Google web OAuth client is not configured");
        }
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(clientId);
        details.setClientSecret(clientSecret);
        GoogleClientSecrets secrets = new GoogleClientSecrets();
        secrets.setWeb(details);
        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(),
                secrets, List.of(GmailScopes.GMAIL_READONLY))
                .setAccessType("offline").build();
    }

    public String authorizationUrl(String state, String email) throws IOException, GeneralSecurityException {
        cipher.verifyConfigured();
        // Google may prefill this address, but the user still approves access there.
        return flow().newAuthorizationUrl().setRedirectUri(redirectUri).setState(state)
                .set("login_hint", email).set("prompt", "consent select_account").build();
    }

    @Transactional
    // Save only the address Google confirms, and require it to match the entered address.
    public String complete(Account account, String code, String requestedEmail) throws IOException, GeneralSecurityException {
        TokenResponse token = flow().newTokenRequest(code).setRedirectUri(redirectUri).execute();
        Gmail gmail = gmailWithTokens(token.getAccessToken(), token.getRefreshToken());
        String address = gmail.users().getProfile("me").execute().getEmailAddress();
        if (address == null || address.isBlank()) throw new IOException("Google did not return a Gmail address");
        if (!address.equalsIgnoreCase(requestedEmail)) throw new GmailAddressMismatchException();

        Optional<GmailConnection> previous = connections.findByAccount_Id(account.getId());
        String refreshToken = token.getRefreshToken();
        // Google may omit a new refresh token when reconnecting the same inbox.
        if ((refreshToken == null || refreshToken.isBlank()) && previous.isPresent()
                && previous.get().getGmailAddress().equalsIgnoreCase(address)) {
            refreshToken = cipher.decrypt(previous.get().getEncryptedRefreshToken());
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IOException("Google did not provide offline access. Disconnect in Google and retry consent.");
        }
        GmailConnection connection = previous.orElseGet(GmailConnection::new);
        connection.setAccount(account);
        connection.setGmailAddress(address);
        // Only the encrypted refresh token is stored; short-lived access tokens stay in memory.
        connection.setEncryptedRefreshToken(cipher.encrypt(refreshToken));
        connection.setUpdatedAt(LocalDateTime.now());
        connections.save(connection);
        return address;
    }

    private Gmail gmailWithTokens(String accessToken, String refreshToken) throws GeneralSecurityException, IOException {
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setClientSecrets(clientId, clientSecret).build();
        credential.setAccessToken(accessToken);
        if (refreshToken != null) credential.setRefreshToken(refreshToken);
        return new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(), credential).setApplicationName("JobGraph").build();
    }

    public Gmail getGmailService(Integer accountId) throws IOException, GeneralSecurityException {
        // Build the Gmail client from this account's saved connection.
        GmailConnection connection = connections.findByAccount_Id(accountId)
                .orElseThrow(() -> new IllegalStateException("Connect Gmail before fetching emails"));
        String refreshToken = cipher.decrypt(connection.getEncryptedRefreshToken());
        return gmailWithTokens(null, refreshToken);
    }

    public Optional<String> connectedAddress(Integer accountId) {
        return connections.findByAccount_Id(accountId).map(GmailConnection::getGmailAddress);
    }

    @Transactional
    // Deleting the token removes JobGraph's access to this inbox.
    public void disconnect(Integer accountId) { connections.deleteByAccount_Id(accountId); }
}
