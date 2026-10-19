---
parent: Requirements
---
# CiteDrive

## Log in to CiteDrive
`req~citedrive.login~1`

JabRef logs the user in to CiteDrive in the browser (OAuth 2.0 authorization code flow with PKCE).
The browser returns to JabRef's HTTP server; if it is disabled, JabRef asks the user to enable it and only then opens the browser.
`jabsrv --citedrive-login` offers the same login without the GUI: it prints the login address instead of opening the browser.
A login not completed within ten minutes fails.
The refresh token is stored in the system keyring only, never in the preferences.

Needs: impl, utest

## Push the library to CiteDrive
`req~citedrive.push~1`

"Push to CiteDrive" uploads the current library as BibTeX.
Without a valid refresh token, JabRef logs in first.
A network error while refreshing the token keeps the stored login.

Needs: impl, utest

## Open the import page after a push
`req~citedrive.push.import-page~1`

After a successful push, JabRef opens the CiteDrive page where the user chooses a project and the pushed entries to import.

Needs: impl

## CiteDrive credentials must remain bound to their server
`req~citedrive.credentials.server~1`

Changing the API address invalidates the saved login, including pending authentication responses.
Stored refresh tokens are loaded only for the address that issued them.
An upload uses the API address captured when its access token was requested.

Needs: impl, utest

## CiteDrive server addresses must use HTTPS
`req~citedrive.https~1`

The authorization, token, upload, and web application addresses must use HTTPS.
The browser callback remains on the local HTTP loopback listener.

Needs: impl, utest

## Concurrent CiteDrive token requests must share one authentication operation
`req~citedrive.refresh.single-request~1`

Requests for the same server configuration share an outstanding token request so that rotating refresh tokens are not reused concurrently.

Needs: impl, utest

## CiteDrive token requests must have finite network timeouts
`req~citedrive.token.timeout~1`

Authorization code exchange and refresh use a ten-second connection timeout and a thirty-second read timeout.
Token exchange runs outside the browser callback request thread.

Needs: impl, utest

## CiteDrive must preserve the login after retryable refresh failures
`req~citedrive.refresh.failure~1`

Network, parsing, rate limit, server, and client configuration failures retain the refresh token without starting browser authentication.
Only an `invalid_grant` response with HTTP status 400 clears the token and starts another login.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
