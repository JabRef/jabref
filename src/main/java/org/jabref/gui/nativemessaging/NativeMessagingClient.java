package org.jabref.gui.nativemessaging;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.json.JSONObject;

public interface NativeMessagingClient {

    /**
     * Sends a message asynchronously, i.e. sends the message immediately but returns a future waiting for a response.
     * @param message the message to send
     * @return a future object that will contain the response
     */
    CompletableFuture<JSONObject> sendAsync(String message);

    /**
     * Adds a listener for push notifications
     * @param listener the listener for the push message (encoded as a JSON object)
     */
    void addPushListener(Consumer<JSONObject> listener);

    /**
     * Sends a message immediately without expecting a response
     * @param message the message to send
     */
    void send(String message) throws IOException;
}
