package org.jabref.logic.citedrive;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jabref.logic.net.CiteDrivePreferences;
import org.jabref.logic.remote.RemotePreferences;

import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// [utest->req~citedrive.login~1]
// [utest->req~citedrive.push~1]
@NullMarked
@ResourceLock("Localization.lang")
class CiteDriveOAuthServiceTest {

    private final RemotePreferences remotePreferences = mock(RemotePreferences.class);
    private final CiteDrivePreferences preferences = CiteDrivePreferences.getDefault();
    private final OAuthSessionRegistry registry = new OAuthSessionRegistry();
    private final AtomicReference<URI> openedPage = new AtomicReference<>();
    private final CiteDriveOAuthService.TokenRequestSender sender = mock(CiteDriveOAuthService.TokenRequestSender.class);

    @BeforeEach
    void setUp() {
        when(remotePreferences.shouldEnableHttpServer()).thenReturn(true);
        when(remotePreferences.getHttpServerUri()).thenReturn(URI.create("http://127.0.0.1:23119/"));
    }

    @Test
    void disabledHttpServerFailsWithoutOpeningBrowser() {
        when(remotePreferences.shouldEnableHttpServer()).thenReturn(false);

        assertThrows(ExecutionException.class, () -> service().authorizeInteractive().get(5, TimeUnit.SECONDS));
        assertEquals(Optional.empty(), Optional.ofNullable(openedPage.get()));
        verifyNoInteractions(sender);
    }

    @Test
    void networkErrorOnRefreshKeepsStoredLogin() throws IOException {
        RefreshToken refreshToken = new RefreshToken("stored");
        preferences.setRefreshToken(refreshToken);
        when(sender.send(any())).thenThrow(new SocketTimeoutException("Token endpoint timed out"));

        assertThrows(ExecutionException.class, () -> service().getAccessToken().get(5, TimeUnit.SECONDS));
        assertEquals(refreshToken, preferences.getRefreshToken());
        assertEquals(Optional.empty(), Optional.ofNullable(openedPage.get()));
    }

    @Test
    void refreshWithoutRotationKeepsStoredRefreshToken() throws Exception {
        RefreshToken refreshToken = new RefreshToken("stored");
        preferences.setRefreshToken(refreshToken);
        when(sender.send(any())).thenReturn(response(200, """
                {"access_token":"access","token_type":"Bearer"}
                """));

        assertEquals(new BearerAccessToken("access"), service().getAccessToken().get(5, TimeUnit.SECONDS).orElseThrow());
        assertEquals(refreshToken, preferences.getRefreshToken());
    }

    @Test
    void interactiveLoginWithoutRefreshTokenDiscardsPreviousAccountToken() throws Exception {
        preferences.setRefreshToken(new RefreshToken("previous-account"));
        when(sender.send(any())).thenReturn(response(200, """
                {"access_token":"access","token_type":"Bearer"}
                """));

        var login = service().authorizeInteractive();
        AuthorizationRequest request = AuthorizationRequest.parse(openedPage.get());
        registry.complete(request.getState().getValue(), "code");

        assertEquals(new BearerAccessToken("access"), login.get(5, TimeUnit.SECONDS).orElseThrow());
        assertEquals(Optional.empty(), Optional.ofNullable(preferences.getRefreshToken()));
    }

    @ParameterizedTest
    @CsvSource({"400,invalid_client", "429,slow_down", "503,temporarily_unavailable", "503,invalid_grant"})
    void retryableOrConfigurationErrorKeepsStoredLogin(int status, String error) throws IOException, ParseException {
        // [utest->req~citedrive.refresh.failure~1]
        RefreshToken refreshToken = new RefreshToken("stored");
        preferences.setRefreshToken(refreshToken);
        when(sender.send(any())).thenReturn(response(status, "{\"error\":\"" + error + "\"}"));

        assertThrows(ExecutionException.class, () -> service().getAccessToken().get(5, TimeUnit.SECONDS));
        assertEquals(refreshToken, preferences.getRefreshToken());
        assertEquals(Optional.empty(), Optional.ofNullable(openedPage.get()));
    }

    @Test
    void temporaryFailureCanBeRetriedWithoutAnotherLogin() throws Exception {
        // [utest->req~citedrive.refresh.failure~1]
        preferences.setRefreshToken(new RefreshToken("stored"));
        when(sender.send(any())).thenReturn(response(503, "{\"error\":\"temporarily_unavailable\"}"), successResponse());

        assertThrows(ExecutionException.class, () -> service().getAccessToken().get(5, TimeUnit.SECONDS));
        assertEquals(new BearerAccessToken("access"), service().getAccessToken().get(5, TimeUnit.SECONDS).orElseThrow());
        assertEquals(Optional.empty(), Optional.ofNullable(openedPage.get()));
    }

    @Test
    void invalidGrantClearsStoredLoginAndStartsInteractiveLogin() throws Exception {
        // [utest->req~citedrive.refresh.failure~1]
        preferences.setRefreshToken(new RefreshToken("expired"));
        when(sender.send(any())).thenReturn(response(400, "{\"error\":\"invalid_grant\"}"));
        CompletableFuture<URI> browserOpened = new CompletableFuture<>();
        CiteDriveOAuthService service = new CiteDriveOAuthService(remotePreferences, preferences, registry, browserOpened::complete, () -> false, sender);

        CompletableFuture<?> login = service.getAccessToken();
        AuthorizationRequest request = AuthorizationRequest.parse(browserOpened.get(5, TimeUnit.SECONDS));

        assertEquals(Optional.empty(), Optional.ofNullable(preferences.getRefreshToken()));
        registry.fail(request.getState().getValue(), new IOException("Login cancelled by test"));
        assertThrows(ExecutionException.class, () -> login.get(5, TimeUnit.SECONDS));
    }

    @Test
    void concurrentServicesShareOneRefreshAndStoreRotatedToken() throws Exception {
        // [utest->req~citedrive.refresh.single-request~1]
        preferences.setRefreshToken(new RefreshToken("stored"));
        CompletableFuture<HTTPRequest> received = new CompletableFuture<>();
        CompletableFuture<HTTPResponse> reply = new CompletableFuture<>();
        AtomicInteger requests = new AtomicInteger();
        when(sender.send(any())).thenAnswer(invocation -> {
            requests.incrementAndGet();
            received.complete(invocation.getArgument(0));
            return reply.join();
        });

        try {
            var first = service().getAuthorizedSession();
            received.get(5, TimeUnit.SECONDS);
            var second = service().getAuthorizedSession();
            reply.complete(successResponse());

            assertEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            assertEquals(1, requests.get());
            assertEquals(new RefreshToken("rotated"), preferences.getRefreshToken());
        } finally {
            reply.complete(successResponse());
        }
    }

    @Test
    void serverChangeUpdatesExistingServiceAndDiscardsOldCredential() throws Exception {
        // [utest->req~citedrive.credentials.server~1]
        preferences.setRefreshToken(new RefreshToken("old-server-token"));
        CiteDriveOAuthService service = service();
        preferences.setApiBaseUrl("https://other.example/");
        when(sender.send(any())).thenReturn(successResponse());

        var login = service.getAuthorizedSession();
        AuthorizationRequest request = AuthorizationRequest.parse(openedPage.get());
        assertEquals(URI.create("https://other.example/jabref/login/"), request.getEndpointURI());
        assertEquals(Optional.empty(), Optional.ofNullable(preferences.getRefreshToken()));
        registry.complete(request.getState().getValue(), "code");

        var session = login.get(5, TimeUnit.SECONDS).orElseThrow();
        assertEquals(URI.create("https://other.example/jabref/push/"), session.pushEndpoint());
        ArgumentCaptor<HTTPRequest> tokenRequest = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(sender).send(tokenRequest.capture());
        assertEquals("https://other.example/o/token/", tokenRequest.getValue().getURL().toString());
    }

    @Test
    void serverChangeDuringRefreshCannotRestoreOldCredential() throws Exception {
        // [utest->req~citedrive.credentials.server~1]
        preferences.setRefreshToken(new RefreshToken("stored"));
        CompletableFuture<HTTPRequest> received = new CompletableFuture<>();
        CompletableFuture<HTTPResponse> reply = new CompletableFuture<>();
        when(sender.send(any())).thenAnswer(invocation -> {
            received.complete(invocation.getArgument(0));
            return reply.join();
        });

        try {
            var login = service().getAuthorizedSession();
            received.get(5, TimeUnit.SECONDS);
            preferences.setApiBaseUrl("https://other.example/");
            reply.complete(successResponse());

            assertThrows(ExecutionException.class, () -> login.get(5, TimeUnit.SECONDS));
            assertEquals(Optional.empty(), Optional.ofNullable(preferences.getRefreshToken()));
        } finally {
            reply.complete(successResponse());
        }
    }

    @Test
    void authorizedSessionKeepsItsOriginalUploadAddress() throws Exception {
        // [utest->req~citedrive.credentials.server~1]
        preferences.setRefreshToken(new RefreshToken("stored"));
        when(sender.send(any())).thenReturn(successResponse());
        var session = service().getAuthorizedSession().get(5, TimeUnit.SECONDS).orElseThrow();

        preferences.setApiBaseUrl("https://other.example/");
        preferences.setAppBaseUrl("https://other-app.example/");

        assertEquals(URI.create("https://api-dev.citedrive.com/jabref/push/"), session.pushEndpoint());
        assertEquals(URI.create("https://app-dev.citedrive.com/jabref/import/"), session.importPage());
    }

    @Test
    void insecureApiAddressFailsBeforeOpeningBrowserOrSendingToken() {
        // [utest->req~citedrive.https~1]
        preferences.setApiBaseUrl("http://other.example/");
        preferences.setRefreshToken(new RefreshToken("stored"));

        assertThrows(ExecutionException.class, () -> service().getAccessToken().get(5, TimeUnit.SECONDS));
        assertThrows(ExecutionException.class, () -> service().authorizeInteractive().get(5, TimeUnit.SECONDS));
        verifyNoInteractions(sender);
        assertEquals(Optional.empty(), Optional.ofNullable(openedPage.get()));
    }

    @Test
    void refreshUsesBoundedTimeoutsAndDisablesRedirects() throws Exception {
        // [utest->req~citedrive.token.timeout~1]
        preferences.setRefreshToken(new RefreshToken("stored"));
        when(sender.send(any())).thenReturn(successResponse());

        assertEquals(new BearerAccessToken("access"), service().getAccessToken().get(5, TimeUnit.SECONDS).orElseThrow());
        assertRequestTimeouts();
    }

    @Test
    void codeExchangeRunsOutsideCallbackThreadWithBoundedTimeouts() throws Exception {
        // [utest->req~citedrive.token.timeout~1]
        CompletableFuture<HTTPRequest> received = new CompletableFuture<>();
        CompletableFuture<HTTPResponse> reply = new CompletableFuture<>();
        when(sender.send(any())).thenAnswer(invocation -> {
            received.complete(invocation.getArgument(0));
            return reply.join();
        });
        try {
            var login = service().authorizeInteractive();
            AuthorizationRequest request = AuthorizationRequest.parse(openedPage.get());
            assertEquals(CodeChallengeMethod.S256, request.getCodeChallengeMethod());
            assertTrue(registry.complete(request.getState().getValue(), "code"));
            received.get(5, TimeUnit.SECONDS);
            assertFalse(login.isDone());
            reply.complete(successResponse());

            assertEquals(new BearerAccessToken("access"), login.get(5, TimeUnit.SECONDS).orElseThrow());
            assertRequestTimeouts();
        } finally {
            reply.complete(successResponse());
        }
    }

    private CiteDriveOAuthService service() {
        return new CiteDriveOAuthService(remotePreferences, preferences, registry, openedPage::set, () -> false, sender);
    }

    private void assertRequestTimeouts() throws IOException {
        ArgumentCaptor<HTTPRequest> request = ArgumentCaptor.forClass(HTTPRequest.class);
        verify(sender).send(request.capture());
        assertEquals(10_000, request.getValue().getConnectTimeout());
        assertEquals(30_000, request.getValue().getReadTimeout());
        assertFalse(request.getValue().getFollowRedirects());
    }

    private HTTPResponse successResponse() throws ParseException {
        return response(200, """
                {"access_token":"access","token_type":"Bearer","refresh_token":"rotated"}
                """);
    }

    private HTTPResponse response(int status, String body) throws ParseException {
        HTTPResponse response = new HTTPResponse(status);
        response.setContentType("application/json");
        response.setBody(body);
        return response;
    }
}
