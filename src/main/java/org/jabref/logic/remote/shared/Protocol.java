package org.jabref.logic.remote.shared;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javafx.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @implNote The first byte of every message identifies its type as a {@link RemoteMessage}.
 * Every message is terminated with '\0'.
 */
public class Protocol implements AutoCloseable {

    public static final String IDENTIFIER = "jabref";

    private static final Logger LOGGER = LoggerFactory.getLogger(Protocol.class);

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public Protocol(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public void sendMessage(RemoteMessage type) throws IOException {
        out.writeObject(type);
        out.writeObject(null);
        out.write('\0');
        out.flush();
    }

    public void sendMessage(RemoteMessage type, Object argument) throws IOException {
        out.writeObject(type);

        // encode the commandline arguments to handle special characters (eg. spaces and Chinese characters)
        // related to issue #6487
        if (type == RemoteMessage.SEND_COMMAND_LINE_ARGUMENTS) {
            String[] encodedArgs = ((String[]) argument).clone();
            for (int i = 0; i < encodedArgs.length; i++) {
                encodedArgs[i] = URLEncoder.encode(encodedArgs[i], StandardCharsets.UTF_8);
            }
            out.writeObject(encodedArgs);
        } else {
            out.writeObject(argument);
        }

        out.write('\0');
        out.flush();
    }

    public Pair<RemoteMessage, Object> receiveMessage() throws IOException {
        try {
            RemoteMessage type = (RemoteMessage) in.readObject();
            Object argument = in.readObject();
            int endOfMessage = in.read();

            // decode the received commandline arguments
            if (type == RemoteMessage.SEND_COMMAND_LINE_ARGUMENTS) {
                for (int i = 0; i < ((String[]) argument).length; i++) {
                    ((String[]) argument)[i] = URLDecoder.decode(((String[]) argument)[i], StandardCharsets.UTF_8);
                }
            }

            if (endOfMessage != '\0') {
                throw new IOException("Message didn't end on correct end of message identifier. Got " + endOfMessage);
            }

            return new Pair<>(type, argument);
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not deserialize message", e);
        }
    }

    @Override
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
