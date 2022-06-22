package org.jabref.gui.remote;

import java.util.List;

import javafx.application.Platform;

import org.jabref.cli.ArgumentProcessor;
import org.jabref.gui.JabRefGUI;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.remote.server.MessageHandler;
import org.jabref.preferences.PreferencesService;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefMessageHandler implements MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefMessageHandler.class);

    @Override
    public void handleCommandLineArguments(String[] message, PreferencesService preferencesService) {
        try {
            ArgumentProcessor argumentProcessor = new ArgumentProcessor(message, ArgumentProcessor.Mode.REMOTE_START, preferencesService);

            List<ParserResult> loaded = argumentProcessor.getParserResults();
            for (int i = 0; i < loaded.size(); i++) {
                ParserResult pr = loaded.get(i);
                boolean focusPanel = i == 0;
                Platform.runLater(() ->
                        // Need to run this on the JavaFX thread
                        JabRefGUI.getMainFrame().addParserResult(pr, focusPanel)
                );
            }
        } catch (ParseException e) {
            LOGGER.error("Error when parsing CLI args", e);
        }
    }
}
