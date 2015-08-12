/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui.remote;

import net.sf.jabref.JabRef;
import net.sf.jabref.imports.ParserResult;
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
            JabRef.jrf.addParserResult(pr, i == 0);
        }
    }
}
