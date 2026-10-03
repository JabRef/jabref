---
parent: Requirements
---
# CiteDrive

## Log in to CiteDrive
`req~citedrive.login~1`

JabRef logs the user in to CiteDrive in the browser (OAuth 2.0 authorization code flow with PKCE).
The browser returns to JabRef's HTTP server; if it is disabled, JabRef asks the user to enable it and only then opens the browser.
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

<!-- markdownlint-disable-file MD022 -->
