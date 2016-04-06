/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.remote;

import net.sf.jabref.JabRef;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.remote.server.MessageHandler;

import java.util.Optional;
import java.util.Vector;

public class JabRefMessageHandler implements MessageHandler {

    private final JabRef jabRef;

    public JabRefMessageHandler(JabRef jabRef) {
        this.jabRef = jabRef;
    }

    @Override
    public void handleMessage(String message) {
        Optional<Vector<ParserResult>> loaded = jabRef.processArguments(message.split("\n"), false);
        if (!(loaded.isPresent())) {
            throw new IllegalStateException("Could not start JabRef with arguments " + message);
        }

        for (int i = 0; i < loaded.get().size(); i++) {
            ParserResult pr = loaded.get().elementAt(i);
            JabRef.mainFrame.addParserResult(pr, i == 0);
        }
    }
}
