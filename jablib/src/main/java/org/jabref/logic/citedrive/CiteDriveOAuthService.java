package org.jabref.logic.citedrive;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.CiteDrivePreferences;
import org.jabref.logic.remote.RemotePreferences;

import com.google.common.annotations.VisibleForTesting;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// OAuth 2.0 authorization code flow with PKCE against CiteDrive.
///
/// The browser redirects back to JabRef's HTTP server ([org.jabref.logic.citedrive.OAuthSessionRegistry] receives the code).
@NullMarked
public class CiteDriveOAuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiteDriveOAuthService.class);

    private static final ClientID CLIENT_ID = new ClientID("jabref-desktop");

    private final URI authEndpoint;
    private final URI tokenEndpoint;

    private final RemotePreferences remotePreferences;
    private final CiteDrivePreferences citeDrivePreferences;
    private final OAuthSessionRegistry sessionRegistry;
    private final Consumer<URI> authorizationPageOpener;

    /// @param authorizationPageOpener shows the CiteDrive login page to the user (in the browser)
    public CiteDriveOAuthService(RemotePreferences remotePreferences,
                                 CiteDrivePreferences citeDrivePreferences,
                                 OAuthSessionRegistry sessionRegistry,
                                 Consumer<URI> authorizationPageOpener) {
        this(remotePreferences, citeDrivePreferences, sessionRegistry, authorizationPageOpener, citeDrivePreferences.getAuthorizationEndpoint(), citeDrivePreferences.getTokenEndpoint());
    }

    @VisibleForTesting
    public CiteDriveOAuthService(RemotePreferences remotePreferences,
                                 CiteDrivePreferences citeDrivePreferences,
                                 OAuthSessionRegistry sessionRegistry,
                                 Consumer<URI> authorizationPageOpener,
                                 URI authEndpoint,
                                 URI tokenEndpoint) {
        this.remotePreferences = remotePreferences;
        this.citeDrivePreferences = citeDrivePreferences;
        this.sessionRegistry = sessionRegistry;
        this.authorizationPageOpener = authorizationPageOpener;
        this.authEndpoint = authEndpoint;
        this.tokenEndpoint = tokenEndpoint;
    }

    /// Lets the user log in in the browser. Also stores the refresh token.
    ///
    /// [impl->req~citedrive.login~1]
    ///
    /// Fails if the HTTP server receiving the browser redirect is disabled, or if the user does not finish logging in in time.
    public CompletableFuture<Optional<AccessToken>> authorizeInteractive() {
        if (!remotePreferences.shouldEnableHttpServer()) {
            return CompletableFuture.failedFuture(new JabRefException(
                    "HTTP server disabled",
                    Localization.lang("Logging in to CiteDrive needs the HTTP server. Please enable it in the preferences.")));
        }

        String state = UUID.randomUUID().toString();
        // One verifier per login attempt: a second attempt must not invalidate the code of the first one
        CodeVerifier codeVerifier = new CodeVerifier();
        CompletableFuture<String> codeFuture = sessionRegistry.register(state);

        URI authUrl = new AuthorizationRequest.Builder(ResponseType.CODE, CLIENT_ID)
                .endpointURI(authEndpoint)
                .redirectionURI(getCallBackUri())
                .state(new State(state))
                .scope(new Scope("read", "write")) // required for CiteDrive API
                .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
                .build()
                .toURI();
        authorizationPageOpener.accept(authUrl);

        return codeFuture.thenApply(code -> exchangeCodeForToken(code, codeVerifier));
    }

    /// Redirects to [org.jabref.http.server.resources.callback.CallbackResource#citeDriveCallback]
    private URI getCallBackUri() {
        // "/callback" is standard for OAuth2 redirect URIs
        return remotePreferences.getHttpServerUri().resolve("/callback");
    }

    /// Also stores the refresh token
    private Optional<AccessToken> exchangeCodeForToken(String code, CodeVerifier codeVerifier) {
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(new AuthorizationCode(code), getCallBackUri(), codeVerifier);
        TokenResponse response;
        try {
            response = TokenResponse.parse(new TokenRequest(tokenEndpoint, CLIENT_ID, codeGrant).toHTTPRequest().send());
        } catch (ParseException | IOException e) {
            LOGGER.error("Could not obtain CiteDrive token", e);
            return Optional.empty();
        }
        if (!response.indicatesSuccess()) {
            TokenErrorResponse errorResponse = response.toErrorResponse();
            LOGGER.error("Could not receive token {}", errorResponse.getErrorObject().toJSONObject());
            return Optional.empty();
        }
        AccessTokenResponse successResponse = response.toSuccessResponse();
        citeDrivePreferences.setRefreshToken(successResponse.getTokens().getRefreshToken());
        return Optional.of(successResponse.getTokens().getAccessToken());
    }

    /// Refreshes the access token using the stored refresh token.
    /// Goes interactive if there is no refresh token or CiteDrive rejects it.
    ///
    /// Also stores the refresh token
    public CompletableFuture<Optional<AccessToken>> getAccessToken() {
        RefreshToken cachedRefreshToken = citeDrivePreferences.getRefreshToken();
        if (cachedRefreshToken == null) {
            return authorizeInteractive();
        }

        return CompletableFuture.supplyAsync(() -> refreshToken(cachedRefreshToken))
                                .thenCompose(token -> {
                                    if (token.isPresent()) {
                                        return CompletableFuture.completedFuture(token);
                                    }
                                    LOGGER.info("Refresh token rejected, falling back to interactive authentication");
                                    return authorizeInteractive();
                                });
    }

    /// @return empty if CiteDrive rejected the refresh token (it is then removed)
    /// @throws CompletionException on network or parse errors; the refresh token is kept to retry later
    private Optional<AccessToken> refreshToken(RefreshToken refreshToken) {
        TokenResponse response;
        try {
            TokenRequest request = new TokenRequest(tokenEndpoint, CLIENT_ID, new RefreshTokenGrant(refreshToken));
            response = TokenResponse.parse(request.toHTTPRequest().send());
        } catch (IOException | ParseException e) {
            throw new CompletionException(e);
        }

        if (!response.indicatesSuccess()) {
            LOGGER.warn("Refresh token failed: {}", response.toErrorResponse().getErrorObject().toJSONObject());
            citeDrivePreferences.setRefreshToken(null);
            return Optional.empty();
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();
        RefreshToken newRefreshToken = successResponse.getTokens().getRefreshToken();
        // Some providers may omit refresh_token on refresh; keep the old one in that case
        if (newRefreshToken != null) {
            citeDrivePreferences.setRefreshToken(newRefreshToken);
        }
        return Optional.of(successResponse.getTokens().getAccessToken());
    }
}
