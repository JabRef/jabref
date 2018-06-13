package org.jabref.gui;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;

import org.jabref.Globals;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ImportFormatReader.UnknownFormatImport;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.util.OptionalUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClipBoardManager {

    public static final DataFormat XML = new DataFormat("application/xml");
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClipBoardManager.class);

    private final Clipboard clipboard;

    private final ImportFormatReader importFormatReader;

    public ClipBoardManager() {
        this(Clipboard.getSystemClipboard(), Globals.IMPORT_FORMAT_READER);
    }

    public ClipBoardManager(Clipboard clipboard, ImportFormatReader importFormatReader) {
        this.clipboard = clipboard;
        this.importFormatReader = importFormatReader;
    }

    /**
     * Puts content onto the clipboard.
     */
    public void setContent(ClipboardContent content) {
        clipboard.setContent(content);
    }

    /**
     * Get the String residing on the clipboard.
     *
     * @return any text found on the Clipboard; if none found, return an
     * empty String.
     */
    public String getContents() {
        String result = clipboard.getString();
        if (result == null) {
            return "";
        } else {
            return result;
        }
    }

    public void setHtmlContent(String html) {
        final ClipboardContent content = new ClipboardContent();
        content.putHtml(html);
        clipboard.setContent(content);
    }

    public void setContent(String string) {
        final ClipboardContent content = new ClipboardContent();
        content.putString(string);
        clipboard.setContent(content);
    }

    public void setContent(List<BibEntry> entries) throws IOException {
        final ClipboardContent content = new ClipboardContent();
        BibEntryWriter writer = new BibEntryWriter(new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences()), false);
        String serializedEntries = writer.serializeAll(entries, BibDatabaseMode.BIBTEX);
        content.put(DragAndDropDataFormats.ENTRIES, serializedEntries);
        content.putString(serializedEntries);
        clipboard.setContent(content);
    }

    public List<BibEntry> extractEntries() {
        Object entries = clipboard.getContent(DragAndDropDataFormats.ENTRIES);

        BibtexParser parser = new BibtexParser(Globals.prefs.getImportFormatPreferences(), Globals.getFileUpdateMonitor());
        if (entries != null) {
            // We have determined that the clipboard data is a set of entries (serialized as a string).
            try {
                return parser.parseEntries((String) entries);
            } catch (ParseException ex) {
                LOGGER.error("Could not paste", ex);
            }
        } else {
            String data = clipboard.getString();
            if (data != null) {
                try {
                    // fetch from doi
                    Optional<DOI> doi = DOI.parse(data);
                    if (doi.isPresent()) {
                        LOGGER.info("Found DOI in clipboard");
                        Optional<BibEntry> entry = new DoiFetcher(Globals.prefs.getImportFormatPreferences()).performSearchById(doi.get().getDOI());
                        return OptionalUtil.toList(entry);
                    } else {
                        try {
                            UnknownFormatImport unknownFormatImport = importFormatReader.importUnknownFormat(data);
                            return unknownFormatImport.parserResult.getDatabase().getEntries();
                        } catch (ImportException e) {
                            // import failed and result will be empty
                        }
                    }
                } catch (FetcherException ex) {
                    LOGGER.error("Error while fetching", ex);
                }
            }
        }
        return Collections.emptyList();
    }
}
