/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.imports;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;
import net.sf.jabref.Util;

import org.xml.sax.helpers.DefaultHandler;

/**
 * Fetcher for CiteSeer http://citeseer.ist.psu.edu/
 * 
 */
public class CiteSeerEntryFetcher implements EntryFetcher {

    final static String OAI_URL = "http://cs1.ist.psu.edu/cgi-bin/oai.cgi?verb=GetRecord&metadataPrefix=oai_citeseer&identifier=oai:CiteSeerPSU:";

    protected SAXParser saxParser;

    protected boolean stop;

    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter frame) {

        stop = false;

        String[] ids = query.trim().split("[;,\\s]+");

        for (int i = 0; i < ids.length; i++) {

            if (stop)
                break;

            // Try to import based on the id:
            String id = ids[i];

            // Clean IDs
            id = id.replaceAll("(http://citeseer.ist.psu.edu/|\\.html|oai:CiteSeerPSU:)", "");

            // Can only fetch for numerical IDs
            if (!id.matches("^\\d+$")) {
            	frame.showMessage(Globals.lang(
                    "Citeseer only supports numerical ids, '%0' is invalid.\n"
                        + "See the help for further information.", id), Globals
                    .lang("Fetch Citeseer"), JOptionPane.INFORMATION_MESSAGE);
                continue;
            }

            // Create an empty entry
            BibtexEntry entry = new BibtexEntry(Util.createNeutralId(), BibtexEntryType
                .getType("article"));
            entry.setField("citeseerurl", id);

            try {
                URL citeseerUrl = new URL(OAI_URL + id);
                HttpURLConnection citeseerConnection = (HttpURLConnection) citeseerUrl
                    .openConnection();
                InputStream inputStream = citeseerConnection.getInputStream();

                DefaultHandler handlerBase = new CiteSeerEntryFetcherHandler(entry);

                if (saxParser == null)
                    saxParser = SAXParserFactory.newInstance().newSAXParser();

                saxParser.parse(inputStream, handlerBase);

                /* Correct line breaks and spacing */
                for (String name : entry.getAllFields()) {
                    entry.setField(name, OAI2Fetcher.correctLineBreaks(entry.getField(name)
                        .toString()));
                }

                dialog.addEntry(entry);
                dialog.setProgress(i + 1, ids.length);
            } catch (Exception e) {
            	frame.showMessage(Globals
                    .lang("Error fetching from Citeseer:\n" + e.getLocalizedMessage()), Globals
                    .lang("Fetch Citeseer"), JOptionPane.ERROR_MESSAGE);
            }

            return true;
        }
        return false;
    }

    public String getHelpPage() {
        return GUIGlobals.citeSeerHelp;
    }

    public URL getIcon() {
        return GUIGlobals.getIconUrl("citeseer");
    }

    public String getKeyName() {
        return "Fetch CiteSeer";
    }

    public JPanel getOptionsPanel() {
        // No Options
        return null;
    }

    public String getTitle() {
        return Globals.menuTitle("Fetch CiteSeer by ID");
    }

    public void stopFetching() {
        stop = true;
    }

}
