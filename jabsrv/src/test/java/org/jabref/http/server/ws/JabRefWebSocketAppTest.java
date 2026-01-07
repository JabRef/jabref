package org.jabref.http.server.ws;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import org.jabref.logic.remote.server.RemoteMessageHandler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for JabRefWebSocketApp behavior.
 * This test uses a simple HK2 ServiceLocator to register a test RemoteMessageHandler
 * and invokes the message handling logic directly.
 */
public class JabRefWebSocketAppTest {

    private static class TestHandler implements RemoteMessageHandler {
        volatile String lastCommandArg;
        volatile boolean focused;

        @Override
        public void handleCommandLineArguments(String[] message) {
            if (message != null && message.length > 0) {
                lastCommandArg = String.join(" ", message);
            }
        }

        @Override
        public void handleFocus() {
            focused = true;
        }
    }

    @Test
    void testJsonAddCommandDelegatesToHandler() {
        ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        TestHandler handler = new TestHandler();
        ServiceLocatorUtilities.addOneConstant(locator, handler, "remoteMessageHandler", RemoteMessageHandler.class);

        JabRefWebSocketApp app = new JabRefWebSocketApp(locator);

        String addPayload = "{ \"command\": \"add\", \"argument\": \"@article{abc, title=\\\"T\\\"}\" }";
        String response = app.handleTextMessage(addPayload, handler);

        assertNotNull(response);
        assertTrue(response.contains("success"), "Response should indicate success");
        assertNotNull(handler.lastCommandArg);
        assertTrue(handler.lastCommandArg.contains("--importBibtex"));
    }

    @Test
    void testPingReturnsPong() {
        ServiceLocator locator = ServiceLocatorUtilities.createAndPopulateServiceLocator();
        JabRefWebSocketApp app = new JabRefWebSocketApp(locator);

        TestHandler handler = new TestHandler();
        String payload = "{ \"command\": \"ping\" }";
        String response = app.handleTextMessage(payload, handler);
        assertNotNull(response);
        assertTrue(response.contains("pong"));
    }
}
