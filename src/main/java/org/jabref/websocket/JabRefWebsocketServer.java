package org.jabref.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jabref.websocket.handlers.HandlerCmdRegister;
import org.jabref.websocket.handlers.HandlerInfoGoogleScholarCitationCounts;
import org.jabref.websocket.handlers.HandlerInfoGoogleScholarSolvingCaptchaNeeded;
import org.jabref.websocket.handlers.HandlerInfoMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple websocket server implementation for JabRef for bidirectional communication.
 */
public class JabRefWebsocketServer extends WebSocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefWebsocketServer.class);

    final Runnable heartbeatRunnable = () -> {
        System.out.println("[ws] heartbeat thread is active ...");

        JsonObject messagePayload = new JsonObject();

        broadcastMessage(WsServerUtils.createMessageContainer(WsAction.HEARTBEAT, messagePayload));
    };

    private final Object SYNC_OBJECT = new Object();

    private final List<WsClient> wsClients = new CopyOnWriteArrayList<>();

    private int heartbeatInterval = 5;
    private TimeUnit timeUnitHeartbeatInterval = TimeUnit.SECONDS;
    private boolean enableHeartbeat = true;
    private volatile ScheduledExecutorService heartbeatExecutor = null;

    /**
     * [s] 0 ... disabled
     */
    private int connectionLostTimeout = 5;

    /**
     * is <code>true</code>, if the the server is about to start or has started, <code>false</code> otherwise
     */
    private boolean serverStarted = false;

    public JabRefWebsocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    public JabRefWebsocketServer(InetSocketAddress address) {
        super(address);
    }

    public static void main(String[] args) throws IOException {
        int port = 8855;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception ignored) {
        }

        JabRefWebsocketServer jabRefWebsocketServer = new JabRefWebsocketServer(port);
        jabRefWebsocketServer.startServer();

        BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            String input = sysin.readLine();

            JsonObject messagePayload = new JsonObject();
            messagePayload.addProperty("message", input);

            if (input.equals("exit")) {
                jabRefWebsocketServer.stopServer();
                break;
            }

            jabRefWebsocketServer.broadcastMessage(WsServerUtils.createMessageContainer(WsAction.INFO_MESSAGE, messagePayload));
        }
    }

    /**
     * Registers a new <code>WsClient</code>, if no <code>WsClient</code> has already registered with the given <code>websocket</code> object.
     *
     * @return <code>true</code>, if the registration was successful, <code>false</code> otherwise
     */
    public boolean registerWsClient(WsClientType wsClientType, WebSocket websocket) {
        if (websocket == null) {
            return false;
        }

        for (WsClient wsClient : wsClients) {
            if (wsClient.getWebsocket().equals(websocket)) {
                return false;
            }
        }

        wsClients.add(new WsClient(wsClientType, websocket));

        return true;
    }

    /**
     * Deregisters an existing <code>WsClient</code>, which has the associated <code>websocket</code> object, if a registration exists.
     *
     * @param websocket
     * @return <code>true</code>, if a <code>WsClient</code> could be registered, <code>false</code> otherwise
     */
    private boolean deregisterWsClient(WebSocket websocket) {
        if (websocket == null) {
            return false;
        }

        for (WsClient wsClient : wsClients) {
            if (wsClient.getWebsocket().equals(websocket)) {
                wsClients.remove(wsClient);
                return true;
            }
        }

        return false;
    }

    /**
     * Get the first <code>WsClient</code> which matches the given <code>wsClientType</code>, prioritizing open connections.
     *
     * @param wsClientType
     * @return the matching WsClient, or <code>null</code> otherwise
     */
    private WsClient getFirstWsClient(WsClientType wsClientType) {
        WsClient wsClientCandiate = null;

        for (WsClient wsClient : wsClients) {
            if (wsClient.getWsClientType().equals(wsClientType)) {
                wsClientCandiate = wsClient;

                if (wsClientCandiate.getWebsocket().isOpen()) {
                    return wsClientCandiate; // immediately return wsClientCandidate, if the connection is open
                }
            }
        }

        return wsClientCandiate;
    }

    private boolean sendJsonString(WebSocket recipient, String jsonString) {
        if (recipient == null) {
            return false;
        }

        if (recipient.isOpen()) {
            recipient.send(jsonString);
            return true;
        } else {
            return false;
        }
    }

    public boolean sendMessage(WebSocket recipient, JsonObject messageContainer) {
        return sendJsonString(recipient, new Gson().toJson(messageContainer));
    }

    public boolean sendMessage(WsClient recipient, JsonObject messageContainer) {
        String jsonString = new Gson().toJson(messageContainer);
        return sendJsonString(recipient.getWebsocket(), jsonString);
    }

    public boolean sendMessage(WsClientType recipient, JsonObject messageContainer) {
        WsClient wsClient = getFirstWsClient(recipient);
        if (wsClient != null) {
            return sendMessage(wsClient, messageContainer);
        }
        return false;
    }

    public void broadcastMessage(JsonObject messageContainer) {
        for (WsClient wsClient : wsClients) {
            sendMessage(wsClient, messageContainer);
        }
    }

    @Override
    public void onOpen(WebSocket websocket, ClientHandshake handshake) {
        synchronized (SYNC_OBJECT) {
            System.out.println("[ws] @onOpen: " + websocket.getRemoteSocketAddress().getAddress().getHostAddress() + " connected.");

            JsonObject messagePayload = new JsonObject();
            messagePayload.addProperty("message", "Welcome!");

            sendMessage(websocket, WsServerUtils.createMessageContainer(WsAction.INFO_MESSAGE, messagePayload));
        }
    }

    @Override
    public void onClose(WebSocket websocket, int code, String reason, boolean remote) {
        synchronized (SYNC_OBJECT) {
            System.out.println("[ws] @onClose: " + websocket + " has disconnected.");
            deregisterWsClient(websocket);
        }
    }

    @Override
    public void onError(WebSocket websocket, Exception ex) {
        synchronized (SYNC_OBJECT) {
            System.out.println("[ws] @onError: " + websocket + " has caused an error.");
            ex.printStackTrace();
            if (websocket != null) {
                // some errors like port binding failed, which may not be assignable to a specific websocket
            }
        }
    }

    @Override
    public void onStart() {
        System.out.println("[ws] JabRefWebsocketServer started on port " + getPort() + ".");

        serverStarted = true;

        setConnectionLostTimeout(0);
        setConnectionLostTimeout(connectionLostTimeout);

        if (enableHeartbeat) {
            heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

            heartbeatExecutor.scheduleAtFixedRate(heartbeatRunnable, 0, heartbeatInterval, timeUnitHeartbeatInterval);

            System.out.println("[ws] heartbeat thread is enabled...");
        } else {
            System.out.println("[ws] heartbeat thread is disabled...");
        }
    }

    public boolean startServer() {
        if (serverStarted) {
            System.out.println("[ws] server has already been started");

            return false;
        } else {
            System.out.println("[ws] JabRefWebsocketServer is starting up...");
            serverStarted = true;
            start();

            return true;
        }
    }

    public void stopServer() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();

            this.heartbeatExecutor = null;
        }

        if (serverStarted) {
            System.out.println("[ws] stopping JabRefWebsocketServer...");

            serverStarted = false;
            try {
                stop(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("[ws] JabRefWebsocketServer is not started");
        }
    }

    public boolean isServerStarted() {
        return serverStarted;
    }

    @Override
    public void onMessage(WebSocket websocket, ByteBuffer message) {
        synchronized (SYNC_OBJECT) {
            System.out.println("[ws] @onMessage: " + websocket + ": " + message);
        }
    }

    @Override
    public void onMessage(WebSocket websocket, String message) {
        synchronized (SYNC_OBJECT) {
            System.out.println("[ws] @onMessage: " + websocket + ": " + message);

            JsonObject jsonMessage = new Gson().fromJson(message, JsonObject.class);

            String action = jsonMessage.get("action").getAsString();
            JsonObject messagePayload = jsonMessage.getAsJsonObject("payload");

            if (!WsAction.isValidWsAction(action)) {
                System.out.println("[ws] unknown WsAction received: " + action);

                return;
            }

            if (WsAction.CMD_REGISTER.equals(action)) {
                HandlerCmdRegister.handler(websocket, messagePayload, this);

                return;
            }

            if (WsAction.INFO_MESSAGE.equals(action)) {
                HandlerInfoMessage.handler(websocket, messagePayload);
            }
            else if (WsAction.INFO_GOOGLE_SCHOLAR_CITATION_COUNTS.equals(action)) {
                HandlerInfoGoogleScholarCitationCounts.handler(websocket, messagePayload);
            }
            else if (WsAction.INFO_GOOGLE_SCHOLAR_SOLVING_CAPTCHA_NEEDED.equals(action)) {
                HandlerInfoGoogleScholarSolvingCaptchaNeeded.handler(websocket, messagePayload);
            }
            else {
                System.out.println("[ws] unimplemented WsAction received: " + action);
            }
        }
    }
}
