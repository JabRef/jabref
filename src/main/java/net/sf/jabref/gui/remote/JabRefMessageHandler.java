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

import java.util.List;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.cli.ArgumentProcessor;
import net.sf.jabref.importer.ParserResult;
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
