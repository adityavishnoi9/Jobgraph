"""Build the Gmail learning guide from the current source, without reading secrets.

Run from any directory: python3 docs/gmail-guide/build_guide.py
Open the resulting HTML and print as A4 with background graphics enabled.
The checked-in PDF is a reading snapshot; rerun this script to update excerpts.
"""
from pathlib import Path
from html import escape
import hashlib
import json
import textwrap

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent
B = "jobgraph-backend/src/main/java/com/JobGraph/jobgraph_backend/"
F = "jobgraph-frontend/src/"
FILES = {
    "auth": B + "gmail/GmailAuthService.java",
    "connect": B + "gmail/GmailConnectionController.java",
    "cipher": B + "gmail/TokenCipher.java",
    "fetch": B + "gmail/GmailFetchService.java",
    "emails": B + "gmail/EmailController.java",
    "stack": B + "gmail/EmailStack.java",
    "dto": B + "gmail/EmailDto.java",
    "model": B + "model/GmailConnection.java",
    "repo": B + "repository/GmailConnectionRepository.java",
    "security": B + "config/SecurityConfig.java",
    "cors": B + "config/CorsConfig.java",
    "account": B + "service/AccountService.java",
    "login": B + "controller/AuthController.java",
    "api": F + "api/api.js",
    "button": F + "components/ConnectGmailButton.jsx",
    "dialog": F + "components/GmailConnectDialog.jsx",
    "inbox": F + "components/InboxPage.jsx",
    "render": F + "components/FetchedEmails.jsx",
    "migration": "jobgraph-backend/src/main/resources/db/migration/V2__accounts_and_gmail.sql",
    "tests": "jobgraph-backend/src/test/java/com/JobGraph/jobgraph_backend/GmailConnectionTests.java",
}
SNAPSHOT = {k: (ROOT / p).read_text() for k, p in FILES.items()}
REFS = {
    "G1": ("Google: OAuth for web server applications", "https://developers.google.com/identity/protocols/oauth2/web-server"),
    "G2": ("Google: Gmail permission scopes", "https://developers.google.com/workspace/gmail/api/auth/scopes"),
    "G3": ("Google: messages.list", "https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.messages/list"),
    "G4": ("Google: messages.get", "https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.messages/get"),
    "G5": ("Google: Message resource and MIME structure", "https://developers.google.com/workspace/gmail/api/reference/rest/v1/users.messages"),
    "G6": ("Google: OAuth and refresh-token expiration", "https://developers.google.com/identity/protocols/oauth2"),
    "S1": ("Spring Security: CSRF protection", "https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html"),
    "J1": ("Google Java OAuth client: Credential implementation", "https://github.com/googleapis/google-oauth-java-client/blob/v1.36.0/google-oauth-client/src/main/java/com/google/api/client/auth/oauth2/Credential.java"),
}


def ref(key):
    return f'<a class="ref" href="{REFS[key][1]}">[{key}]</a>'


def p(s):
    return f"<p>{s}</p>"


def h(s):
    return f"<h3>{s}</h3>"


def note(title, body, kind="note"):
    return f'<aside class="callout {kind}"><strong>{title}</strong><p>{body}</p></aside>'


def bullets(*items):
    return "<ul>" + "".join(f"<li>{i}</li>" for i in items) + "</ul>"


def steps(*items):
    return "<ol>" + "".join(f"<li>{i}</li>" for i in items) + "</ol>"


def table(headers, rows, widths=None):
    cols = "" if not widths else "<colgroup>" + "".join(f'<col style="width:{x}%">' for x in widths) + "</colgroup>"
    return '<table>' + cols + '<thead><tr>' + ''.join(f'<th>{x}</th>' for x in headers) + '</tr></thead><tbody>' + ''.join('<tr>' + ''.join(f'<td>{x}</td>' for x in row) + '</tr>' for row in rows) + '</tbody></table>'


def code(key, first, last):
    lines = SNAPSHOT[key].splitlines()[first - 1:last]
    source = textwrap.dedent("\n".join(lines)).splitlines()
    rows = ''.join(f'<div class="code-line"><span class="ln">{first+i}</span><span>{escape(t) or " "}</span></div>' for i, t in enumerate(source))
    return f'<figure class="code"><figcaption>{Path(FILES[key]).name} <span>lines {first}–{last} · exact excerpt</span></figcaption><div class="code-body">{rows}</div></figure>'


def example(label, s):
    return f'<figure class="code"><figcaption>{label}<span>illustrative example</span></figcaption><pre>{escape(textwrap.dedent(s).strip())}</pre></figure>'


def svg_start(height):
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 650 {height}" role="img"><defs><marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto-start-reverse"><path d="M0,0 L8,4 L0,8 z" fill="#2f7769"/></marker></defs>'


def box(x, y, w, ht, title, sub="", color="#e8f2ec"):
    s = f'<rect x="{x}" y="{y}" width="{w}" height="{ht}" rx="12" fill="{color}" stroke="#b8d3c7"/>'
    s += f'<text x="{x+w/2}" y="{y+26}" text-anchor="middle" font-size="14" font-weight="700" fill="#183f39">{escape(title)}</text>'
    for i, line in enumerate(sub.split('|') if sub else []):
        s += f'<text x="{x+w/2}" y="{y+47+i*16}" text-anchor="middle" font-size="11.5" fill="#52695f">{escape(line)}</text>'
    return s


def arrow(x1, y1, x2, y2, label="", offset=-8):
    s = f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="#2f7769" stroke-width="1.7" marker-end="url(#arrow)"/>'
    if label:
        s += f'<text x="{(x1+x2)/2}" y="{(y1+y2)/2+offset}" text-anchor="middle" font-size="11.5" fill="#31594e">{escape(label)}</text>'
    return s


def architecture():
    s = svg_start(350)
    s += box(10, 20, 180, 80, "React browser", "Dialog + Inbox page|Session cookie + CSRF")
    s += box(230, 20, 180, 80, "Spring Boot", "Account + Gmail services|Owner checks + encryption")
    s += box(450, 20, 190, 80, "Google", "OAuth + Gmail API|Consent + messages")
    s += arrow(191, 58, 225, 58) + arrow(410, 58, 446, 58)
    s += box(130, 207, 185, 89, "PostgreSQL", "Account → connection|Encrypted refresh token|Survives app restart")
    s += box(350, 207, 185, 89, "Java memory", "Account → email deque|Temporary message bodies|Cleared on restart", "#fff0e5")
    s += arrow(290, 103, 220, 202) + arrow(349, 103, 440, 202)
    s += '<text x="325" y="335" text-anchor="middle" font-size="12" fill="#52695f">The AI service is not called by the current Fetch job alerts path.</text></svg>'
    return s


def sequence():
    s = svg_start(495)
    xs = [60, 225, 440, 605]
    for x, title in zip(xs, ["Browser", "Backend", "Google", "Database"]):
        s += f'<rect x="{x-43}" y="4" width="86" height="34" rx="8" fill="#183f39"/><text x="{x}" y="26" text-anchor="middle" font-size="12" fill="white">{title}</text><line x1="{x}" y1="40" x2="{x}" y2="480" stroke="#b3c9c0" stroke-dasharray="4 4"/>'
    for y, a, b, label in [
        (76, 0, 1, "1. POST /connect"), (126, 1, 0, "2. URL + saved state"),
        (176, 0, 2, "3. Navigate; sign in + consent"), (226, 2, 0, "4. Redirect: code + state"),
        (276, 0, 1, "5. GET /callback"), (326, 1, 2, "6. Code → tokens; confirm address"),
        (376, 1, 3, "7. Save encrypted refresh token"), (426, 1, 0, "8. Redirect /inbox"),
        (472, 0, 1, "9. GET /status")]:
        s += arrow(xs[a], y, xs[b], y, label)
    return s + '</svg>'


def cipher_diagram():
    s = svg_start(277)
    s += box(5, 10, 180, 75, "Refresh token", "Plain text in memory")
    s += box(235, 10, 180, 75, "AES-256-GCM", "Key + fresh 12-byte IV")
    s += box(465, 10, 180, 75, "Encrypted output", "Ciphertext + 16-byte tag")
    s += arrow(186, 47, 230, 47) + arrow(416, 47, 460, 47)
    s += '<text x="325" y="131" text-anchor="middle" font-size="13" font-weight="700" fill="#183f39">Database value after Base64 encoding</text>'
    for x, w, txt, col in [(20,130,"IV · 12 bytes","#e8f2ec"),(150,340,"Encrypted token · variable length","#d3e7dd"),(490,140,"GCM tag · 16 bytes","#fff0e5")]:
        s += f'<rect x="{x}" y="151" width="{w}" height="51" fill="{col}" stroke="#b8d3c7"/><text x="{x+w/2}" y="181" text-anchor="middle" font-size="11.5" fill="#183f39">{txt}</text>'
    s += '<text x="325" y="236" text-anchor="middle" font-size="12" fill="#52695f">The encryption key stays in backend configuration, outside this row.</text></svg>'
    return s


def refresh_diagram():
    s = svg_start(325)
    nodes = [(5,15,"Saved connection","Read encrypted refresh token"),(335,15,"TokenCipher","Decrypt with backend AES key"),(335,135,"Google token endpoint","Refresh token → access token"),(5,135,"GoogleCredential","Attach Authorization: Bearer"),(170,250,"Gmail API","List IDs, then read messages")]
    for x,y,title,sub in nodes:
        s += box(x,y,310,65,title,sub)
    s += arrow(317,48,330,48) + arrow(490,82,490,128) + arrow(331,168,319,168) + arrow(160,204,260,245)
    return s + '</svg>'


def mime_diagram():
    s = svg_start(218)
    s += box(205,5,240,60,"multipart/mixed","Container, often no body.data")
    s += box(35,112,245,80,"multipart/alternative","text/plain or text/html|Find a readable body")
    s += box(370,112,245,80,"Attachment part","May have attachmentId|No attachment download here", "#fff0e5")
    s += arrow(270,68,158,107) + arrow(380,68,490,107)
    return s + '</svg>'


PAGES = []


def page(title, sub, body, section="UNDERSTANDING GMAIL", sources=""):
    PAGES.append(dict(title=title, sub=sub, body=body, section=section, sources=sources))


page("Gmail, from click<br>to inbox.", "A guided explanation of JobGraph’s connection, message fetching, and security.",
     '<div class="cover-art">' + architecture() + '</div>' +
     '<div class="cover-tags"><span>OAuth flow</span><span>Real code</span><span>Security</span><span>Debugging</span></div>' +
     p("Prepared for your current JobGraph codebase • 03 October 2026") +
     note("How to use this guide", "Read the diagrams first, then follow the numbered source excerpts in your IDE. Examples use fictional addresses and placeholders. No real keys, passwords, tokens, or mailbox contents are included."),
     section="JOBGRAPH / DEVELOPER GUIDE")

page("Your reading route", "Follow the user’s journey first. Return to the reference pages while debugging.",
     table(["Pages", "What you will understand"], [
         ('<a href="#page-3">3–4</a>', "The architecture, account ownership, and the names of every credential."),
         ('<a href="#page-5">5–10</a>', "The connection dialog, sessions, OAuth state, Google consent, and callback validation."),
         ('<a href="#page-11">11–14</a>', "Exchanging the code, confirming the Gmail address, saving tokens, encryption, and automatic refresh."),
         ('<a href="#page-15">15–20</a>', "Connection lifecycle, Gmail search, message bodies, temporary storage, and the Inbox UI."),
         ('<a href="#page-21">21–25</a>', "Security boundaries, endpoint reference, local setup, and diagnosing errors."),
         ('<a href="#page-26">26–30</a>', "Current limitations, a debugger walkthrough, a worked example, tests, and source references.")], [16,84]) +
     h("The feature that exists today") +
     p("A signed-in user saves a profile, connects one Gmail account through Google, and manually fetches messages matching a job-alert search. The Inbox page displays those fetched messages. There is no automatic profile relevance ranking in this request path.") +
     h("Three actions to keep separate") +
     steps("<b>Connect:</b> obtain permission and save a reusable credential. This does not fetch the job-alert list.", "<b>Fetch:</b> use that credential to search Gmail and load message details into Java memory.", "<b>View:</b> read the already-fetched messages from JobGraph’s API and render them in React.") +
     note("About the excerpts", "Code blocks with line numbers are copied from the working tree used to build this guide. Blocks marked ‘illustrative example’ explain a concept and are not additional application code. The source manifest records the files and their hashes."))

page("Who does what?", "The browser starts the flow; the backend owns the Google credentials and mailbox requests.",
     architecture() +
     table(["Component", "Responsibility"], [
         ("React + Vite", "Collect the intended Gmail address, send authenticated API requests, redirect the browser to Google, and display messages."),
         ("Spring Security", "Restore the user’s session, check CSRF tokens on writes, and enforce protected API routes."),
         ("GmailConnectionController", "Bind the OAuth attempt to a session/account; validate the callback; manage connection status and disconnect."),
         ("GmailAuthService + TokenCipher", "Create Google clients, exchange credentials, verify the mailbox address, and encrypt/decrypt the saved refresh token."),
         ("GmailFetchService + EmailStack", "Search and read Gmail messages; keep separate temporary collections for each account.")], [37,63]) +
     note("Where the AI service fits", "The Python service has a /filter-emails endpoint, but EmailController and the current frontend fetch flow do not call it. The ranking feature is therefore separate from the flow documented here."), sources="Repo: Gmail services, frontend API, main.py and graph.py")

page("The names that matter", "Each credential has a specific owner, purpose, and place to live.",
     table(["Name", "Meaning and current storage"], [
         ("JobGraph login email", "Identifies the local account. Its password is BCrypt-hashed in the accounts table."),
         ("Connected Gmail address", "The mailbox Google confirms. May differ from both the login email and the resume’s contact email."),
         ("Session cookie", "An identifier that lets the backend recover the JobGraph login session. It is not a Gmail access token."),
         ("Spring CSRF token", "A session-backed token the frontend sends with writes such as POST /gmail/connect."),
         ("OAuth state", "Random value saved before leaving for Google. The callback must return the same value in the same session."),
         ("Authorization code", "A temporary value returned through the browser. The backend exchanges it for Google tokens."),
         ("Access token", "Credential used on Gmail API requests. The Java client holds it in memory."),
         ("Refresh token", "Credential used to obtain a new access token. JobGraph saves it encrypted in PostgreSQL."),
         ("OAuth client ID / secret", "Identify the JobGraph web application to Google. The secret is backend configuration."),
         ("Token encryption key", "JobGraph’s own 32-byte AES key. It protects stored refresh tokens and is independent of the Google client secret.")], [32,68]) +
     note("Account ownership", "A profile is required before connecting, but the Gmail connection belongs to the Account entity. Editing the resume contact email does not change the Gmail connection or its owner."), sources="Repo: AccountService, GmailConnection, GmailAuthService, TokenCipher")

page("1. Start in the browser", "Connect Gmail opens a dialog. Continue with Google starts the server flow.",
     p("GmailConnectDialog uses a native dialog and focuses the email input. The form trims the address and calls onContinue. HTML validates email syntax; the backend also validates it. These checks do not establish ownership of the mailbox.") +
     code("button",36,42) +
     p("startGmailConnection sends JSON to <code>POST /api/gmail/connect</code>. The response contains an authorization URL. <code>window.location.assign(url)</code> navigates the browser to Google’s website; it is not an AJAX request to Google’s login screen.") +
     example("Request and response shape", '''POST /api/gmail/connect
Content-Type: application/json
X-CSRF-TOKEN: <session CSRF token>
Cookie: JSESSIONID=<local session id>

{"email":"career.mailbox@gmail.com"}

200 OK
{"url":"https://accounts.google.com/o/oauth2/auth?..."}''') +
     h("Why an email field is not enough") +
     p("Typing an address only tells JobGraph which mailbox the user intends to connect. Google handles sign-in, any verification challenge, and the permission screen. JobGraph does not send its own Gmail verification code and never asks the user to enter a Google password into this dialog.") +
     note("After returning from Google", "The URL result such as ?gmail=connected controls a UI notice. The component also calls GET /api/gmail/status for the saved connection. A success-looking URL alone is not proof that a connection exists."), sources="Repo: GmailConnectDialog:19–32; ConnectGmailButton:5–42; api.js:85")

page("2. Keep the user’s identity", "Session authentication protects local API calls; CSRF protects state-changing requests.",
     code("api",29,38) +
     p("Before a write, csrfToken() calls GET /api/auth/csrf if needed. It receives a token and the header name expected by Spring. The browser sends the session cookie with same-origin requests. Vite proxies /api to localhost:8080, so React normally uses relative URLs.") +
     code("login",90,98) +
     p("Login verifies credentials, rotates an existing session ID, puts the authenticated user in a SecurityContext, and explicitly saves it. It clears the previous CSRF token; the frontend clears its cached token too. A subsequent write obtains a fresh token.") +
     table(["Check", "Question it answers"], [
         ("Session authentication", "Which JobGraph account is making this request?"),
         ("Spring CSRF token", "Does this write include the token expected for this session?"),
         ("OAuth state, later", "Does Google’s callback match the connection attempt started in this session?")], [36,64]) +
     p("GET requests do not need Spring’s CSRF header here. The OAuth callback is a GET, so it uses the separate state validation shown on page 10. Public signup/login POST routes still require CSRF." + ref("S1")), sources="Repo: api.js:19–38; AuthController:88–100; SecurityConfig:29–44")

page("3. Create the OAuth attempt", "The backend chooses the account from the authenticated session.",
     code("connect",59,71) +
     steps("<b>Find the owner:</b> accounts.current(authentication) resolves the logged-in account. The browser does not supply an accountId.", "<b>Require a saved profile:</b> the controller returns 409 if the profile does not exist.", "<b>Create state:</b> SecureRandom fills 32 bytes. URL-safe Base64 makes those bytes suitable for a redirect URL. Base64 is an encoding step, not encryption.", "<b>Build Google’s URL:</b> authorizationUrl() also checks that the encryption key is configured before redirecting the user.", "<b>Save three session values:</b> state, account ID, and requested email. These tie the future callback to this exact attempt.") +
     note("Why these three values?", "State connects the outbound and return trips. Account ID checks that the callback is still for the same JobGraph user. Requested email lets the backend compare Google’s confirmed mailbox with the address the user entered.") +
     p("The values are one set of session attributes, so starting another connection in a second tab overwrites the first attempt. There is no separate OAuth-attempt timestamp or short expiry in this code; the attempt depends on session lifetime and removal during callback."), sources="Repo: GmailConnectionController:55–74; AccountService:15–20")

page("4. Build Google’s consent URL", "The Java client builds the URL from the app credentials and requested permission.",
     code("auth",58,68) +
     table(["Parameter / setting", "Why the code uses it"], [
         ("client_id", "Identifies the configured Google web OAuth client."),
         ("redirect_uri", "The backend callback address registered for that client."),
         ("response_type=code", "The authorization-code flow returns a code to the callback."),
         ("gmail.readonly", "Requests permission to read Gmail. This scope does not grant sending, deletion, or marking messages read."),
         ("access_type=offline", "Requests the ability to obtain fresh access tokens later through a refresh token."),
         ("state", "Carries the random value created by the backend."),
         ("login_hint", "Suggests the typed address to Google; it is not a mailbox ownership check."),
         ("prompt=consent select_account", "Requests consent and an account picker.")], [38,62]) +
     p("The installed google-api-client 2.7.0 builds an authorization URL beginning with <code>https://accounts.google.com/o/oauth2/auth</code>. The token exchange uses <code>https://oauth2.googleapis.com/token</code>. These endpoints were checked in the installed library’s GoogleOAuthConstants source.") +
     note("Read access is broader than the search", "gmail.readonly permits reading mailbox data. The job-alert query controls what this feature fetches; it does not limit the underlying OAuth grant to job emails."), sources="Repo: GmailAuthService:48–68. Protocol background " + ref("G1") + " " + ref("G2"))

page("The complete connection trip", "Follow the arrows once before reading the callback code.",
     sequence() +
     p("Google returns the browser to the backend callback with a code and state. The browser’s JobGraph cookie allows the backend to recover the session that started the flow. The backend then talks directly to Google to exchange the code and identify the Gmail mailbox.") +
     note("What the browser receives", "It receives the authorization URL, the callback code/state while navigating, a redirect to /inbox, and connection status. JobGraph’s JSON APIs do not return the Google access token, refresh token, client secret, or encryption key."), sources="Repo: GmailConnectionController:55–118; GmailAuthService:64–97")

page("5. Validate the callback", "The callback can reach the controller anonymously, but attaching Gmail still needs the original login session.",
     p("SecurityConfig permits GET /api/gmail/callback so the controller can handle success, cancellation, and expired sessions. The controller immediately checks for a session and authentication; without them it redirects to /login?gmail=session-expired.") +
     code("connect",89,103) +
     p("The callback reads all three saved values and removes them before proceeding. It compares the saved and returned state using MessageDigest.isEqual, checks the account ID, and requires the requested email. A mismatch returns /inbox?gmail=invalid-state without exchanging the code.") +
     h("Then choose the outcome") +
     bullets("If Google returned an error, or no code, redirect with <code>gmail=cancelled</code>.", "If checks pass, call <code>gmail.complete(account, code, requestedEmail)</code>.", "After completion, clear that account’s old email stack and redirect with <code>gmail=connected</code>.", "If the confirmed email differs, use <code>gmail=email-mismatch</code>. Other completion failures use <code>gmail=error</code>.") +
     note("An attempt is consumed on return", "Even a bad-state callback removes the saved attempt. Retrying the same callback URL will not complete that attempt. Begin again with Connect Gmail."), sources="Repo: GmailConnectionController:77–118")

page("6. Exchange the code", "The backend obtains tokens, then asks Gmail which mailbox those tokens belong to.",
     code("auth",73,88) +
     steps("<b>Exchange:</b> newTokenRequest(code).execute() contacts Google’s token endpoint using the configured client and callback URI.", "<b>Create a client:</b> gmailWithTokens receives the returned access and refresh tokens.", "<b>Confirm identity:</b> users().getProfile(\"me\") reads the mailbox’s email address. Here ‘me’ means the user authorized by the Google credential.", "<b>Compare:</b> the confirmed address must equal the typed address, ignoring case. A login hint did not enforce this earlier.", "<b>Ensure durable access:</b> keep the new refresh token, or reuse the existing decrypted token only when reconnecting the same Gmail address.") +
     note("Why reuse only for the same address?", "A refresh token belongs to a particular authorization. Reusing mailbox A’s token while displaying mailbox B’s address would attach the wrong mailbox. A different mailbox without a new refresh token causes completion to fail.") +
     p("The address comparison does not normalize Gmail dots, plus aliases, or every possible Workspace alias. A typed alias that differs from Google’s confirmed address can produce an email-mismatch result. Use the address Google confirms."), sources="Repo: GmailAuthService:71–97")

page("7. Save the connection", "One database row connects a JobGraph account to one Gmail mailbox.",
     code("auth",90,97) +
     code("model",12,14) +
     p("The service updates the existing row or creates a new GmailConnection. The database requires a unique, non-null account_id. That means an account has at most one saved Gmail connection; the saved profile and Gmail row both point to the account.") +
     table(["Database column", "Stored value"], [
         ("account_id", "The JobGraph account owner; it is the lookup key used by the services."),
         ("gmail_address", "The address returned by Google’s Gmail profile API."),
         ("encrypted_refresh_token", "Base64 text containing the IV, encrypted refresh token, and authentication tag."),
         ("updated_at", "When JobGraph last saved/replaced the connection; this is not a token-expiry time.")], [39,61]) +
     p("GmailConnectionRepository extends JpaRepository, so save() persists the entity. <code>findByAccount_Id(accountId)</code> means lookup by the nested <code>account.id</code> property and returns Optional&lt;GmailConnection&gt;. The underscore makes the property traversal explicit.") +
     note("A detail in the schema", "gmail_address itself is not unique. The schema allows the same Gmail address to be connected to separate JobGraph accounts after each completes consent. Ownership checks still use account_id."), sources="Repo: GmailAuthService:80–97; GmailConnectionRepository; V2 migration:25–31")

page("8. Protect the saved token", "TokenCipher uses AES-256-GCM so the backend can recover the refresh token when needed.",
     cipher_diagram() +
     code("cipher",34,45) +
     p("The configured key is Base64-decoded and must be exactly 32 bytes: a 256-bit AES key. Every encryption generates a fresh 12-byte IV. GCMParameterSpec(128, iv) requests a 128-bit authentication tag. Java’s doFinal() produces encrypted bytes with the tag; the code prepends the IV and encodes the combined bytes as Base64.") +
     p("During decryption, the first 12 bytes become the IV and the remaining bytes go through AES/GCM with the same key. A wrong key or changed encrypted value should fail authentication instead of producing a trusted token.") +
     note("Encryption and hashing serve different needs", "JobGraph must recover the refresh token to send it to Google, so it encrypts it. A login password is only checked for a match, so it is stored as a BCrypt hash. Base64 alone provides neither protection."), sources="Repo: TokenCipher:21–59; SecurityConfig:48–50")

page("9. Obtain access when fetching", "The saved refresh token lets the Google client obtain a usable access token.",
     code("auth",111,116) +
     refresh_diagram() +
     p("gmailWithTokens(null, refreshToken) constructs GoogleCredential with the app’s client ID/secret and the decrypted refresh token. It intentionally starts with no access token. On the first outgoing request, the library’s Credential interceptor sees the missing access token and refreshes it, then adds the Bearer authorization header.") +
     p("This behavior was checked in the installed google-oauth-client 1.36.0 source, Credential.intercept(), lines 195–207. The library can also refresh around expiry or authentication failure; the application does not implement its own refresh HTTP request." + ref("J1")) +
     note("Current caching behavior", "getGmailService() creates a new client on each fetch call. The access token is reused within that client’s list/get operations, but there is no shared per-account access-token cache across fetch calls. The refresh token remains encrypted in the database."), sources="Repo: GmailAuthService:100–116; installed Google OAuth client source")

page("Status, replacement, disconnect", "A saved connection, an active login, and a valid Google grant are separate states.",
     code("auth",119,121) +
     p("connectedAddress() checks the local database and maps the connection to its address. It does not decrypt a token or contact Google. Therefore ‘Connected’ means a row exists; it does not prove the Google grant is still valid.") +
     table(["Action", "What happens in the current code"], [
         ("Check status", "GET /gmail/status returns connected=true plus the stored address, or false and null."),
         ("Change Gmail account", "Run the same consent flow. The row is updated after address/token checks succeed; old fetched messages are then cleared."),
         ("Cancel replacement", "No new connection is saved. The old saved connection remains."),
         ("Disconnect", "DELETE /gmail/connection deletes the account’s local connection row and clears its in-memory email stack."),
         ("Log out", "Invalidate the JobGraph session. The Gmail connection row remains for the next login."),
         ("Restart backend", "Saved connection rows remain in PostgreSQL. EmailStack’s in-memory messages disappear.")], [29,71]) +
     code("connect",124,128) +
     note("Local disconnect is not Google revocation", "There is no call to Google’s revocation endpoint. Disconnect removes JobGraph’s saved token; the authorization grant may still appear in the user’s Google Account settings. An already-running request may also finish after disconnect."), sources="Repo: GmailAuthService:119–125; GmailConnectionController:109–128; AuthController:117–121")

page("10. Click Fetch job alerts", "The backend searches the signed-in user’s connected Gmail account.",
     code("emails",34,46) +
     p("The frontend uses POST /api/emails/fetch with its session and CSRF header. AccountService derives accountId from authentication. The controller first checks that a local Gmail connection exists, then calls GmailFetchService. Results are pushed into the account’s stack, and the response reports counts.") +
     example("Default query, line breaks added for readability", '''newer_than:10d (
  subject:(job OR opening OR hiring OR position
           OR vacancy OR recruitment OR interview)
  OR from:(glassdoor.com OR indeed.com
           OR linkedin.com OR naukri.com)
)''') +
     p("The default search asks for recent messages with job-related subject words or matching senders. maxResults defaults to 10, and the controller clamps it to 1–50. The current button sends no custom query or limit. These are application choices, not Gmail’s maximum capabilities.") +
     note("Fetching does not guarantee a relevant job", "These search terms can include marketing, interview notifications, or unrelated messages. A From header is not proof of sender authenticity. This path does not perform profile matching, fraud checks, or AI scoring."), sources="Repo: EmailController:30–46; api.js:83. Query API reference " + ref("G3"))

page("11. Search IDs, then read details", "The Gmail API list call returns references; each get call supplies a message.",
     code("fetch",31,46) +
     table(["Java call", "Conceptual REST request"], [
         ("messages().list(\"me\")", "GET https://gmail.googleapis.com/gmail/v1/users/me/messages?q=…&amp;maxResults=…"),
         ("messages().get(\"me\", id)", "GET https://gmail.googleapis.com/gmail/v1/users/me/messages/{id}"),
         ("users().getProfile(\"me\")", "GET https://gmail.googleapis.com/gmail/v1/users/me/profile (used during connection)")], [41,59]) +
     p("The list response provides IDs and thread IDs, so the loop requests each message separately. ‘me’ always refers to the Google user behind that client’s credential. Ten returned messages require one list call and ten detail calls, plus a token refresh when needed." + ref("G3") + " " + ref("G4")) +
     h("What the loop does and does not do") +
     bullets("If getMessages() returns null, return an empty list.", "Read detail requests sequentially, then convert each result to EmailDto.", "Sort the fetched batch oldest to newest before passing it to the stack.", "There is no nextPageToken loop, so this fetch reads one result page.", "There is no partial-success handling: if one message fails, the method throws and the controller does not push this batch.") +
     p("The code does not send a format parameter to messages.get. The API’s parsed message response supplies payload and headers used by the converter. The code never requests Gmail mutations such as marking a message read."), sources="Repo: GmailFetchService:31–55")

page("12. Turn MIME into readable text", "An email can contain nested parts, alternative formats, and attachments.",
     mime_diagram() +
     code("fetch",78,98) +
     p("extractBody first decodes data on the current part. Otherwise it looks for a direct text/plain child. If it finds none, it recursively checks children and returns the first non-empty body. The data is URL-safe Base64 and is decoded as UTF-8.") +
     note("Practical limits of this parser", "It does not download attachmentId bodies, combine every part, or inspect character-set declarations. The fallback can return HTML text or another part’s content. It prefers plain text among immediate children, but it is not a complete MIME normalization engine."), sources="Repo: GmailFetchService:78–107. Message structure " + ref("G5"))

page("13. Keep each account’s messages", "EmailDto is the API shape; EmailStack is temporary server memory.",
     code("fetch",70,75) +
     p("The converter reads Subject and From headers, takes Gmail’s snippet and ID, extracts the body, and converts internalDate from milliseconds to LocalDateTime using the backend’s time zone. Jackson serializes the DTO getters into the JSON the frontend receives.") +
     code("stack",12,24) +
     p("ConcurrentHashMap separates collections by account ID. computeIfAbsent creates the deque only when needed. ArrayDeque itself is not thread-safe, so operations on an account’s deque are synchronized. Before pushing a message, the code removes any existing item with the same Gmail message ID.") +
     example("Order in a single fresh batch", '''Fetched and sorted: [older A, newer B]
push(A) -> [A]
push(B) -> [B, A]    // newest is now at the front''') +
     note("Two limits to understand", "Only the incoming batch is sorted. A later custom search that fetches old messages can move those older messages above newer cached ones. Also, the cache has no size cap, expiry, or database persistence."), sources="Repo: EmailDto; GmailFetchService:49–75; EmailStack:12–45")

page("14. Show the messages in React", "Fetching and displaying use two API requests.",
     code("button",55,57) +
     p("The POST fetch response is a count object, not the message list. After it succeeds, onFetched() calls the handler supplied by InboxPage. That handler requests GET /api/emails/stack, puts the returned array into React state, and scrolls toward the message list.") +
     code("inbox",37,46) +
     h("How the Inbox page behaves") +
     bullets("On opening, load the saved profile and the existing stack. A user without a saved profile is redirected to the workspace.", "While loading, display a loading message; on failure, display the API error.", "For each email, show subject, sender, received time, snippet, and an expandable body.", "Disconnect clears the local React array after the backend removes the connection and stack.") +
     example("Rendering pattern used by FetchedEmails", '''<div className="fetched-email__body">{email.body}</div>''') +
     note("Email text stays text", "The body is inserted as a React text value. The component does not use dangerouslySetInnerHTML. HTML-looking email text is therefore escaped rather than executed or rendered as remote HTML. This also means some HTML-only messages may look unformatted."), sources="Repo: ConnectGmailButton:52–61; InboxPage:26–59; FetchedEmails:16–21")

page("What protects which boundary?", "Security is several checks working together, each at a different point in the request.",
     table(["Boundary / risk", "Protection in this code"], [
         ("Anonymous access to inbox APIs", "SecurityConfig requires authentication for /api/** except its explicit public routes."),
         ("Forged browser write requests", "Session-backed Spring CSRF token on connect, fetch, disconnect, login, signup, and logout."),
         ("Callback attached to the wrong attempt", "Random OAuth state, saved account ID, requested email, and session checks."),
         ("Typed address falsely claimed as owned", "Read Google’s /users/me/profile result and compare the returned address."),
         ("Access to another JobGraph account’s data", "Derive the account from Authentication; repository and stack calls use that account ID."),
         ("Database-only theft of a refresh token", "AES-GCM encryption with the key outside the database row."),
         ("Token exposure through frontend JSON", "Return status/counts/messages, not the connection entity or token values."),
         ("Modifying Gmail accidentally", "Request gmail.readonly and call read methods only."),
         ("Email content executing as page HTML", "Render message body through ordinary React text interpolation.")], [42,58]) +
     h("Cookie and origin settings") +
     p("The current properties set HttpOnly=true, SameSite=Lax, and a 12-hour session timeout. HttpOnly blocks normal JavaScript reads of the cookie. SameSite=Lax is compatible with the top-level GET return from Google. The local configuration does not explicitly enable a Secure cookie; a deployed HTTPS setup should configure that.") +
     p("CORS accepts the configured frontend origin plus the listed localhost/127.0.0.1 origins and permits credentials. CORS controls browser cross-origin access; it does not replace authentication or CSRF. The normal Vite proxy avoids cross-origin frontend API calls.") +
     note("Encryption’s practical boundary", "The running backend can decrypt the refresh token. If both the database and encryption key are compromised, the token can be recovered. The current cipher does not bind the encrypted value to account_id with additional authenticated data."), sources="Repo: SecurityConfig, CorsConfig, AccountService, TokenCipher, application.properties")

page("The endpoints at a glance", "All paths below are backend paths. The frontend prepends /api through its shared API helper.",
     table(["Method + path", "Input / result", "Protection"], [
         ("GET /api/auth/csrf", "Returns token and headerName; prepares a session token.", "Public route."),
         ("GET /api/gmail/status", "Returns {connected, email}. Reads the local row.", "Session login."),
         ("POST /api/gmail/connect", "JSON {email}; returns {url} for Google navigation.", "Login + CSRF + saved profile."),
         ("GET /api/gmail/callback", "Google query parameters code/state or error; sends a browser redirect.", "Public filter route; controller checks session, state, owner, email."),
         ("DELETE /api/gmail/connection", "Deletes local connection and cached emails; returns 204.", "Login + CSRF."),
         ("POST /api/emails/fetch", "Optional query and maxResults URL parameters; returns fetched and stackSize.", "Login + CSRF + saved connection."),
         ("GET /api/emails/stack", "Returns the current account’s cached EmailDto array.", "Session login."),
         ("GET /api/emails/stack/latest", "Returns the first cached item, or 204 if empty.", "Session login.")], [33,43,24]) +
     example("Example responses", '''GET /api/gmail/status
{"connected":true,"email":"career.mailbox@gmail.com"}

POST /api/emails/fetch
{"fetched":2,"stackSize":2}

GET /api/emails/stack
[{"messageId":"example-id","subject":"Engineer opening",
  "from":"Hiring <jobs@example.com>","snippet":"A new role...",
  "body":"Full message text...","receivedAt":"2026-10-03T10:30:00"}]''') +
     p("fetched is the number returned by this fetch operation. Repeated fetches can return the same IDs, so this count is not necessarily the number of newly discovered messages. stackSize is the resulting cached collection size."), sources="Repo: GmailConnectionController, EmailController, AuthController, EmailDto")

page("Configuration you can trace", "These are placeholder names and local URLs; the guide contains no real credential values.",
     table(["Environment variable", "Purpose"], [
         ("GOOGLE_CLIENT_ID", "The web application OAuth client ID."),
         ("GOOGLE_CLIENT_SECRET", "Its matching client secret, used only by the backend."),
         ("GMAIL_TOKEN_ENCRYPTION_KEY", "Base64-encoded 32 random bytes; must remain stable to decrypt existing tokens."),
         ("GOOGLE_REDIRECT_URI", "Defaults to http://localhost:8080/api/gmail/callback."),
         ("APP_FRONTEND_URL", "Defaults to http://localhost:5173; used when redirecting back to the UI.")], [47,53]) +
     p("application.properties imports the ignored jobgraph-ai-service/.env as a properties file using two optional paths, supporting startup from the repo root or backend folder. Spring maps the variables above into app.gmail.* and injects them through @Value. Editing .env requires restarting the backend to reload these values.") +
     h("Local Google configuration") +
     steps("Enable Gmail API in the Google Cloud project used by the OAuth client.", "Use a Web application OAuth client with the callback above registered exactly, including scheme, port, and path.", "Configure the consent screen and add the Gmail account as a test user if the app is in Testing.", "Start the frontend at localhost:5173 and backend at localhost:8080; sign in, save a profile, then connect.") +
     p("Use localhost consistently during the trip. A session established for 127.0.0.1 is associated with a different host from localhost, so a callback to localhost may not recover that session. CORS permission alone does not share cookies between these hostnames.") +
     note("Do not rotate the encryption key casually", "The same saved ciphertext requires the original key. The current format has no key version or migration mechanism. Preserve the key across restarts; plan a token re-encryption or reconnection strategy before replacing it."), sources="Repo: application.properties:23–37; GmailAuthService:37–45; TokenCipher:21–29. Setup " + ref("G1"))

page("Diagnose connection failures", "First identify which leg failed: your API, Google’s screen, or the callback.",
     table(["Symptom", "Where it comes from / what to inspect"], [
         ("401 Login required", "Protected API has no valid JobGraph session. Check /api/auth/me and whether the request carries the cookie."),
         ("403 before controller breakpoint", "Often missing/stale CSRF on a write or a rejected cross-origin request. Inspect the CSRF header and browser Origin/preflight."),
         ("409 Save a profile first", "POST /gmail/connect rejected an account with no saved profile."),
         ("503 during connect", "Missing OAuth client configuration or an invalid encryption key can throw IllegalStateException here."),
         ("Google redirect_uri_mismatch", "Compare the actual authorization URL’s redirect_uri with the web client’s registered callback exactly."),
         ("Google denies a test user", "Check consent-screen audience, test-user address, chosen Google account, and organization policy."),
         ("gmail=session-expired", "Callback cannot recover the required session/login. Check host consistency, session expiry, and whether logout happened."),
         ("gmail=invalid-state", "Returned state/owner/email session data failed validation; an old callback or second tab can cause this."),
         ("gmail=cancelled", "Callback had an error or no code. The UI groups these cases into a cancellation notice."),
         ("gmail=email-mismatch", "Google’s confirmed address differs from the typed address."),
         ("gmail=error", "Token exchange, mailbox lookup, encryption, or persistence failed. The detailed cause is in backend logs.")], [35,65]) +
     p("The UI intentionally exposes short notices. In particular, missing refresh-token failures become the generic gmail=error result even though the service exception explains offline access. Follow the backend exception cause to find the actual reason.") +
     note("A failed replacement", "Completion checks the new address and refresh token before updating the connection. A cancellation or mismatch normally leaves the previous local connection intact."), sources="Repo: GmailConnectionController:73–118; GmailAuthService:73–97. Redirect matching " + ref("G1"))

page("Diagnose fetch failures", "A saved connection can still fail when the backend tries to use its credential.",
     table(["Symptom", "Meaning and next check"], [
         ("409 Connect Gmail first", "No local connection row exists for the authenticated account."),
         ("502 Could not fetch Gmail messages", "The catch block wraps any fetch exception. Inspect the server cause: refresh failure, API response, network issue, parsing failure, or wrong encryption key."),
         ("Google invalid_grant", "A refresh token or code is unusable. During fetch, investigate revoked/expired access; reconnect after fixing the cause."),
         ("Google quota / permission response", "Check the original Google status and reason. The frontend 502 does not distinguish quota, API disabled, or insufficient permission."),
         ("Successful fetch, zero messages", "Check the actual query, date window, chosen Gmail account, and result limit. No matching messages is a normal empty result."),
         ("Messages disappear after restart", "EmailStack is memory-only. The saved Gmail connection can remain while cached messages are lost."),
         ("HTML-looking body or blank body", "Inspect MIME parts. The parser may fall back to HTML text or lack attachment-backed body data."),
         ("Incorrect displayed time", "The backend converts to LocalDateTime and loses the timezone offset; the browser interprets that local-looking timestamp.")], [37,63]) +
     h("Why reconnect may be needed during development") +
     p("Google documents that external apps in Testing can receive refresh tokens expiring after seven days when Gmail scopes are requested. Tokens can also stop working after revocation or other account/policy changes. A saved row does not prevent these provider-side changes." + ref("G6")) +
     h("Useful diagnostics without exposing credentials") +
     bullets("Browser DevTools → Network: inspect method, path, status, response body, and whether the write included its CSRF header.", "Backend log: locate ‘Gmail fetch failed for account …’ or ‘Gmail OAuth callback failed …’, then inspect the nested exception.", "When sharing logs, omit cookies, authorization headers, token response bodies, secrets, and callback codes.") , sources="Repo: EmailController:36–46; GmailFetchService; ConnectGmailButton:52–61")

page("What this version does not promise", "These observations come from the implementation, so you can plan the next steps accurately.",
     table(["Current behavior", "Consequence / a possible next step"], [
         ("Manual fetch only; no scheduler or push subscription", "New mail appears only after a user fetches. Add a scheduled or event-driven sync if needed."),
         ("One list page and sequential detail calls", "Large result sets are incomplete and may be slow. Add pagination and controlled batching/concurrency."),
         ("In-memory deque per app process", "No durable inbox or multi-instance consistency. Persist messages with account and Gmail-connection ownership."),
         ("Only each incoming batch is sorted", "Later custom queries can disturb global newest-first order. Sort the merged collection or query a durable ordered store."),
         ("Disconnect and fetch are separate operations", "An in-flight fetch can repopulate the cache after clear/replacement. Bind writes to a connection version and reject stale results."),
         ("Status checks the database only", "A revoked grant can still look connected. Track reconnect-needed status after provider failures."),
         ("Simple MIME and local timestamps", "Attachment-backed bodies, encoding, and timezone cases need fuller handling."),
         ("One OAuth state slot; no dedicated TTL", "Parallel connect tabs can invalidate each other. Store expiring attempts if multi-tab support is needed."),
         ("No Google revocation on disconnect", "The Google grant can remain. Add explicit revocation if the intended disconnect semantics require it."),
         ("No profile-ranking call in this path", "A fetched email is not a scored job match. Build the ranking stage as a separate, explicit step.")], [43,57]) +
     p("The code also does not add account ID as AES-GCM additional authenticated data, version encryption keys, configure a provider retry/backoff policy, or set an application-level fetch rate limit. These are design improvements, not claims that this guide implemented them.") +
     note("Scope is still a meaningful limit", "The current Google permission is read-only, but the backend has access to message contents. Treat those contents as user data throughout future filtering, storage, and AI processing."), sources="Repo review: controllers, Gmail services, TokenCipher, EmailStack, frontend API")

page("A debugger tour in your IDE", "Use one account and a small test mailbox so each step is easy to follow.",
     table(["Breakpoint", "Inspect and explain"], [
         ("GmailConnectionController.connect()", "authentication.getName(), account ID, saved-profile check, and the presence of session attributes after creation."),
         ("GmailAuthService.authorizationUrl()", "Configured redirect URI, requested scope, login_hint, prompt, and state presence. Avoid copying actual secrets."),
         ("GmailConnectionController.callback()", "Compare account IDs and state-match result. Watch attributes get removed before token exchange."),
         ("GmailAuthService.complete()", "Presence of access/refresh tokens, requested vs confirmed address, and whether fallback is taken. Do not log token values."),
         ("TokenCipher.encrypt()/decrypt()", "Key length=32 bytes, IV length=12 bytes, success/failure of authenticated decryption."),
         ("EmailController.fetchEmails()", "Current account ID, query string, requested/clamped limit, and returned batch size."),
         ("GmailFetchService.fetchEmails()", "List-result count, each message ID, payload MIME types, then the oldest-first sorted batch."),
         ("EmailStack.pushAll()", "Deque contents for only this account and removal of repeated IDs."),
         ("InboxPage.showFetchedEmails()", "POST fetch succeeds, GET stack returns, React state changes, and the list renders.")], [44,56]) +
     h("Trace failures from the outside inward") +
     steps("Find the failing browser request and its HTTP status.", "Confirm it reached the expected controller breakpoint. If not, inspect authentication, CSRF, CORS, or routing.", "If it reached a Google .execute(), inspect the provider exception and reason.", "If Google succeeded, inspect transformation, persistence/cache, and the next frontend request separately.") +
     note("Do not confuse the two localhost services", "The browser usually sends /api requests to Vite at port 5173, which forwards them to Spring at port 8080. Google’s registered callback goes directly to Spring at port 8080."), sources="Repo: method map throughout this guide; vite.config.js:7–9")

page("A complete example to rehearse", "Fictional account 42 logs in with one email and connects a different Gmail mailbox.",
     steps("<b>Login:</b> dev@example.com authenticates to JobGraph. Spring stores its security context in the session. The account’s database ID is 42.", "<b>Save a profile:</b> the resume contact email may be contact@example.com. Neither that field nor the Gmail address replaces account ID 42 as the owner.", "<b>Connect:</b> enter career.mailbox@gmail.com. The server saves state S, account ID 42, and that requested address in the session.", "<b>Consent:</b> choose career.mailbox@gmail.com at Google. Google returns code C and state S through the browser to the callback.", "<b>Validate:</b> the session still belongs to account 42 and state matches. Remove the saved attempt; exchange C for tokens. Gmail’s profile endpoint confirms career.mailbox@gmail.com.", "<b>Persist:</b> encrypt refresh token R. Save account_id=42, the confirmed address, ciphertext, and updated_at. Redirect to /inbox?gmail=connected.", "<b>Fetch:</b> account 42 clicks Fetch job alerts. Decrypt R, let the Java client refresh an access token, search Gmail, and retrieve matching message details.", "<b>Store and show:</b> convert messages to DTOs, push them into stack[42], return counts, and let React GET /emails/stack to render them.", "<b>Different user:</b> account 99’s stack and repository lookup use 99. Supplying another person’s resume contact email does not change those lookups.", "<b>Disconnect:</b> delete account 42’s local Gmail row and clear stack[42]. The JobGraph account and saved profile remain.") +
     h("Predict the result before running it") +
     table(["Change the scenario", "Expected outcome"], [
         ("Choose a different Google address", "email-mismatch; no successful replacement."),
         ("Log out before callback", "Callback cannot complete for the original session."),
         ("Restart after saving the Gmail row", "Connection row remains; cached emails need another fetch."),
         ("Fetch the same message IDs again", "Existing cached copies are removed and refreshed, not duplicated.")], [47,53]), sources="Illustrative walkthrough derived from the current request flow")

page("Learn with the existing tests", "Automated tests explain intent, but mocked Google calls cannot prove live consent works.",
     table(["Existing test area", "What it checks"], [
         ("Authorization URL", "Callback, state, offline access, consent parameter, login hint, and Gmail read-only scope."),
         ("Callback binding", "Wrong state is rejected; a valid callback can use Gmail different from login; cancellation and address mismatch redirect correctly."),
         ("Stack and cipher", "Accounts get separate stacks; repeated entries are deduplicated; tokens round-trip through encryption; malformed ciphertext is rejected."),
         ("AccountProfileIntegrationTests", "Session authentication, CSRF behavior, account isolation, session renewal, and allowed frontend preflight handling.")], [38,62]) +
     example("Run the existing suite", '''cd jobgraph-backend
mvn test''') +
     p("This guide was built by reading source and checking the generated document. It does not claim a new live Gmail consent test or a new backend test run. GmailConnectionTests mocks GmailAuthService.complete() for callback cases, so token exchange, Google address lookup, and refresh-token fallback still need dedicated integration coverage.") +
     h("Exercises with answers") +
     table(["Question", "Answer"], [
         ("Does map(GmailConnection::getGmailAddress) update the row?", "No. It converts Optional&lt;GmailConnection&gt; to Optional&lt;String&gt;. An absent connection stays empty."),
         ("Why is connect POST, but callback GET?", "Connect creates server-side attempt state; Google returns through browser navigation to a validated callback."),
         ("Can we decrypt the password like the token?", "No. Passwords are hashed for comparison; refresh tokens are encrypted because they must be recovered.")], [47,53]) +
     note("Good next test cases", "Cover refresh-token omission, revoked grants, corrupt encrypted tokens, nested MIME, timezones, paging, and fetch-versus-disconnect races. These cases check behavior that the current small test suite does not establish."), sources="Repo: GmailConnectionTests; AccountProfileIntegrationTests")

page("Source map and further reading", "Use the file map to return from the explanation to the implementation.",
     table(["Folder", "Read these files"], [
         ("backend …/gmail/", "GmailConnectionController.java • GmailAuthService.java • TokenCipher.java • GmailFetchService.java • EmailController.java • EmailStack.java • EmailDto.java"),
         ("backend …/model/ and …/repository/", "GmailConnection.java • GmailConnectionRepository.java"),
         ("backend …/config/ and …/controller/", "SecurityConfig.java • CorsConfig.java • AuthController.java; also service/AccountService.java"),
         ("frontend src/components/", "GmailConnectDialog.jsx • ConnectGmailButton.jsx • InboxPage.jsx • FetchedEmails.jsx"),
         ("Other entry points", "frontend src/api/api.js and vite.config.js; backend src/main/resources/application.properties and db/migration/V2__accounts_and_gmail.sql")], [36,64]) +
     p("Backend Java folder prefix: <code>jobgraph-backend/src/main/java/com/JobGraph/jobgraph_backend/</code>. Frontend folder prefix: <code>jobgraph-frontend/</code>. Code line numbers reflect the snapshot captured on 03 October 2026.") +
     h("Official references checked for this guide") +
     '<div class="references">' + ''.join(f'<p><b>{key}</b> <a href="{url}">{title}</a></p>' for key,(title,url) in REFS.items()) + '</div>' +
     p("Library behavior was also checked in locally installed source JARs: google-api-client 2.7.0 (GoogleOAuthConstants) and google-oauth-client 1.36.0 (Credential). These version-specific sources help distinguish what this application actually uses from examples in newer documentation.") +
     note("Rebuild and keep learning", "The companion HTML is searchable and printable. build_guide.py rebuilds its source excerpts; source-manifest.json records the reviewed file paths and SHA-256 hashes. Re-check explanations and line ranges when application code changes."), sources="External links checked 03 October 2026; project source is the primary basis of this guide")

assert len(PAGES) == 30, len(PAGES)

CSS = """
@page { size:A4; margin:0; }
* { box-sizing:border-box; }
html { background:#e5e9e5; }
body { margin:0; color:#213b37; font:10pt/1.5 Arial, sans-serif; -webkit-print-color-adjust:exact; print-color-adjust:exact; }
.sheet { width:210mm; height:297mm; padding:16mm 17mm 18mm; position:relative; background:#fffefb; break-after:page; margin:8mm auto; }
.sheet:last-child { break-after:auto; }
.running { display:flex; justify-content:space-between; align-items:center; padding-bottom:10px; border-bottom:1px solid #d8e3db; font-size:8pt; font-weight:700; letter-spacing:1.3px; color:#387867; }
.brand { font-size:13pt; letter-spacing:-.6px; color:#163e37; }.brand b { color:#d17850; }
h1 { font:500 29pt/1.1 Georgia, serif; letter-spacing:-1px; margin:24px 0 12px; color:#183f39; }
.sub { color:#657b72; margin:0 0 21px; font-size:11pt; line-height:1.45; }
h3 { font-size:11pt; color:#1d6454; margin:19px 0 6px; }
p { margin:0 0 11px; }
ul,ol { margin:8px 0 14px; padding-left:21px; }
li { padding-left:3px; margin:0 0 7px; }
li::marker { color:#bd704b; font-weight:700; }
code { font:8.4pt Menlo, Consolas, monospace; color:#26594d; overflow-wrap:anywhere; }
table { width:100%; border-collapse:collapse; table-layout:fixed; margin:10px 0 16px; font-size:9.15pt; line-height:1.43; }
th { text-align:left; background:#1d5147; color:#fff; font-size:8.6pt; padding:10px 11px; }
td { padding:9px 11px; vertical-align:top; border-bottom:1px solid #dce5de; overflow-wrap:anywhere; }
tbody tr:nth-child(even) { background:#f3f6f1; }
.callout { margin:15px 0 14px; padding:13px 16px; border-left:3px solid #b9754c; border-radius:0 8px 8px 0; background:#faf0e5; font-size:9.2pt; line-height:1.45; }
.callout strong { color:#734c32; display:block; margin-bottom:4px; }.callout p { margin:0; }
.code { margin:13px 0 15px; border:1px solid #cedbd3; border-radius:8px; overflow:hidden; background:#edf3ef; }
.code figcaption { background:#dae8df; padding:7px 10px; font-size:8pt; font-weight:700; color:#1c5948; }
.code figcaption span { font-size:7pt; font-weight:400; float:right; padding-top:1px; }
.code-body { padding:9px 10px 10px 0; }
.code-line { display:grid; grid-template-columns:32px minmax(0,1fr); font:7.55pt/1.48 Menlo, Consolas, monospace; }
.code-line > span:last-child { white-space:pre-wrap; overflow-wrap:anywhere; }
.ln { text-align:right; color:#89998e; padding-right:9px; font-size:6.9pt; user-select:none; }
pre { white-space:pre-wrap; overflow-wrap:anywhere; font:8pt/1.5 Menlo, Consolas, monospace; margin:0; padding:12px; }
svg { display:block; width:100%; height:auto; font-family:Arial,sans-serif; margin:7px 0 15px; }
.ref { font-size:8pt; color:#2c7563; padding-left:4px; text-decoration:none; }
a { color:#276c5b; text-underline-offset:2px; }
.footer { position:absolute; left:17mm; right:17mm; bottom:9mm; border-top:1px solid #d8e3db; padding-top:7px; display:flex; justify-content:space-between; gap:12px; color:#6b8278; font-size:7pt; line-height:1.4; }
.footer .sources { max-width:90%; }.footer .number { white-space:nowrap; color:#23564a; font-weight:700; }
.cover h1 { font-size:51pt; margin-top:49px; letter-spacing:-2.4px; }
.cover .sub { max-width:520px; font-size:15pt; }.cover-art { margin-top:25px; }
.cover-tags { display:flex; gap:10px; flex-wrap:wrap; margin:12px 0 25px; }.cover-tags span { padding:6px 12px; border:1px solid #bed4c4; border-radius:22px; color:#296b56; font-size:8pt; }
.references { margin:9px 0 14px; }.references p { margin:0 0 8px; font-size:9pt; }.references b { display:inline-block; width:26px; color:#ad633f; }
@media print { html { background:white; }.sheet { margin:0; } }
"""

parts = []
for i, pg in enumerate(PAGES, 1):
    klass = "sheet cover" if i == 1 else "sheet"
    parts.append(f'<article class="{klass}" id="page-{i}"><div class="running"><span class="brand">jobgraph<b>.</b></span><span>{pg["section"]}</span></div><div class="page-content"><h1>{pg["title"]}</h1><p class="sub">{pg["sub"]}</p><div class="body-content">{pg["body"]}</div></div><footer class="footer"><span class="sources">{pg["sources"] or "JOBGRAPH • GMAIL LEARNING GUIDE • 03 OCTOBER 2026"}</span><span class="number">{i:02d} / {len(PAGES)}</span></footer></article>')
html = '<!doctype html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>JobGraph — Gmail Connection, Reading &amp; Security Guide</title><style>' + CSS + '</style></head><body>' + ''.join(parts) + '</body></html>'
(OUT / "JobGraph-Gmail-Guide.html").write_text(html)
(OUT / "source-manifest.json").write_text(json.dumps({"snapshot_date": "2026-10-03", "files": [{"path": path, "sha256": hashlib.sha256(SNAPSHOT[key].encode()).hexdigest()} for key,path in FILES.items()], "references": REFS}, indent=2) + "\n")
print(f"Wrote {len(PAGES)} pages to {OUT / 'JobGraph-Gmail-Guide.html'}")
