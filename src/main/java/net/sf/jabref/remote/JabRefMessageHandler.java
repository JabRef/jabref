package net.sf.jabref.remote;

import net.sf.jabref.JabRef;
import net.sf.jabref.imports.ParserResult;
import net.sf.jabref.remote.server.MessageHandler;

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

        // put "bringToFront" in the queue
        // it has to happen before the call to import as the import might open a dialog
        // --> Globals.prefs.getBoolean(JabRefPreferences.USE_IMPORT_INSPECTION_DIALOG)
        // this dialog has to be shown AFTER JabRef has been brought to front
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                JabRef.jrf.showIfMinimizedToSysTray();
            }
        });

        for (int i = 0; i < loaded.size(); i++) {
            ParserResult pr = loaded.elementAt(i);
            JabRef.jrf.addParserResult(pr, (i == 0));
        }
    }
}
