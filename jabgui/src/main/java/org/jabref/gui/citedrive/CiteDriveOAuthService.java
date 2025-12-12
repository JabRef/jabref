package org.jabref.gui.citedrive;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.citedrive.OAuthSessionRegistry;
import org.jabref.logic.remote.RemotePreferences;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeChallenge;
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
    private static final Scope SCOPE = Scope.parse("read write");

    private static final URI AUTH_ENDPOINT;
    private static final URI TOKEN_ENDPOINT;

    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final RemotePreferences remotePreferences;
    private final OAuthSessionRegistry sessionRegistry;

    static {
        try {
            AUTH_ENDPOINT = new URI("https://api-dev.citedrive.com/jabref/login/");
            TOKEN_ENDPOINT = new URI("https://api-dev.citedrive.com/o/token/");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private CodeVerifier lastPkceVerifier; // store per-login attempt (TODO: better: map by state - in OAuthSessionRegistry?)

    public CiteDriveOAuthService(
            ExternalApplicationsPreferences externalApplicationsPreferences,
            RemotePreferences remotePreferences,
            OAuthSessionRegistry sessionRegistry) {
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.remotePreferences = remotePreferences;
        this.sessionRegistry = sessionRegistry;
    }

    public CompletableFuture<Optional<Tokens>> authorizeInteractive() {
        String state = UUID.randomUUID().toString();
        CompletableFuture<String> codeFuture = sessionRegistry.register(state);

        URI authUrl = buildAuthUrl(state);
        try {
            org.jabref.gui.desktop.os.NativeDesktop.openBrowser(authUrl, externalApplicationsPreferences);
        } catch (IOException e) {
            LOGGER.error("Could not open browser", e);
            return CompletableFuture.failedFuture(e);
        }

        return codeFuture.thenApply(code -> exchangeCodeForToken(code));
    }

    private URI buildAuthUrl(String state) {
        CodeVerifier codeVerifier = new CodeVerifier();
        lastPkceVerifier = codeVerifier;

        CodeChallenge codeChallenge = CodeChallenge.compute(CodeChallengeMethod.S256, codeVerifier);

        return new AuthorizationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE), CLIENT_ID)
                .scope(SCOPE)
                .state(new State(state))
                .redirectionURI(getCallBackUri())
                .endpointURI(AUTH_ENDPOINT)
                .codeChallenge(codeChallenge, CodeChallengeMethod.S256)
                .build()
                .toURI();
    }

    /// Redirects to [org.jabref.http.server.resources.callback.CallbackResource#citeDriveCallback]
    private @NonNull URI getCallBackUri() {
        // "/callback" is standard for OAuth2 redirect URIs
        return remotePreferences.getHttpServerUri().resolve("/callback");
    }

    private Optional<Tokens> exchangeCodeForToken(String code) {
        AuthorizationCode authCode = new AuthorizationCode(code);
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(authCode, getCallBackUri(), lastPkceVerifier);
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
