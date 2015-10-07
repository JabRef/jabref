package net.sf.jabref.gui.remote;

import net.sf.jabref.JabRef;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.remote.server.MessageHandler;

import javax.swing.SwingUtilities;
import java.util.Vector;

public class JabRefMessageHandler implements MessageHandler {

    private final JabRef jabRef;

    public JabRefMessageHandler(JabRef jabRef) {
        this.jabRef = jabRef;
    }

    @Override
    public void handleMessage(String message) {
        Vector<ParserResult> loaded = jabRef.processArguments(message.split("\n"), false);
        if (loaded == null) {
            throw new IllegalStateException("Could not start JabRef with arguments " + message);
        }

        for (int i = 0; i < loaded.size(); i++) {
            ParserResult pr = loaded.elementAt(i);
            JabRef.jrf.addParserResult(pr, i == 0);
        }
    }
}
