package net.sf.jabref.gui.importer.fetcher;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import javax.swing.JOptionPane;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fetcher.ADSFetcher;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

public class ADSFetcherGUI extends ADSFetcher implements EntryFetcherGUI {

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        try {
            /* Remove "doi:" scheme identifier */
            /* Allow fetching only 1 key */
            String key = query.replaceAll("^(doi:|DOI:)", "");
            /* Query ADS and load the results into the BibDatabase */
            status.setStatus(Localization.lang("Processing %0", key));
            BibDatabase bd = importADSEntries(key, status);
            if ((bd != null) && bd.hasEntries()) {
                /* Add the entry to the inspection dialog */
                for (BibEntry entry : bd.getEntries()) {
                    importADSAbstract(key, entry, status);
                    dialog.addEntry(entry);
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            status.setStatus(Localization.lang("Error while fetching from %0", "ADS") + ": " + e.getMessage());
            getLogger().warn("Error while fetching from ADS", e);
        }
        return true;
    }

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_ADS;

    }

    private BibDatabase importADSEntries(String key, OutputPrinter status) {
        String url = constructUrl(key);
        try {
            URL ADSUrl = new URL(url + "&data_type=BIBTEX");
            HttpURLConnection ADSConnection = (HttpURLConnection) ADSUrl.openConnection();
            ADSConnection.setRequestProperty("User-Agent", "JabRef");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ADSConnection.getInputStream(), Charset.forName("ISO-8859-1")))) {
                ParserResult pr = BibtexParser.parse(reader);
                return pr.getDatabase();
            }
        } catch (FileNotFoundException e) {
            status.showMessage(
                    Localization.lang("'%0' is not a valid ADS bibcode.", key) + "\n\n" + Localization
                            .lang("Note: A full text search is currently not supported for %0", getTitle()),
                    getTitle(), JOptionPane.ERROR_MESSAGE);
            getLogger().debug("File not found", e);
        } catch (IOException e) {
            status.showMessage(Localization.lang("An Exception occurred while accessing '%0'", url) + "\n\n" + e,
                    getTitle(), JOptionPane.ERROR_MESSAGE);
            getLogger().debug("Problem accessing URL", e);
        } catch (RuntimeException e) {
            status.showMessage(
                    Localization.lang("An Error occurred while fetching from ADS (%0):", url) + "\n\n" + e.getMessage(),
                    getTitle(), JOptionPane.ERROR_MESSAGE);
            getLogger().warn("Problem fetching from ADS", e);
        }
        return null;
    }

    private void importADSAbstract(String key, BibEntry entry, OutputPrinter status) {
        /* TODO: construct ADSUrl from BibEntry */
        String url = constructUrl(key);
        try {
            URL ADSUrl = new URL(url + "&data_type=XML");
            HttpURLConnection ADSConnection = (HttpURLConnection) ADSUrl.openConnection();
            ADSConnection.setRequestProperty("User-Agent", "JabRef");
            BufferedInputStream bis = new BufferedInputStream(ADSConnection.getInputStream());

            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(bis);
            boolean isAbstract = false;
            StringBuilder abstractSB = new StringBuilder();
            while (reader.hasNext()) {
                reader.next();
                if (reader.isStartElement() && "abstract".equals(reader.getLocalName())) {
                    isAbstract = true;
                }
                if (isAbstract && reader.isCharacters()) {
                    abstractSB.append(reader.getText());
                }
                if (isAbstract && reader.isEndElement()) {
                    isAbstract = false;
                }
            }
            String abstractText = abstractSB.toString();
            abstractText = abstractText.replace("\n", " ");
            entry.setField("abstract", abstractText);
        } catch (XMLStreamException e) {
            status.showMessage(Localization.lang("An Error occurred while parsing abstract"), getTitle(),
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            status.showMessage(Localization.lang("An Exception occurred while accessing '%0'", url) + "\n\n" + e,
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) {
            status.showMessage(
                    Localization.lang("An Error occurred while fetching from ADS (%0):", url) + "\n\n" + e.getMessage(),
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }

}
