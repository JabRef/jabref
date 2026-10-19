package org.jabref.logic.citedrive;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
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
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;

    private final Supplier<URI> authEndpoint;
    private final Supplier<URI> tokenEndpoint;
    private final TokenRequestSender tokenRequestSender;

    private final RemotePreferences remotePreferences;
    private final CiteDrivePreferences citeDrivePreferences;
    private final OAuthSessionRegistry sessionRegistry;
    private final Consumer<URI> authorizationPageOpener;
    private final BooleanSupplier callbackServerEnabler;

    /// @param authorizationPageOpener shows the CiteDrive login page to the user (in the browser)
    /// @param callbackServerEnabler   makes the HTTP server receiving the browser redirect run; false if it stays off
    public CiteDriveOAuthService(RemotePreferences remotePreferences,
                                 CiteDrivePreferences citeDrivePreferences,
                                 OAuthSessionRegistry sessionRegistry,
                                 Consumer<URI> authorizationPageOpener,
                                 BooleanSupplier callbackServerEnabler) {
        this(remotePreferences, citeDrivePreferences, sessionRegistry, authorizationPageOpener, callbackServerEnabler,
                citeDrivePreferences::getAuthorizationEndpoint, citeDrivePreferences::getTokenEndpoint, HTTPRequest::send);
    }

    @VisibleForTesting
    public CiteDriveOAuthService(RemotePreferences remotePreferences,
                                 CiteDrivePreferences citeDrivePreferences,
                                 OAuthSessionRegistry sessionRegistry,
                                 Consumer<URI> authorizationPageOpener,
                                 BooleanSupplier callbackServerEnabler,
                                 URI authEndpoint,
                                 URI tokenEndpoint) {
        this(remotePreferences, citeDrivePreferences, sessionRegistry, authorizationPageOpener, callbackServerEnabler,
                () -> authEndpoint, () -> tokenEndpoint, HTTPRequest::send);
    }

    @VisibleForTesting
    CiteDriveOAuthService(RemotePreferences remotePreferences,
                          CiteDrivePreferences citeDrivePreferences,
                          OAuthSessionRegistry sessionRegistry,
                          Consumer<URI> authorizationPageOpener,
                          BooleanSupplier callbackServerEnabler,
                          TokenRequestSender tokenRequestSender) {
        this(remotePreferences, citeDrivePreferences, sessionRegistry, authorizationPageOpener, callbackServerEnabler,
                citeDrivePreferences::getAuthorizationEndpoint, citeDrivePreferences::getTokenEndpoint, tokenRequestSender);
    }

    private CiteDriveOAuthService(RemotePreferences remotePreferences,
                                  CiteDrivePreferences citeDrivePreferences,
                                  OAuthSessionRegistry sessionRegistry,
                                  Consumer<URI> authorizationPageOpener,
                                  BooleanSupplier callbackServerEnabler,
                                  Supplier<URI> authEndpoint,
                                  Supplier<URI> tokenEndpoint,
                                  TokenRequestSender tokenRequestSender) {
        this.remotePreferences = remotePreferences;
        this.citeDrivePreferences = citeDrivePreferences;
        this.sessionRegistry = sessionRegistry;
        this.authorizationPageOpener = authorizationPageOpener;
        this.callbackServerEnabler = callbackServerEnabler;
        this.authEndpoint = authEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.tokenRequestSender = tokenRequestSender;
    }

    /// Lets the user log in in the browser. Also stores the refresh token.
    ///
    /// [impl->req~citedrive.login~1]
    ///
    /// Fails if the HTTP server receiving the browser redirect is disabled, or if the user does not finish logging in in time.
    public CompletableFuture<Optional<AccessToken>> authorizeInteractive() {
        return authorizeInteractive(snapshotConfiguration()).thenApply(session -> session.map(AuthorizedSession::accessToken));
    }

    private CompletableFuture<Optional<AuthorizedSession>> authorizeInteractive(ServerConfiguration configuration) {
        if (!configuration.isSecure() || configuration.revision() != citeDrivePreferences.getServerRevision()) {
            return CompletableFuture.failedFuture(loginFailure("Invalid or changed CiteDrive server configuration"));
        }
        if (!remotePreferences.shouldEnableHttpServer() && !callbackServerEnabler.getAsBoolean()) {
            return CompletableFuture.failedFuture(new JabRefException(
                    "HTTP server disabled",
                    Localization.lang("Logging in to CiteDrive needs the HTTP server. Please enable it in the preferences.")));
        }

        String state = UUID.randomUUID().toString();
        // One verifier per login attempt: a second attempt must not invalidate the code of the first one
        CodeVerifier codeVerifier = new CodeVerifier();
        CompletableFuture<String> codeFuture = sessionRegistry.register(state);

        URI authUrl = new AuthorizationRequest.Builder(ResponseType.CODE, CLIENT_ID)
                .endpointURI(configuration.authEndpoint())
                .redirectionURI(configuration.callbackUri())
                .state(new State(state))
                .scope(new Scope("read", "write")) // required for CiteDrive API
                .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
                .build()
                .toURI();
        authorizationPageOpener.accept(authUrl);

        return codeFuture.thenApplyAsync(code -> exchangeCodeForToken(code, codeVerifier, configuration));
    }

    private URI getCallBackUri() {
        return remotePreferences.getHttpServerUri().resolve("/callback");
    }

    /// Also stores the refresh token
    private Optional<AuthorizedSession> exchangeCodeForToken(String code, CodeVerifier codeVerifier, ServerConfiguration configuration) {
        if (configuration.revision() != citeDrivePreferences.getServerRevision()) {
            return Optional.empty();
        }
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(new AuthorizationCode(code), configuration.callbackUri(), codeVerifier);
        TokenResponse response;
        try {
            response = sendTokenRequest(configuration.tokenEndpoint(), codeGrant);
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
        if (!citeDrivePreferences.updateRefreshToken(configuration.revision(), Optional.ofNullable(successResponse.getTokens().getRefreshToken()))) {
            return Optional.empty();
        }
        return Optional.of(configuration.authorizedSession(successResponse.getTokens().getAccessToken()));
    }

    /// Refreshes the access token using the stored refresh token.
    /// Goes interactive if there is no refresh token or CiteDrive rejects it.
    ///
    /// Also stores the refresh token
    public CompletableFuture<Optional<AccessToken>> getAccessToken() {
        return getAuthorizedSession().thenApply(session -> session.map(AuthorizedSession::accessToken));
    }

    /// Pins the upload and import addresses to the server that issued the access token.
    ///
    /// [impl->req~citedrive.credentials.server~1]
    public CompletableFuture<Optional<AuthorizedSession>> getAuthorizedSession() {
        ServerConfiguration configuration = snapshotConfiguration();
        if (!configuration.isSecure()) {
            return CompletableFuture.failedFuture(loginFailure("CiteDrive requires HTTPS server addresses"));
        }

        return sessionRegistry.requestToken(citeDrivePreferences, configuration.revision(), () -> {
            Optional<RefreshToken> cachedRefreshToken;
            synchronized (citeDrivePreferences) {
                if (configuration.revision() != citeDrivePreferences.getServerRevision()) {
                    return CompletableFuture.failedFuture(loginFailure("CiteDrive server configuration changed"));
                }
                cachedRefreshToken = Optional.ofNullable(citeDrivePreferences.getRefreshToken());
            }
            return cachedRefreshToken.map(refreshToken -> CompletableFuture.supplyAsync(() -> refreshToken(refreshToken, configuration))
                                                                           .thenCompose(session -> session.map(value -> CompletableFuture.completedFuture(Optional.of(value)))
                                                                                                          .orElseGet(() -> authorizeInteractive(configuration))))
                                     .orElseGet(() -> authorizeInteractive(configuration));
        });
    }

    /// @return empty if CiteDrive rejected the refresh token (it is then removed)
    /// @throws CompletionException on network or parse errors; the refresh token is kept to retry later
    ///
    /// [impl->req~citedrive.refresh.failure~1]
    private Optional<AuthorizedSession> refreshToken(RefreshToken refreshToken, ServerConfiguration configuration) {
        if (configuration.revision() != citeDrivePreferences.getServerRevision()) {
            throw new CompletionException(loginFailure("CiteDrive server configuration changed"));
        }
        TokenResponse response;
        try {
            response = sendTokenRequest(configuration.tokenEndpoint(), new RefreshTokenGrant(refreshToken));
        } catch (IOException | ParseException e) {
            LOGGER.warn("Could not refresh CiteDrive token", e);
            throw new CompletionException(e);
        }

        if (!response.indicatesSuccess()) {
            var error = response.toErrorResponse().getErrorObject();
            LOGGER.warn("Refresh token failed: {}", error.toJSONObject());
            if (OAuth2Error.INVALID_GRANT.getCode().equals(error.getCode()) && error.getHTTPStatusCode() == HTTPResponse.SC_BAD_REQUEST) {
                citeDrivePreferences.updateRefreshToken(configuration.revision(), Optional.empty());
                return Optional.empty();
            }
            throw new CompletionException(loginFailure("CiteDrive token endpoint failed: " + error.getCode()));
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();
        Optional<RefreshToken> newRefreshToken = Optional.ofNullable(successResponse.getTokens().getRefreshToken()).or(() -> Optional.of(refreshToken));
        if (!citeDrivePreferences.updateRefreshToken(configuration.revision(), newRefreshToken)) {
            throw new CompletionException(loginFailure("CiteDrive server configuration changed"));
        }
        return Optional.of(configuration.authorizedSession(successResponse.getTokens().getAccessToken()));
    }

    // [impl->req~citedrive.token.timeout~1]
    private TokenResponse sendTokenRequest(URI endpoint, AuthorizationGrant grant) throws IOException, ParseException {
        HTTPRequest request = new TokenRequest.Builder(endpoint, CLIENT_ID, grant).build().toHTTPRequest();
        request.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        request.setReadTimeout(READ_TIMEOUT_MILLIS);
        request.setFollowRedirects(false);
        return TokenResponse.parse(tokenRequestSender.send(request));
    }

    private ServerConfiguration snapshotConfiguration() {
        synchronized (citeDrivePreferences) {
            return new ServerConfiguration(citeDrivePreferences.getServerRevision(), authEndpoint.get(), tokenEndpoint.get(),
                    getCallBackUri(), citeDrivePreferences.getPushEndpoint(), citeDrivePreferences.getImportPage());
        }
    }

    private static JabRefException loginFailure(String message) {
        return new JabRefException(message, Localization.lang("Login could not be completed"));
    }

    /// An access token can only be uploaded to the address captured with its authentication operation.
    @NullMarked
    public record AuthorizedSession(AccessToken accessToken, URI pushEndpoint, URI importPage) {
    }

    @FunctionalInterface
    @NullMarked
    interface TokenRequestSender {
        HTTPResponse send(HTTPRequest request) throws IOException;
    }

    @NullMarked
    private record ServerConfiguration(long revision, URI authEndpoint, URI tokenEndpoint, URI callbackUri, URI pushEndpoint, URI importPage) {
        boolean isSecure() {
            return CiteDrivePreferences.isSecureUrl(authEndpoint.toString())
                    && CiteDrivePreferences.isSecureUrl(tokenEndpoint.toString())
                    && CiteDrivePreferences.isSecureUrl(pushEndpoint.toString())
                    && CiteDrivePreferences.isSecureUrl(importPage.toString());
        }

        AuthorizedSession authorizedSession(AccessToken accessToken) {
            return new AuthorizedSession(accessToken, pushEndpoint, importPage);
        }
    }
}
