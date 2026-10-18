package com.JobGraph.jobgraph_backend;

import com.JobGraph.jobgraph_backend.gmail.*;
import com.JobGraph.jobgraph_backend.model.Account;
import com.JobGraph.jobgraph_backend.service.AccountService;
import com.JobGraph.jobgraph_backend.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GmailConnectionTests {
    @Test
    void authorizationUrlUsesWebCallbackOfflineAccessAndAccountPicker() throws Exception {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        GmailAuthService gmail = new GmailAuthService(mock(com.JobGraph.jobgraph_backend.repository.GmailConnectionRepository.class),
                new TokenCipher(Base64.getEncoder().encodeToString(key)), "test-client", "test-secret",
                "http://localhost:8080/api/gmail/callback");
        String url = gmail.authorizationUrl("test-state", "other@gmail.com");
        assertTrue(url.startsWith("https://accounts.google.com/"));
        assertTrue(url.contains("state=test-state"));
        assertTrue(url.contains("access_type=offline"));
        assertTrue(url.contains("prompt=consent"));
        assertTrue(java.net.URI.create(url).getQuery().contains("login_hint=other@gmail.com"));
        assertTrue(url.contains("gmail.readonly"));
        assertTrue(java.net.URI.create(url).getQuery().contains("redirect_uri=http://localhost:8080/api/gmail/callback"));
    }

    @Test
    void oauthStateBindsConnectionToTheAccountAndDifferentGmailAddress() throws Exception {
        GmailAuthService gmail = mock(GmailAuthService.class);
        AccountService accounts = mock(AccountService.class);
        ProfileService profiles = mock(ProfileService.class);
        EmailStack stacks = new EmailStack();
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", 7);
        account.setEmail("login@example.com");
        var authentication = new UsernamePasswordAuthenticationToken("login@example.com", null, List.of());
        when(accounts.current(authentication)).thenReturn(account);
        when(profiles.exists(7)).thenReturn(true);
        when(gmail.authorizationUrl(anyString(), eq("other@gmail.com"))).thenReturn("https://accounts.google.com/test");
        when(gmail.connectedAddress(7)).thenReturn(Optional.of("other@gmail.com"));
        GmailConnectionController controller = new GmailConnectionController(gmail, accounts, profiles, stacks, "http://localhost:5173");
        MockHttpServletRequest request = new MockHttpServletRequest();

        var connect = new GmailConnectionController.ConnectRequest("other@gmail.com");
        assertEquals("https://accounts.google.com/test", controller.connect(connect, authentication, request).get("url"));
        String state = (String) request.getSession().getAttribute("gmailOAuthState");
        assertEquals("other@gmail.com", request.getSession().getAttribute("gmailOAuthEmail"));
        MockHttpServletResponse invalid = new MockHttpServletResponse();
        controller.callback("code", "wrong-state", null, authentication, request, invalid);
        assertEquals("http://localhost:5173/inbox?gmail=invalid-state", invalid.getRedirectedUrl());
        verify(gmail, never()).complete(any(), anyString(), anyString());

        controller.connect(connect, authentication, request);
        state = (String) request.getSession().getAttribute("gmailOAuthState");
        MockHttpServletResponse valid = new MockHttpServletResponse();
        controller.callback("code", state, null, authentication, request, valid);
        verify(gmail).complete(account, "code", "other@gmail.com");
        assertEquals("http://localhost:5173/inbox?gmail=connected", valid.getRedirectedUrl());
        assertEquals("other@gmail.com", controller.status(authentication).email());

        controller.connect(connect, authentication, request);
        state = (String) request.getSession().getAttribute("gmailOAuthState");
        MockHttpServletResponse cancelled = new MockHttpServletResponse();
        controller.callback(null, state, "access_denied", authentication, request, cancelled);
        assertEquals("http://localhost:5173/inbox?gmail=cancelled", cancelled.getRedirectedUrl());
        verify(gmail, times(1)).complete(account, "code", "other@gmail.com");

        controller.connect(connect, authentication, request);
        state = (String) request.getSession().getAttribute("gmailOAuthState");
        when(gmail.complete(account, "code", "other@gmail.com"))
                .thenThrow(new GmailAuthService.GmailAddressMismatchException());
        MockHttpServletResponse mismatch = new MockHttpServletResponse();
        controller.callback("code", state, null, authentication, request, mismatch);
        assertEquals("http://localhost:5173/inbox?gmail=email-mismatch", mismatch.getRedirectedUrl());
    }

    @Test
    void fetchedEmailStacksArePrivateAndTokensAreEncrypted() {
        EmailStack stacks = new EmailStack();
        EmailDto email = mock(EmailDto.class);
        stacks.pushAll(1, List.of(email));
        stacks.pushAll(1, List.of(email));
        assertEquals(1, stacks.size(1));
        assertEquals(0, stacks.size(2));
        stacks.clear(1);
        assertTrue(stacks.viewAll(1).isEmpty());

        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        TokenCipher cipher = new TokenCipher(Base64.getEncoder().encodeToString(key));
        String encrypted = cipher.encrypt("refresh-token-secret");
        assertNotEquals("refresh-token-secret", encrypted);
        assertEquals("refresh-token-secret", cipher.decrypt(encrypted));
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(encrypted.substring(1)));
    }
}
