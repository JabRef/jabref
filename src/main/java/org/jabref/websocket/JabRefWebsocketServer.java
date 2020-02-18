package org.jabref.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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

    private static JabRefWebsocketServer jabRefWebsocketServerSingleton = null;

    private static final int DEFAULT_PORT = 8855;
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefWebsocketServer.class);

    private final Runnable heartbeatRunnable = () -> {
        System.out.println("[ws] heartbeat thread is active ...");

        JsonObject messagePayload = new JsonObject();

        broadcastMessage(WsAction.HEARTBEAT, messagePayload);
    };

    private final Object SYNC_OBJECT = new Object();

    private boolean serverStarting = false;
    private boolean serverStarted = false;

    // configuration
    private boolean enableHeartbeat = true;
    private int heartbeatInterval = 5;
    private TimeUnit timeUnitHeartbeatInterval = TimeUnit.SECONDS;
    private volatile ScheduledExecutorService heartbeatExecutor = null;

    /**
     * [s] 0 ... disabled
     */
    private int connectionLostTimeout = 5;

    private JabRefWebsocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    private JabRefWebsocketServer(InetSocketAddress address) {
        super(address);
    }

    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            /**
             * run() is invoked, when JabRef gets terminated.
             */
            public void run() {
                if (jabRefWebsocketServerSingleton != null) {
                    if (jabRefWebsocketServerSingleton.isServerStarted()) {
                        jabRefWebsocketServerSingleton.stopServer();
                    }
                }
            }
        });
    }

    public static synchronized JabRefWebsocketServer getInstance() {
        if (jabRefWebsocketServerSingleton == null) {
            jabRefWebsocketServerSingleton = new JabRefWebsocketServer(DEFAULT_PORT);
        }
        return jabRefWebsocketServerSingleton;
    }

    public static synchronized JabRefWebsocketServer getInstance(int port) {
        if (jabRefWebsocketServerSingleton == null) {
            jabRefWebsocketServerSingleton = new JabRefWebsocketServer(port);
        }
        return jabRefWebsocketServerSingleton;
    }

    public static boolean isJabRefWebsocketServerInstantiated() {
        return jabRefWebsocketServerSingleton != null;
    }

    public static void main(String[] args) throws IOException {
        JabRefWebsocketServer jabRefWebsocketServer;

        if (JabRefWebsocketServer.isJabRefWebsocketServerInstantiated()) {
            jabRefWebsocketServer = JabRefWebsocketServer.getInstance();
        } else {
            int port = DEFAULT_PORT;
            try {
                port = Integer.parseInt(args[0]);
            } catch (Exception ignored) {
            }

            jabRefWebsocketServer = JabRefWebsocketServer.getInstance(port);
        }

        if (!jabRefWebsocketServer.isServerStarting() && !jabRefWebsocketServer.isServerStarted()) {
            jabRefWebsocketServer.startServer();
        }

        BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            String input = systemIn.readLine();

            if (input.equals("quit")) {
                break;
            }

            JsonObject messagePayload = new JsonObject();
            messagePayload.addProperty("messageType", "info");
            messagePayload.addProperty("message", input);

            jabRefWebsocketServer.broadcastMessage(WsAction.INFO_MESSAGE, messagePayload);
        }

        jabRefWebsocketServer.stopServer();
    }

    /**
     * Gets the first websocket client, which matches the given <code>WsClientType</code>.
     *
     * @param wsClientType wsClientType of the requested websocket client
     * @return the matching websocket client, or <code>null</code> otherwise
     */
    private WebSocket getFirstWsClientByWsClientType(WsClientType wsClientType) {
        for (WebSocket websocket : getConnections()) {
            WsClientData wsClientData = websocket.getAttachment();
            if (wsClientData != null && wsClientData.getWsClientType().equals(wsClientType)) {
                return websocket;
            }
        }
        return null;
    }

    /**
     * Gets the websocket client, which matches the given websocket's <code>uid</code>.
     *
     * @param wsUid wsUid of the requested websocket client
     * @return the matching websocket client, or <code>null</code> otherwise
     */
    private WebSocket getWsClientByWsUid(String wsUid) {
        for (WebSocket websocket : getConnections()) {
            WsClientData wsClientData = websocket.getAttachment();
            if (wsClientData != null && wsClientData.getWsUID().equals(wsUid)) {
                return websocket;
            }
        }
        return null;
    }

    private boolean sendJsonString(WebSocket websocketOfRecipient, String jsonString) {
        if (!serverStarted) {
            return false;
        }

        if (websocketOfRecipient == null || jsonString == null) {
            return false;
        }

        if (websocketOfRecipient.isOpen()) {
            websocketOfRecipient.send(jsonString);
            return true;
        } else {
            return false;
        }
    }

    public boolean sendMessage(WebSocket websocketOfRecipient, WsAction wsAction, JsonObject messagePayload) {
        JsonObject messageContainer = WsServerUtils.createMessageContainer(wsAction, messagePayload);
        return sendJsonString(websocketOfRecipient, new Gson().toJson(messageContainer));
    }

    public boolean sendMessage(WsClientType wsClientTypeOfRecipient, WsAction wsAction, JsonObject messagePayload) {
        WebSocket websocket = getFirstWsClientByWsClientType(wsClientTypeOfRecipient);
        if (websocket != null) {
            return sendMessage(websocket, wsAction, messagePayload);
        }
        return false;
    }

    public boolean sendMessage(String wsUIDofRecipient, WsAction wsAction, JsonObject messagePayload) {
        WebSocket websocket = getWsClientByWsUid(wsUIDofRecipient);
        if (websocket != null) {
            return sendMessage(websocket, wsAction, messagePayload);
        }
        return false;
    }

    public void broadcastMessage(WsAction wsAction, JsonObject messagePayload) {
        for (WebSocket websocket : getConnections()) {
            sendMessage(websocket, wsAction, messagePayload);
        }
    }

    public boolean startServer() {
        if (serverStarting) {
            System.out.println("[ws] JabRefWebsocketServer is already starting");
            return false;
        }
        if (serverStarted) {
            System.out.println("[ws] JabRefWebsocketServer has already been started");
            return false;
        } else {
            System.out.println("[ws] JabRefWebsocketServer is starting up...");

            serverStarting = true;

            addShutdownHook();
            start();

            return true;
        }
    }

    public boolean stopServer() {
        if (serverStarting) {
            System.out.println("[ws] JabRefWebsocketServer is currently starting up and cannot be stopped during this process");
            return false;
        } else if (serverStarted) {
            System.out.println("[ws] stopping JabRefWebsocketServer...");

            if (heartbeatExecutor != null) {
                heartbeatExecutor.shutdown();
                this.heartbeatExecutor = null;
            }

            try {
                stop(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            serverStarted = false;

            return true;
        } else {
            System.out.println("[ws] JabRefWebsocketServer is not started");
            return false;
        }
    }

    public boolean isServerStarting() {
        return serverStarting;
    }

    public boolean isServerStarted() {
        return serverStarted;
    }

    @Override
    public void onOpen(WebSocket websocket, ClientHandshake handshake) {
        synchronized (SYNC_OBJECT) {
            System.out.println("[ws] @onOpen: " + websocket.getRemoteSocketAddress().getAddress().getHostAddress() + " connected.");

            websocket.setAttachment(new WsClientData(WsClientType.UNKNOWN));

            JsonObject messagePayload = new JsonObject();
            messagePayload.addProperty("messageType", "info");
            messagePayload.addProperty("message", "welcome!");

            sendMessage(websocket, WsAction.INFO_MESSAGE, messagePayload);
        }
    }

    @Override
    public void onClose(WebSocket websocket, int code, String reason, boolean remote) {
        synchronized (SYNC_OBJECT) {
            System.out.println("[ws] @onClose: " + websocket + " has disconnected.");
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
        serverStarted = true;
        serverStarting = false;

        System.out.println("[ws] JabRefWebsocketServer has started on port " + getPort() + ".");

        setConnectionLostTimeout(connectionLostTimeout);

        if (enableHeartbeat) {
            heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

            heartbeatExecutor.scheduleAtFixedRate(heartbeatRunnable, 0, heartbeatInterval, timeUnitHeartbeatInterval);

            System.out.println("[ws] heartbeat thread is enabled...");
        } else {
            System.out.println("[ws] heartbeat thread is disabled...");
        }
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

            JsonObject messageContainer = new Gson().fromJson(message, JsonObject.class);

            String action = messageContainer.get("action").getAsString();
            JsonObject messagePayload = messageContainer.getAsJsonObject("payload");

            if (!WsAction.isValidWsAction(action)) {
                System.out.println("[ws] unknown WsAction received: " + action);
                return;
            }

            WsAction wsAction = WsAction.getWsActionFromString(action);

            if (WsAction.CMD_REGISTER.equals(wsAction)) {
                HandlerCmdRegister.handler(websocket, messagePayload);
            } else if (WsAction.INFO_MESSAGE.equals(wsAction)) {
                HandlerInfoMessage.handler(websocket, messagePayload);
            } else if (WsAction.INFO_GOOGLE_SCHOLAR_CITATION_COUNTS.equals(wsAction)) {
                HandlerInfoGoogleScholarCitationCounts.handler(websocket, messagePayload);
            } else if (WsAction.INFO_GOOGLE_SCHOLAR_SOLVING_CAPTCHA_NEEDED.equals(wsAction)) {
                HandlerInfoGoogleScholarSolvingCaptchaNeeded.handler(websocket, messagePayload);
            } else {
                System.out.println("[ws] unimplemented WsAction received: " + action);
            }
        }
    }
}
