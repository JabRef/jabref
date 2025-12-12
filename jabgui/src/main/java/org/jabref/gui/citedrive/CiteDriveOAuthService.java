package org.jabref.gui.citedrive;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jabref.gui.DialogService;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.citedrive.OAuthSessionRegistry;
import org.jabref.logic.remote.RemotePreferences;

import com.google.common.annotations.VisibleForTesting;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
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
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiteDriveOAuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiteDriveOAuthService.class);

    private static final ClientID CLIENT_ID = new ClientID("jabref-desktop");

    private static final URI DEFAULT_AUTH_ENDPOINT;
    private static final URI DEFAULT_TOKEN_ENDPOINT;

    static {
        try {
            DEFAULT_AUTH_ENDPOINT = new URI("https://api-dev.citedrive.com/jabref/login/");
            DEFAULT_TOKEN_ENDPOINT = new URI("https://api-dev.citedrive.com/o/token/");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private final DialogService dialogService;
    private final URI AUTH_ENDPOINT;
    private final URI TOKEN_ENDPOINT;

    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final RemotePreferences remotePreferences;
    private final OAuthSessionRegistry sessionRegistry;

    private CodeVerifier codeVerifier; // store per-login attempt (TODO: better: map by state - in OAuthSessionRegistry?)

    public CiteDriveOAuthService(
            ExternalApplicationsPreferences externalApplicationsPreferences,
            RemotePreferences remotePreferences,
            OAuthSessionRegistry sessionRegistry,
            DialogService dialogService) {
        this(externalApplicationsPreferences, remotePreferences, sessionRegistry, dialogService, DEFAULT_AUTH_ENDPOINT, DEFAULT_TOKEN_ENDPOINT);
    }

    @VisibleForTesting
    public CiteDriveOAuthService(
            ExternalApplicationsPreferences externalApplicationsPreferences,
            RemotePreferences remotePreferences,
            OAuthSessionRegistry sessionRegistry,
            DialogService dialogService,
            URI authEndpoint,
            URI tokenEndpoint) {
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.remotePreferences = remotePreferences;
        this.sessionRegistry = sessionRegistry;
        this.dialogService = dialogService;
        this.AUTH_ENDPOINT = authEndpoint;
        this.TOKEN_ENDPOINT = tokenEndpoint;
    }

    public CompletableFuture<Optional<Tokens>> authorizeInteractive() {
        String state = UUID.randomUUID().toString();
        CompletableFuture<String> codeFuture = sessionRegistry.register(state);

        URI authUrl = buildAuthUrl(state);
        org.jabref.gui.desktop.os.NativeDesktop.openBrowserShowPopup(authUrl.toASCIIString(), dialogService, externalApplicationsPreferences);

        return codeFuture.thenApply(code -> exchangeCodeForToken(code));
    }

    private URI buildAuthUrl(String state) {
        this.codeVerifier = new CodeVerifier();
        URI uri = new AuthorizationRequest.Builder(ResponseType.CODE, CLIENT_ID)
                .endpointURI(AUTH_ENDPOINT)
                .redirectionURI(getCallBackUri())
                .state(new State(state))
                // .scope(new Scope("openid", "read", "write")) // only required for https://github.com/navikt/mock-oauth2-server
                .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
                .build()
                .toURI();
        LOGGER.trace("PKCE verifier:  {}", codeVerifier.getValue());
        LOGGER.trace("Auth URL:       {}", uri);
        return uri;
    }

    /// Redirects to [org.jabref.http.server.resources.callback.CallbackResource#citeDriveCallback]
    private @NonNull URI getCallBackUri() {
        // "/callback" is standard for OAuth2 redirect URIs
        return remotePreferences.getHttpServerUri().resolve("/callback");
    }

    private Optional<Tokens> exchangeCodeForToken(String code) {
        LOGGER.trace("Received code {}", code);
        AuthorizationCode authCode = new AuthorizationCode(code);
        LOGGER.trace("Using PKCE code verifier: {}", codeVerifier.getValue());
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(authCode, getCallBackUri(), codeVerifier);
        TokenRequest request = new TokenRequest(TOKEN_ENDPOINT, CLIENT_ID, codeGrant);
        TokenResponse response;
        try {
            HTTPRequest httpRequest = request.toHTTPRequest();
            HTTPResponse httpResponse = httpRequest.send();
            response = TokenResponse.parse(httpResponse);
        } catch (ParseException e) {
            LOGGER.error("Could not parse response", e);
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.error("I/O error", e);
            return Optional.empty();
        }
        if (!response.indicatesSuccess()) {
            TokenErrorResponse errorResponse = response.toErrorResponse();
            LOGGER.error("Could not receive token {}", errorResponse.getErrorObject().toJSONObject());
            return Optional.empty();
        }
        AccessTokenResponse successResponse = response.toSuccessResponse();
        AccessToken accessToken = successResponse.getTokens().getAccessToken();
        RefreshToken refreshToken = successResponse.getTokens().getRefreshToken();
        return Optional.of(new Tokens(accessToken, refreshToken));
    }
}
