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
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseButton;

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

    // Singleton pattern
    private static ClipBoardManager singleton;

    private final Clipboard clipboard;
    private final java.awt.datatransfer.Clipboard primarySelection;
    private final ImportFormatReader importFormatReader;

    // Private constructor (singleton)
    private ClipBoardManager() {
        clipboard = Clipboard.getSystemClipboard();
        primarySelection = Toolkit.getDefaultToolkit().getSystemSelection();
        importFormatReader = Globals.IMPORT_FORMAT_READER;
    }

    // Singleton pattern
    public synchronized static ClipBoardManager getInstance() {
        if (ClipBoardManager.singleton == null) {
            ClipBoardManager.singleton = new ClipBoardManager();
        }
        return ClipBoardManager.singleton;
    }

    // Override clone() method
    @Override
    public ClipBoardManager clone() {
        return null;
    }

    public void install(TextInputControl field) {
        field.selectedTextProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                setContentPrimary(newValue);
            }
        });
        field.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.MIDDLE) {
                field.insertText(field.getCaretPosition(), getContentsPrimary());
            }
        });
    }

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

    // X11 Primary Selection
    public String getContentsPrimary() {
        Transferable contents = primarySelection.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                LOGGER.warn(e.getMessage());
            }
        }
        return getContents();
    }

    /**
     * Puts content onto the clipboard.
     */
    public void setContent(ClipboardContent content) {
        clipboard.setContent(content);

        // Copy to X11 Primary Clipboard
        if (content.hasHtml()) {
            setContentPrimary(content.getHtml());
        } else if (content.hasRtf()) {
            setContentPrimary(content.getRtf());
        } else if (content.hasUrl()) {
            setContentPrimary(content.getUrl());
        } else if (content.hasString()) {
            setContentPrimary(content.getString());
        }
    }

    // X11 Primary Selection
    public void setContentPrimary(String string) {
        primarySelection.setContents(new StringSelection(string), null);
    }

    public void setHtmlContent(String html) {
        final ClipboardContent content = new ClipboardContent();
        content.putHtml(html);
        clipboard.setContent(content);
        setContentPrimary(html);
    }

    public void setContent(String string) {
        final ClipboardContent content = new ClipboardContent();
        content.putString(string);
        clipboard.setContent(content);
        setContentPrimary(string);
    }

    public void setContent(List<BibEntry> entries) throws IOException {
        final ClipboardContent content = new ClipboardContent();
        BibEntryWriter writer = new BibEntryWriter(new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences()), Globals.entryTypesManager);
        String serializedEntries = writer.serializeAll(entries, BibDatabaseMode.BIBTEX);
        content.put(DragAndDropDataFormats.ENTRIES, serializedEntries);
        content.putString(serializedEntries);
        setContentPrimary(serializedEntries);
        clipboard.setContent(content);
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
