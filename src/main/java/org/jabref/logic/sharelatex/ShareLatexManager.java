package org.jabref.logic.sharelatex;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.jabref.JabRefExecutorService;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.FileSaveSession;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.StringSaveSession;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.sharelatex.ShareLatexProject;
import org.jabref.model.util.FileUpdateMonitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ShareLatexManager {

    private static final Log LOGGER = LogFactory.getLog(ShareLatexManager.class);
    private static final SavePreferences preferences = new SavePreferences().withEncoding(StandardCharsets.UTF_8).withSaveInOriginalOrder(true);
    private final BibtexDatabaseWriter<StringSaveSession> stringdbWriter = new BibtexDatabaseWriter<>(StringSaveSession::new);
    private final BibtexDatabaseWriter<FileSaveSession> fileWriter = new BibtexDatabaseWriter<>(FileSaveSession::new);

    private final SharelatexConnector connector = new SharelatexConnector();
    private final ShareLatexParser parser = new ShareLatexParser();
    private SharelatexConnectionProperties properties;

    public String login(String server, String username, String password) throws IOException {
        return connector.connectToServer(server, username, password);
    }

    public List<ShareLatexProject> getProjects() throws IOException {
        if (connector.getProjects().isPresent()) {
            return parser.getProjectFromJson(connector.getProjects().get());
        }
        return Collections.emptyList();
    }

    public void startWebSocketHandler(String projectID, BibDatabaseContext database, ImportFormatPreferences preferences, FileUpdateMonitor fileMonitor) {
        JabRefExecutorService.INSTANCE.executeAndWait(() -> {

            try {
                connector.startWebsocketListener(projectID, database, preferences, fileMonitor);
            } catch (URISyntaxException e) {
                LOGGER.error(e);
            }
            registerListener(ShareLatexManager.this);

        });
    }

    public void sendNewDatabaseContent(BibDatabaseContext database) {
        try {
            fileWriter.saveDatabase(database, preferences);

            StringSaveSession saveSession = stringdbWriter.saveDatabase(database, preferences);
            String updatedcontent = saveSession.getStringValue().replace("\r\n", "\n");

            connector.sendNewDatabaseContent(updatedcontent);
        } catch (InterruptedException | SaveException e) {
            LOGGER.error("Could not prepare databse for saving ", e);
        }
    }

    public void registerListener(Object listener) {
        connector.registerListener(listener);
    }

    public void unregisterListener(Object listener) {
        connector.unregisterListener(listener);
    }

    public void disconnectAndCloseConnection() {
        connector.disconnectAndCloseConn();
    }

    public void setConnectionProperties(SharelatexConnectionProperties props) {
        this.properties = props;
    }

    public SharelatexConnectionProperties getConnectionProperties() {
        return this.properties;
    }
}
