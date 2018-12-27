package org.jabref.gui.remote;

import java.util.Arrays;
import java.util.List;

import org.jabref.JabRefGUI;
import org.jabref.cli.ArgumentProcessor;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.remote.server.MessageHandler;

public class JabRefMessageHandler implements MessageHandler {

    @Override
    public void handleCommandLineArguments(String[] message) {
        ArgumentProcessor argumentProcessor = new ArgumentProcessor(message, ArgumentProcessor.Mode.REMOTE_START);
        if (!(argumentProcessor.hasParserResults())) {
            throw new IllegalStateException("Could not start JabRef with arguments " + Arrays.toString(message));
        }

        List<ParserResult> loaded = argumentProcessor.getParserResults();
        for (int i = 0; i < loaded.size(); i++) {
            ParserResult pr = loaded.get(i);
            JabRefGUI.getMainFrame().addParserResult(pr, i == 0);
        }
    }
}
