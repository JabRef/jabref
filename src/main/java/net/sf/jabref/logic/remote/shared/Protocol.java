package net.sf.jabref.logic.remote.shared;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Every message is terminated with '\0'.
 */
public class Protocol {

    public static final String IDENTIFIER = "jabref";

    private static final Log LOGGER = LogFactory.getLog(Protocol.class);

    private final Socket socket;
    private final OutputStream out;
    private final InputStream in;


    public Protocol(Socket socket) throws IOException {
        this.socket = socket;
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();
    }

    public void sendMessage(String message) throws IOException {
        out.write(message.getBytes());
        out.write('\0');
        out.flush();
    }

    public String receiveMessage() throws IOException {
        int c;
        StringBuilder result = new StringBuilder();
        try {
            while (((c = in.read()) != '\0') && (c >= 0)) {
                result.append((char) c);
            }
        } catch (SocketTimeoutException ex) {
            LOGGER.info("Connection timed out.", ex);
        }
        return result.toString();
    }

    public void close() {
        try {
            in.close();
        } catch (IOException ignored) {
            // Ignored
        }

        try {
            out.close();
        } catch (IOException ignored) {
            // Ignored
        }

        try {
            socket.close();
        } catch (IOException ignored) {
            // Ignored
        }
    }
}
