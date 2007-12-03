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
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Util;
import net.sf.jabref.gui.ImportInspectionDialog;

import org.xml.sax.helpers.DefaultHandler;

/**
 * Fetcher for CiteSeer http://citeseer.ist.psu.edu/
 * 
 */
public class CiteSeerEntryFetcher implements EntryFetcher {

    final static String OAI_URL = "http://cs1.ist.psu.edu/cgi-bin/oai.cgi?verb=GetRecord&metadataPrefix=oai_citeseer&identifier=oai:CiteSeerPSU:";

    protected SAXParser saxParser;

    protected boolean stop;

    public void processQuery(String query, ImportInspectionDialog dialog, JabRefFrame frame) {

        stop = false;

        dialog.setVisible(true);

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
                JOptionPane.showMessageDialog(dialog, Globals.lang(
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
                JOptionPane.showMessageDialog(dialog, Globals
                    .lang("Error fetching from Citeseer:\n" + e.getLocalizedMessage()), Globals
                    .lang("Fetch Citeseer"), JOptionPane.ERROR_MESSAGE);
            }

            dialog.entryListComplete();
        }
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
        return "Fetch CiteSeer by ID";
    }

    public void cancelled() {

    }

    public void done(int entriesImported) {

    }

    public void stopFetching() {
        stop = true;
    }

}
