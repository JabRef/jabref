package org.jabref.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.jabref.websocket.handlers.HandlerCmdRegister;
import org.jabref.websocket.handlers.HandlerInfoGoogleScholarCitationCounts;
import org.jabref.websocket.handlers.HandlerInfoMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple, robust websocket server implementation for JabRef for bidirectional communication with arbitrarily many
 * websocket clients
 */
public class JabRefWebsocketServer extends WebSocketServer {
    // internals
    private static final int MAX_ONMESSAGE_CALLS_IN_PARALLEL = 500; // default: 500; 1: enables sequential processing
    private static final int DEFAULT_PORT = 8855;
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefWebsocketServer.class);
    private static final boolean SHOW_VERBOSE_DEBUG_OUTPUT = false;

    private static JabRefWebsocketServer jabRefWebsocketServerSingleton = null;

    private final Semaphore semaphoreWebSocketOnMessage = new Semaphore(MAX_ONMESSAGE_CALLS_IN_PARALLEL, true);

    private final Runnable heartbeatRunnable = () -> {
        if (SHOW_VERBOSE_DEBUG_OUTPUT) {
            LOGGER.debug("[ws] heartbeat thread is active...");
        }

        JsonObject messagePayload = new JsonObject();

        broadcastMessage(WebSocketAction.HEARTBEAT, messagePayload);
    };

    private volatile ScheduledExecutorService heartbeatExecutor = null;

    // server state
    private boolean serverStarting = false;
    private boolean serverStarted = false;

    // configuration (must be configured before starting the server)
    private int connectionLostTimeoutValue = 6; // [s] should be an even number, 0 ... disabled
    private boolean heartbeatEnabled = true;
    private TimeUnit timeUnitHeartbeatInterval = TimeUnit.SECONDS;
    private int heartbeatInterval = (int) timeUnitHeartbeatInterval.convert(connectionLostTimeoutValue, TimeUnit.SECONDS); // should be an even number
    private double heartbeatToleranceFactor = 0.5; // should be set to 0.5, since setConnectionLostTimeout() also uses this factor internally

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
                if (jabRefWebsocketServerSingleton != null && jabRefWebsocketServerSingleton.isServerStarted()) {
                    jabRefWebsocketServerSingleton.stopServer();
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
        } else {
            int activePort = jabRefWebsocketServerSingleton.getPort();

            if (activePort != port) {
                LOGGER.debug("[ws] JabRefWebsocketServer has already been started on a different port (" + activePort +
                        "), thus the given port (" + port + ") will not be used.");
            }
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

            if ("quit".equals(input)) {
                break;
            }

            JsonObject messagePayload = new JsonObject();
            messagePayload.addProperty("messageType", "info");
            messagePayload.addProperty("message", input);

            jabRefWebsocketServer.broadcastMessage(WebSocketAction.INFO_MESSAGE, messagePayload);
        }

        jabRefWebsocketServer.stopServer();
    }

    public int getConnectionLostTimeoutValue() {
        return connectionLostTimeoutValue;
    }

    public void setConnectionLostTimeoutValue(int connectionLostTimeoutValue) {
        this.connectionLostTimeoutValue = connectionLostTimeoutValue;
    }

    public boolean isHeartbeatEnabled() {
        return heartbeatEnabled;
    }

    public void setHeartbeatEnabled(boolean heartbeatEnabled) {
        this.heartbeatEnabled = heartbeatEnabled;
    }

    public TimeUnit getTimeUnitHeartbeatInterval() {
        return timeUnitHeartbeatInterval;
    }

    public void setTimeUnitHeartbeatInterval(TimeUnit timeUnitHeartbeatInterval) {
        this.timeUnitHeartbeatInterval = timeUnitHeartbeatInterval;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public double getHeartbeatToleranceFactor() {
        return heartbeatToleranceFactor;
    }

    public void setHeartbeatToleranceFactor(double heartbeatToleranceFactor) {
        this.heartbeatToleranceFactor = heartbeatToleranceFactor;
    }

    /**
     * Checks if at least one websocket client is registered, which matches the given <code>WebSocketClientType</code>.
     *
     * @param webSocketClientType webSocketClientType of the websocket clients to search for
     * @return <code>true</code>, if at least one websocket client with the given type is registered, or
     * <code>false</code> otherwise
     */
    public boolean isWebSocketClientWithGivenWebSocketClientTypeRegistered(WebSocketClientType webSocketClientType) {
        for (WebSocket websocket : getConnections()) {
            WebSocketClientData webSocketClientData = websocket.getAttachment();
            if (webSocketClientData != null && webSocketClientData.getWebSocketClientType().equals(webSocketClientType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the first websocket client, which matches the given <code>WebSocketClientType</code>.
     *
     * @param webSocketClientType webSocketClientType of the requested websocket client
     * @return the matching websocket client
     */
    private Optional<WebSocket> getFirstWebSocketClientByWebSocketClientType(WebSocketClientType webSocketClientType) {
        for (WebSocket websocket : getConnections()) {
            WebSocketClientData webSocketClientData = websocket.getAttachment();
            if (webSocketClientData != null && webSocketClientData.getWebSocketClientType().equals(webSocketClientType)) {
                return Optional.of(websocket);
            }
        }

        return Optional.empty();
    }

    /**
     * Checks if a websocket client is registered, which matches the given websocket's <code>uid</code>.
     *
     * @param webSocketUID webSocketUID of the websocket client to search for
     * @return <code>true</code>, if a websocket client with the given uid is registered, or <code>false</code>
     * otherwise
     */
    public boolean isWebSocketClientWithGivenWebSocketUIDRegistered(String webSocketUID) {
        for (WebSocket websocket : getConnections()) {
            WebSocketClientData webSocketClientData = websocket.getAttachment();
            if (webSocketClientData != null && webSocketClientData.getWebSocketUID().equals(webSocketUID)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the websocket client, which matches the given websocket's <code>UID</code>.
     *
     * @param webSocketUID webSocketUID of the requested websocket client
     * @return the matching websocket client
     */
    private Optional<WebSocket> getWebSocketClientByWebSocketUID(String webSocketUID) {
        for (WebSocket websocket : getConnections()) {
            WebSocketClientData webSocketClientData = websocket.getAttachment();
            if (webSocketClientData != null && webSocketClientData.getWebSocketUID().equals(webSocketUID)) {
                return Optional.of(websocket);
            }
        }

        return Optional.empty();
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
        }

        return false;
    }

    public boolean sendMessage(WebSocket websocketOfRecipient, WebSocketAction webSocketAction, JsonObject messagePayload) {
        JsonObject messageContainer = WebSocketServerUtils.createMessageContainer(webSocketAction, messagePayload);

        return sendJsonString(websocketOfRecipient, new Gson().toJson(messageContainer));
    }

    public boolean sendMessage(WebSocketClientType webSocketClientTypeOfRecipient, WebSocketAction webSocketAction, JsonObject messagePayload) {
        Optional<WebSocket> websocket = getFirstWebSocketClientByWebSocketClientType(webSocketClientTypeOfRecipient);

        if (websocket.isPresent()) {
            return sendMessage(websocket.get(), webSocketAction, messagePayload);
        }

        return false;
    }

    public boolean sendMessage(String webSocketUIDofRecipient, WebSocketAction webSocketAction, JsonObject messagePayload) {
        Optional<WebSocket> websocket = getWebSocketClientByWebSocketUID(webSocketUIDofRecipient);

        if (websocket.isPresent()) {
            return sendMessage(websocket.get(), webSocketAction, messagePayload);
        }

        return false;
    }

    public void broadcastMessage(WebSocketAction webSocketAction, JsonObject messagePayload) {
        for (WebSocket websocket : getConnections()) {
            sendMessage(websocket, webSocketAction, messagePayload);
        }
    }

    public boolean startServer() {
        if (serverStarting) {
            LOGGER.info("[ws] JabRefWebsocketServer is already starting");

            return false;
        } else if (serverStarted) {
            LOGGER.info("[ws] JabRefWebsocketServer has already been started");

            return false;
        } else {
            LOGGER.debug("[ws] JabRefWebsocketServer is starting up...");

            serverStarting = true;

            addShutdownHook();
            setConnectionLostTimeout(connectionLostTimeoutValue);
            start();

            return true;
        }
    }

    public boolean stopServer() {
        if (serverStarting) {
            LOGGER.info("[ws] JabRefWebsocketServer is currently starting up and cannot be stopped during this process");

            return false;
        } else if (serverStarted) {
            LOGGER.debug("[ws] stopping JabRefWebsocketServer...");

            if (heartbeatExecutor != null) {
                heartbeatExecutor.shutdown();
                this.heartbeatExecutor = null;
            }

            try {
                stop(1000);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }

            serverStarted = false;

            return true;
        } else {
            LOGGER.info("[ws] JabRefWebsocketServer is not started");

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
        LOGGER.debug("[ws] @onOpen: " + websocket.getRemoteSocketAddress().getAddress().getHostAddress() + " connected.");

        websocket.setAttachment(new WebSocketClientData(WebSocketClientType.UNKNOWN));

        JsonObject messagePayload = new JsonObject();
        messagePayload.addProperty("messageType", "info");
        messagePayload.addProperty("message", "welcome!");

        sendMessage(websocket, WebSocketAction.INFO_MESSAGE, messagePayload);

        JabRefWebsocketServer jabRefWebsocketServer = JabRefWebsocketServer.getInstance();

        messagePayload = new JsonObject();
        messagePayload.addProperty("connectionLostTimeout", jabRefWebsocketServer.getConnectionLostTimeoutValue() * 1000); // [ms]
        messagePayload.addProperty("heartbeatEnabled", jabRefWebsocketServer.isHeartbeatEnabled());
        messagePayload.addProperty("heartbeatInterval", (int) TimeUnit.MILLISECONDS.convert(jabRefWebsocketServer.getHeartbeatInterval(), jabRefWebsocketServer.getTimeUnitHeartbeatInterval())); // [ms]
        messagePayload.addProperty("heartbeatToleranceFactor", jabRefWebsocketServer.getHeartbeatToleranceFactor());

        sendMessage(websocket, WebSocketAction.INFO_CONFIGURATION, messagePayload);
    }

    @Override
    public void onClose(WebSocket websocket, int code, String reason, boolean remote) {
        LOGGER.debug("[ws] @onClose: " + websocket + " has disconnected.");
    }

    @Override
    public void onError(WebSocket websocket, Exception ex) {
        LOGGER.error("[ws] @onError: " + websocket + " has caused an error.");

        ex.printStackTrace();

        if (websocket != null) {
            // some errors like port binding failed, which may not be assignable to a specific websocket
        }
    }

    @Override
    public void onStart() {
        serverStarted = true;
        serverStarting = false;

        LOGGER.debug("[ws] JabRefWebsocketServer has started on port " + getPort() + ".");

        if (heartbeatEnabled) {
            heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
            heartbeatExecutor.scheduleAtFixedRate(heartbeatRunnable, 0, heartbeatInterval, timeUnitHeartbeatInterval);

            LOGGER.debug("[ws] heartbeat thread is enabled...");
        } else {
            LOGGER.debug("[ws] heartbeat thread is disabled...");
        }
    }

    @Override
    public void onMessage(WebSocket websocket, ByteBuffer message) {
        try {
            semaphoreWebSocketOnMessage.acquire();

            if (SHOW_VERBOSE_DEBUG_OUTPUT) {
                LOGGER.debug("[ws] @onMessage: " + websocket + ": " + message);
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            semaphoreWebSocketOnMessage.release();
        }
    }

    @Override
    public void onMessage(WebSocket websocket, String message) {
        try {
            semaphoreWebSocketOnMessage.acquire();

            if (SHOW_VERBOSE_DEBUG_OUTPUT) {
                LOGGER.debug("[ws] @onMessage: " + websocket + ": " + message);
            }

            JsonObject messageContainer = new Gson().fromJson(message, JsonObject.class);

            String action = messageContainer.get("action").getAsString();
            JsonObject messagePayload = messageContainer.getAsJsonObject("payload");

            if (!WebSocketAction.isValidWebSocketAction(action)) {
                LOGGER.warn("[ws] unknown WebSocketAction received: " + action);
                return;
            }

            Optional<WebSocketAction> webSocketAction = WebSocketAction.getWebSocketActionFromString(action);

            if (webSocketAction.isPresent()) {
                if (WebSocketAction.CMD_REGISTER.equals(webSocketAction.get())) {
                    HandlerCmdRegister.handler(websocket, messagePayload);
                } else if (WebSocketAction.INFO_MESSAGE.equals(webSocketAction.get())) {
                    HandlerInfoMessage.handler(websocket, messagePayload);
                } else if (WebSocketAction.INFO_GOOGLE_SCHOLAR_CITATION_COUNTS.equals(webSocketAction.get())) {
                    HandlerInfoGoogleScholarCitationCounts.handler(websocket, messagePayload);
                } else {
                    LOGGER.warn("[ws] unimplemented WebSocketAction received: " + action);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            semaphoreWebSocketOnMessage.release();
        }
    }
}
