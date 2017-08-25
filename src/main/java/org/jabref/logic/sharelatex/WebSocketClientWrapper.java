package org.jabref.logic.sharelatex;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.Session;

import org.jabref.JabRefExecutorService;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.sharelatex.events.ShareLatexContinueMessageEvent;
import org.jabref.logic.sharelatex.events.ShareLatexEntryMessageEvent;
import org.jabref.logic.sharelatex.events.ShareLatexErrorMessageEvent;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.ext.extension.deflate.PerMessageDeflateExtension;

public class WebSocketClientWrapper {

    private static final Log LOGGER = LogFactory.getLog(WebSocketClientWrapper.class);
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final ShareLatexParser parser = new ShareLatexParser();

    private Session session;
    private String oldContent;
    private int version;
    private int commandCounter;
    private ImportFormatPreferences prefs;
    private String docId;
    private String projectId;
    private String databaseName;
    private final EventBus eventBus = new EventBus("SharelatexEventBus");
    private boolean leftDoc = false;
    private boolean errorReceived = false;

    private String serverOrigin;
    private Map<String, String> cookies;

    public WebSocketClientWrapper() {
        this.eventBus.register(this);
    }

    public void setImportFormatPrefs(ImportFormatPreferences prefs) {
        this.prefs = prefs;
    }

    public void createAndConnect(URI webSocketchannelUri, String projectId, BibDatabaseContext database) {

        try {
            this.projectId = projectId;

            ClientEndpointConfig.Configurator configurator = new MyCustomClientEndpointConfigurator(serverOrigin, cookies);
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().extensions(Arrays.asList(new PerMessageDeflateExtension()))
                    .configurator(configurator).build();
            final CountDownLatch messageLatch = new CountDownLatch(1);

            ClientManager client = ClientManager.createClient();
            client.getProperties().put(ClientProperties.REDIRECT_ENABLED, true);
            client.getProperties().put(ClientProperties.LOG_HTTP_UPGRADE, true);

            ClientManager.ReconnectHandler reconnectHandler = new ClientManager.ReconnectHandler() {

                private final AtomicInteger counter = new AtomicInteger(0);

                @Override
                public boolean onConnectFailure(Exception exception) {
                    final int i = counter.incrementAndGet();
                    if (i <= 3) {
                        LOGGER.debug(
                                "### Reconnecting... (reconnect count: " + i + ")", exception);
                        return true;
                    } else {
                        messageLatch.countDown();
                        return false;
                    }
                }

                @Override
                public long getDelay() {
                    return 0;
                }

            };
            client.getProperties().put(ClientProperties.RECONNECT_HANDLER, reconnectHandler);

            this.session = client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig config) {

                    session.addMessageHandler(String.class, (Whole<String>) message -> {
                        message = parser.fixUTF8Strings(message);
                        LOGGER.debug("Received new message " + message);
                        parseContents(message);
                    });
                }

                @Override
                public void onError(Session session, Throwable t) {
                    LOGGER.error(t);
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    if (errorReceived) {
                        LOGGER.debug("Error received in close session");
                    }

                }
            }, cec, webSocketchannelUri);

            //TODO: Change Dialog
            //TODO: On database change event or on save event send new version
            //TODO: When new db content arrived run merge dialog
            //TODO: Identfiy active database/Name of database/doc Id (partly done)

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void joinProject(String projectId) throws IOException {
        incrementCommandCounter();
        String text = "5:" + commandCounter + "+::{\"name\":\"joinProject\",\"args\":[{\"project_id\":\"" + projectId
                + "\"}]}";
        session.getBasicRemote().sendText(text);
    }

    public void joinDoc(String documentId) throws IOException {
        incrementCommandCounter();
        String text = "5:" + commandCounter + "+::{\"name\":\"joinDoc\",\"args\":[\"" + documentId + "\"]}";
        session.getBasicRemote().sendText(text);
    }

    public void leaveDocument(String documentId) throws IOException {
        incrementCommandCounter();
        String text = "5:" + commandCounter + "+::{\"name\":\"leaveDoc\",\"args\":[\"" + documentId + "\"]}";
        if (session != null) {
            session.getBasicRemote().sendText(text);
        }

    }

    private void sendHeartBeat() throws IOException {
        session.getBasicRemote().sendText("2::");
    }

    public void sendNewDatabaseContent(String newContent) throws InterruptedException {
        queue.put(newContent);
    }

    private void sendUpdateAsDeleteAndInsert(String docId, int position, int version, String oldContent, String newContent) throws IOException {
        ShareLatexJsonMessage message = new ShareLatexJsonMessage();

        List<SharelatexDoc> diffDocs = parser.generateDiffs(oldContent, newContent);
        String str = message.createUpdateMessageAsInsertOrDelete(docId, version, diffDocs);

        LOGGER.debug("Send new update Message");

        session.getBasicRemote().sendText("5:::" + str);
    }

    @Subscribe
    public synchronized void listenToSharelatexEntryMessage(ShareLatexContinueMessageEvent event) {

        JabRefExecutorService.INSTANCE.executeInterruptableTask(() -> {
            try {
                String updatedContent = queue.take();
                if (!leftDoc) {
                    LOGGER.debug("Taken from queue");
                    sendUpdateAsDeleteAndInsert(docId, 0, version, oldContent, updatedContent);

                }
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.debug("Exception in taking from queue", e);
            }
        });

    }

    //Actual response handling
    private void parseContents(String message) {
        try {

            System.out.println("Got message: " + message);
            if (message.contains(":::1")) {

                Thread.currentThread().sleep(300);
                LOGGER.debug("Got :::1. Joining project");

            }
            if (message.contains("2::")) {
                setLeftDoc(false);
                eventBus.post(new ShareLatexContinueMessageEvent());
                sendHeartBeat();

            }

            if (message.endsWith("[null]")) {
                LOGGER.debug("Received null-> Rejoining doc");
                joinDoc(docId);
            }

            if (message.startsWith("[null,{", message.indexOf("+") + 1)) {
                LOGGER.debug("We get a list with all files");
                //We get a list with all files

                String docIdOfFirstBibtex = parser.getFirstBibTexDatabaseId(message);

                LOGGER.debug("DBs with ID " + docIdOfFirstBibtex);
                setDocID(docIdOfFirstBibtex);
                joinDoc(docId);

            }
            if (message.contains("{\"name\":\"connectionAccepted\"}") && (projectId != null)) {

                LOGGER.debug("Joining project");
                Thread.sleep(200);
                joinProject(projectId);

            }

            if (message.contains("[null,[")) {
                System.out.println("Message could be an entry ");

                int version = parser.getVersionFromBibTexJsonString(message);
                setVersion(version);

                String bibtexString = parser.getBibTexStringFromJsonMessage(message);
                setBibTexString(bibtexString);
                List<BibEntry> entries = parser.parseBibEntryFromJsonMessageString(message, prefs);

                LOGGER.debug("Got new entries");
                setLeftDoc(false);

                eventBus.post(new ShareLatexEntryMessageEvent(entries, bibtexString));
                eventBus.post(new ShareLatexContinueMessageEvent());

            }

            if (message.contains("otUpdateApplied")) {
                LOGGER.debug("We got an update");

                leaveDocument(docId);
                setLeftDoc(true);
            }
            if (message.contains("otUpdateError")) {
                String error = parser.getOtErrorMessageContent(message);
                eventBus.post(new ShareLatexErrorMessageEvent(error));
            }
            if (message.contains("0::")) {
                leaveDocAndCloseConn();
            }

        } catch (IOException | ParseException e) {
            LOGGER.error("Error in parsing", e);
        } catch (InterruptedException e) {
            LOGGER.debug(e);
        }
    }

    public void setDatabaseName(String bibFileName) {
        this.databaseName = bibFileName;
    }

    public void leaveDocAndCloseConn() throws IOException {
        leaveDocument(docId);
        queue.clear();
        if (session != null) {
            session.close();
        }

    }

    public void setServerNameOrigin(String serverOrigin) {
        this.serverOrigin = serverOrigin;

    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;

    }

    public void registerListener(Object listener) {
        eventBus.register(listener);
    }

    public void unregisterListener(Object listener) {
        eventBus.unregister(listener);
    }

    private synchronized void setDocID(String docId) {
        this.docId = docId;
    }

    private synchronized void setVersion(int version) {
        this.version = version;
    }

    private synchronized void setBibTexString(String bibtex) {
        this.oldContent = bibtex;
    }

    private synchronized void incrementCommandCounter() {
        this.commandCounter = commandCounter + 1;
    }

    private synchronized void setLeftDoc(boolean leftDoc) {
        this.leftDoc = leftDoc;
    }

    private synchronized void setErrorReceived(boolean errorReceived) {
        this.errorReceived = errorReceived;
    }

}
