package org.jabref.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.TextInputControl;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

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

// Singleton with enum
public enum ClipBoardManager {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClipBoardManager.class);

    private final Clipboard clipboard = Clipboard.getSystemClipboard();
    private final java.awt.datatransfer.Clipboard primary = Toolkit.getDefaultToolkit().getSystemSelection();
    private final ImportFormatReader importFormatReader = Globals.IMPORT_FORMAT_READER;

    ClipBoardManager() {
    }

    public void watchInput(String newValue) {
        if (!newValue.isEmpty()) {
            primary.setContents(new StringSelection(newValue), null);
        }
    }

    public void inputToPrimary(TextInputControl input) {
        input.insertText(input.getCaretPosition(), getContentsPrimary());
    }

    // For activating this functionality in an input, use:
    //
    // input.selectedTextProperty().addListener((observable, oldValue, newValue) -> watchInput(newValue));
    // input.setOnMouseClicked(event -> {
    //     if (event.getButton() == MouseButton.MIDDLE) {
    //         inputToPrimary(input);
    //     }
    // });

    /**
     * Get the String residing on the clipboard.
     *
     * @return any text found on the Clipboard; if none found, return an empty String.
     */
    public String getContents() {
        String result = clipboard.getString();
        if (result == null) {
            return "";
        }
        return result;
    }

    // Get the text from Primary
    public String getContentsPrimary() {
        Transferable contents = primary.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                LOGGER.warn(e.getMessage());
            }
        }
        return getContents();
    }

    // Copy from Clipboard to Primary
    private void clipboardToPrimary(ClipboardContent content) {
        primary.setContents(new StringSelection(content.getString()), null);
    }

    /**
     * Puts content onto the clipboard.
     */
    public void setContent(ClipboardContent content) {
        clipboard.setContent(content);
        clipboardToPrimary(content);
    }

    public void setHtmlContent(String html) {
        final ClipboardContent content = new ClipboardContent();
        content.putHtml(html);
        clipboard.setContent(content);
        clipboardToPrimary(content);
    }

    public void setContent(String string) {
        final ClipboardContent content = new ClipboardContent();
        content.putString(string);
        clipboard.setContent(content);
        clipboardToPrimary(content);
    }

    public void setContent(List<BibEntry> entries) throws IOException {
        final ClipboardContent content = new ClipboardContent();
        BibEntryWriter writer = new BibEntryWriter(new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences()), Globals.entryTypesManager);
        String serializedEntries = writer.serializeAll(entries, BibDatabaseMode.BIBTEX);
        content.put(DragAndDropDataFormats.ENTRIES, serializedEntries);
        content.putString(serializedEntries);
        clipboard.setContent(content);
        clipboardToPrimary(content);
    }

    public List<BibEntry> extractData() {
        Object entries = clipboard.getContent(DragAndDropDataFormats.ENTRIES);

        if (entries == null) {
            return handleStringData(clipboard.getString());
        }
        return handleBibTeXData((String) entries);
    }

    private List<BibEntry> handleBibTeXData(String entries) {
        BibtexParser parser = new BibtexParser(Globals.prefs.getImportFormatPreferences(), Globals.getFileUpdateMonitor());
        try {
            return parser.parseEntries(new ByteArrayInputStream(entries.getBytes(StandardCharsets.UTF_8)));
        } catch (ParseException ex) {
            LOGGER.error("Could not paste", ex);
            return Collections.emptyList();
        }
    }

    private List<BibEntry> handleStringData(String data) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }

        Optional<DOI> doi = DOI.parse(data);
        if (doi.isPresent()) {
            return fetchByDOI(doi.get());
        }

        return tryImportFormats(data);
    }

    private List<BibEntry> tryImportFormats(String data) {
        try {
            UnknownFormatImport unknownFormatImport = importFormatReader.importUnknownFormat(data);
            return unknownFormatImport.parserResult.getDatabase().getEntries();
        } catch (ImportException ignored) {
            return Collections.emptyList();
        }
    }

    private List<BibEntry> fetchByDOI(DOI doi) {
        LOGGER.info("Found DOI in clipboard");
        try {
            Optional<BibEntry> entry = new DoiFetcher(Globals.prefs.getImportFormatPreferences()).performSearchById(doi.getDOI());
            return OptionalUtil.toList(entry);
        } catch (FetcherException ex) {
            LOGGER.error("Error while fetching", ex);
            return Collections.emptyList();
        }
    }
}
