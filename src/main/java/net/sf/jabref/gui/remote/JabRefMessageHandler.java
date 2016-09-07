package net.sf.jabref.gui.remote;

import java.util.List;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.cli.ArgumentProcessor;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.remote.server.MessageHandler;

public class JabRefMessageHandler implements MessageHandler {

    @Override
    public void handleMessage(String message) {
        ArgumentProcessor argumentProcessor = new ArgumentProcessor(message.split("\n"),
                ArgumentProcessor.Mode.REMOTE_START);
        if (!(argumentProcessor.hasParserResults())) {
            throw new IllegalStateException("Could not start JabRef with arguments " + message);
        }

        List<ParserResult> loaded = argumentProcessor.getParserResults();
        for (int i = 0; i < loaded.size(); i++) {
            ParserResult pr = loaded.get(i);
            JabRefGUI.getMainFrame().addParserResult(pr, i == 0);
        }
    }
}
