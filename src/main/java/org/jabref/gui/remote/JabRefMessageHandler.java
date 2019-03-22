package org.jabref.gui.remote;

import java.util.List;

import javafx.application.Platform;

import org.jabref.JabRefGUI;
import org.jabref.cli.ArgumentProcessor;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.remote.server.MessageHandler;

public class JabRefMessageHandler implements MessageHandler {

    @Override
    public void handleCommandLineArguments(String[] message) {
        ArgumentProcessor argumentProcessor = new ArgumentProcessor(message, ArgumentProcessor.Mode.REMOTE_START);

        List<ParserResult> loaded = argumentProcessor.getParserResults();
        for (int i = 0; i < loaded.size(); i++) {
            ParserResult pr = loaded.get(i);
            boolean focusPanel = i == 0;
            Platform.runLater(() ->
                    // Need to run this on the JavaFX thread
                    JabRefGUI.getMainFrame().addParserResult(pr, focusPanel)
            );
        }
    }
}
