package com.JobGraph.jobgraph_backend.gmail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

/**
 * Handles Google OAuth for Gmail access.
 *
 * First run: opens a browser window asking you to log in and approve
 * access (since you added yourself as a test user, this works even
 * though the app isn't published). After approving once, a token is
 * saved locally in the "tokens" folder - future runs reuse it silently,
 * no browser popup needed, until the token expires.
 */
@Component
public class GmailAuthService {

    // third application and uska naam
    private static final String APPLICATION_NAME = "JobGraph";
    // jo json ko java objects mein convert kregi
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    // tokens kaha pr store honge
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    // read-only scope - we only ever read emails, never send/delete
    // scope set krre hai jo hmara 3rd party hai ye kya kya kr skta hai
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);
    // jaha pr credentials hai us file ka path kya haiii
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    // ye apne ko vo service ya gmail ka object bna kr deri hai jisse hum aage kaam krenge
    public Gmail getGmailService() throws IOException, GeneralSecurityException {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        // credentials.json mein kya hai -> koi google ke pass jaega vo kaise maanega yhi hai
        // jobgraph toh credentials job graph ke identity card jaise hai
        Credential credential = authorize(httpTransport);

        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Credential authorize(com.google.api.client.http.HttpTransport httpTransport) throws IOException {
        var in = GmailAuthService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new IOException("credentials.json not found in resources folder. " +
                    "Download it from Google Cloud Console and place it at src/main/resources/credentials.json");
        }

        var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        var flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}