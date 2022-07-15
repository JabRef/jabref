package org.jabref.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseButton;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllowedToUseAwt("Requires ava.awt.datatransfer.Clipboard")
public class ClipBoardManager {

    public static final DataFormat XML = new DataFormat("application/xml");

    private static final Logger LOGGER = LoggerFactory.getLogger(ClipBoardManager.class);

    private static Clipboard clipboard;
    private static java.awt.datatransfer.Clipboard primary;

    private final PreferencesService preferencesService;


    public ClipBoardManager(PreferencesService preferencesService) {
        this(Clipboard.getSystemClipboard(), Toolkit.getDefaultToolkit().getSystemSelection(), preferencesService);
    }

    public ClipBoardManager(Clipboard clipboard, java.awt.datatransfer.Clipboard primary, PreferencesService preferencesService) {
        ClipBoardManager.clipboard = clipboard;
        ClipBoardManager.primary = primary;
        this.preferencesService = preferencesService;
    }

    /**
     * Add X11 clipboard support to a text input control. It is necessary to call this method in every input where you
     * want to use it: {@code ClipBoardManager.addX11Support(TextInputControl input);}.
     *
     * @param input the TextInputControl (e.g., TextField, TextArea, and children) where adding this functionality.
     * @see <a href="https://www.uninformativ.de/blog/postings/2017-04-02/0/POSTING-en.html">Short summary for X11
     * clipboards</a>
     * @see <a href="https://unix.stackexchange.com/questions/139191/whats-the-difference-between-primary-selection-and-clipboard-buffer/139193#139193">Longer
     * text over clipboards</a>
     */
    public static void addX11Support(TextInputControl input) {
        input.selectedTextProperty().addListener(
                // using InvalidationListener because of https://bugs.openjdk.java.net/browse/JDK-8176270
                observable -> Platform.runLater(() -> {
                    String newValue = input.getSelectedText();
                    if (!newValue.isEmpty() && (primary != null)) {
                        primary.setContents(new StringSelection(newValue), null);
                    }
                }));
        input.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.MIDDLE) {
                input.insertText(input.getCaretPosition(), getContentsPrimary());
            }
        });
    }

    /**
     * Get the String residing on the system clipboard.
     *
     * @return any text found on the Clipboard; if none found, return an empty String.
     */
    public static String getContents() {
        String result = clipboard.getString();
        if (result == null) {
            return "";
        }
        return result;
    }

    public Optional<String> getBibTeXEntriesFromClipbaord(){
        return Optional.ofNullable(clipboard.getContent(DragAndDropDataFormats.ENTRIES)).map(String.class::cast);
    }

    /**
     * Get the String residing on the primary clipboard (if it exists).
     *
     * @return any text found on the primary Clipboard; if none found, try with the system clipboard.
     */
    public static String getContentsPrimary() {
        if (primary != null) {
            Transferable contents = primary.getContents(null);
            if ((contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    return (String) contents.getTransferData(DataFlavor.stringFlavor);
                } catch (UnsupportedFlavorException | IOException e) {
                    LOGGER.warn(e.getMessage());
                }
            }
        }
        return getContents();
    }

    /**
     * Puts content onto the system clipboard.
     *
     * @param content the ClipboardContent to set as current value of the system clipboard.
     */
    public void setContent(ClipboardContent content) {
        clipboard.setContent(content);
        setPrimaryClipboardContent(content);
    }

    /**
     * Puts content onto the primary clipboard (if it exists).
     *
     * @param content the ClipboardContent to set as current value of the primary clipboard.
     */
    public void setPrimaryClipboardContent(ClipboardContent content) {
        if (primary != null) {
            primary.setContents(new StringSelection(content.getString()), null);
        }
    }

    public void setHtmlContent(String html, String fallbackPlain) {
        final ClipboardContent content = new ClipboardContent();
        content.putHtml(html);
        content.putString(fallbackPlain);
        clipboard.setContent(content);
        setPrimaryClipboardContent(content);
    }

    public void setContent(String string) {
        final ClipboardContent content = new ClipboardContent();
        content.putString(string);
        clipboard.setContent(content);
        setPrimaryClipboardContent(content);
    }

    public void setContent(List<BibEntry> entries) throws IOException {
        final ClipboardContent content = new ClipboardContent();
        BibEntryWriter writer = new BibEntryWriter(new FieldWriter(preferencesService.getFieldWriterPreferences()), Globals.entryTypesManager);
        String serializedEntries = writer.serializeAll(entries, BibDatabaseMode.BIBTEX);
        content.put(DragAndDropDataFormats.ENTRIES, serializedEntries);
        content.putString(serializedEntries);
        clipboard.setContent(content);
        setPrimaryClipboardContent(content);
    }
}
