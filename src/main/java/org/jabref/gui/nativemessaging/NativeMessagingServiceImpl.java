package org.jabref.gui.nativemessaging;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Future;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ParserResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

public class NativeMessagingServiceImpl implements NativeMessagingService {

    private static final Log LOGGER = LogFactory.getLog(NativeMessagingServiceImpl.class);

    private NativeMessagingClient client;

    public NativeMessagingServiceImpl(NativeMessagingClient client) {
        this.client = Objects.requireNonNull(client);
        this.client.addPushListener(this::handlePushMessage);
    }

    @Override
    public Future<Boolean> isOnline() {
        return client
                .sendAsync(new JSONObject().put("type", "ping").toString())
                .thenApply(response -> response.optString("type").equals("pong"));
    }

    private void handlePushMessage(JSONObject message) {
        if (message == null) {
            return;
        }
        
        String type = message.optString("type");
        try {
        switch (type) {
            case "ping":
                client.send(new JSONObject().put("type", "pong").toString());
                break;
            case "import":
                handleImportMessage(message.optString("format"), message.optString("data"));
                break;
            default:
                LOGGER.error("Unknown native message " + type);
        }
        } catch (IOException e) {
            LOGGER.error("Error while handling push message of type " + type, e);
        }
    }

    private void handleImportMessage(String importFormat, String data) {
        try {
            ParserResult result = Globals.IMPORT_FORMAT_READER.importFromFile(importFormat, data);
            JabRefGUI.getMainFrame().addParserResult(result, true);
        } catch (ImportException e) {
            LOGGER.error("Error while importing with " + importFormat, e);
        }
    }
}
