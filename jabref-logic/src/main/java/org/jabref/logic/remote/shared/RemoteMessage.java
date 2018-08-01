package org.jabref.logic.remote.shared;

public enum RemoteMessage {
    /**
     * Send command line arguments. The message content is of type {@code String[]}.
     */
    SEND_COMMAND_LINE_ARGUMENTS,
    /**
     * As a response to {@link #PING}. The message content is an identifier of type {@code String}.
     */
    PONG,
    /**
     * Response signaling that the message was received successfully. No message content.
     */
    OK,
    /**
     * Request server to identify itself. No message content.
     */
    PING
}
