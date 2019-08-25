package org.jabref.logic.sharelatex;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.sharelatex.ShareLatexProject;
import org.jabref.model.util.FileUpdateMonitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ShareLatexManager {

    private static final Log LOGGER = LogFactory.getLog(ShareLatexManager.class);
    private final SavePreferences prefs;

    private final SharelatexConnector connector = new SharelatexConnector();
    private final ShareLatexParser parser = new ShareLatexParser();
    private SharelatexConnectionProperties properties;

    //TODO: FIXME needs to be udpated to the new methods
    public ShareLatexManager() {

        prefs = Globals.prefs.loadForSaveFromPreferences();

    }

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
            /*
            AtomicFileWriter fileWriter = new AtomicFileWriter(Paths.get(""), prefs.getEncoding());

            StringWriter strWriter = new StringWriter();
            BibtexDatabaseWriter stringdbWriter = new BibtexDatabaseWriter(strWriter, prefs, Globals.entryTypesManager)

                fileWriter.saveDatabase, prefs);

              stringdbWriter.saveDatabase(database);
            String updatedcontent = saveSession.getStringValue().replace("\r\n", "\n");
            */
            connector.sendNewDatabaseContent("");
        } catch (InterruptedException e) {
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
