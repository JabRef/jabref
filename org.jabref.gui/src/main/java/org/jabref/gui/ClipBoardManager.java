package org.jabref.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ImportFormatReader.UnknownFormatImport;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClipBoardManager implements ClipboardOwner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClipBoardManager.class);

    private final Clipboard clipboard;

    private final ImportFormatReader importFormatReader;

    public ClipBoardManager() {
        this(Toolkit.getDefaultToolkit().getSystemClipboard(), Globals.IMPORT_FORMAT_READER);
    }

    public ClipBoardManager(Clipboard clipboard, ImportFormatReader importFormatReader) {
        this.clipboard = clipboard;
        this.importFormatReader = importFormatReader;
    }

    /**
     * Empty implementation of the ClipboardOwner interface.
     */
    @Override
    public void lostOwnership(Clipboard aClipboard, Transferable aContents) {
        //do nothing
    }

    /**
     * Places the string into the clipboard using a {@link Transferable}.
     */
    public void setTransferableClipboardContents(Transferable transferable) {
        clipboard.setContents(transferable, this);
    }

    /**
     * Get the String residing on the clipboard.
     *
     * @return any text found on the Clipboard; if none found, return an
     * empty String.
     */
    public String getClipboardContents() {
        String result = "";
        //odd: the Object param of getContents is not currently used
        Transferable contents = clipboard.getContents(null);
        if ((contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                //highly unlikely since we are using a standard DataFlavor
                LOGGER.info("problem with getting clipboard contents", e);
            }
        }
        return result;
    }

    /**
     * Place a String on the clipboard, and make this class the
     * owner of the Clipboard's contents.
     */
    public void setClipboardContents(String aString) {
        StringSelection stringSelection = new StringSelection(aString);
        clipboard.setContents(stringSelection, this);
    }

    public List<BibEntry> extractBibEntriesFromClipboard() {
        // Get clipboard contents, and see if TransferableBibtexEntry is among the content flavors offered
        Transferable content = clipboard.getContents(null);
        List<BibEntry> result = new ArrayList<>();

        if (content.isDataFlavorSupported(TransferableBibtexEntry.ENTRY_FLAVOR)) {
            // We have determined that the clipboard data is a set of entries.
            try {
                @SuppressWarnings("unchecked")
                List<BibEntry> contents = (List<BibEntry>) content.getTransferData(TransferableBibtexEntry.ENTRY_FLAVOR);
                result = contents;
            } catch (UnsupportedFlavorException | ClassCastException ex) {
                LOGGER.warn("Could not paste this type", ex);
            } catch (IOException ex) {
                LOGGER.warn("Could not paste", ex);
            }
        } else if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String data = (String) content.getTransferData(DataFlavor.stringFlavor);
                // fetch from doi
                if (DOI.parse(data).isPresent()) {
                    LOGGER.info("Found DOI in clipboard");
                    Optional<BibEntry> entry = new DoiFetcher(Globals.prefs.getImportFormatPreferences()).performSearchById(new DOI(data).getDOI());
                    entry.ifPresent(result::add);
                } else {
                    try {
                        UnknownFormatImport unknownFormatImport = importFormatReader.importUnknownFormat(data);
                        result = unknownFormatImport.parserResult.getDatabase().getEntries();
                    } catch (ImportException e) {
                        // import failed and result will be empty
                    }
                }
            } catch (UnsupportedFlavorException ex) {
                LOGGER.warn("Could not parse this type", ex);
            } catch (IOException ex) {
                LOGGER.warn("Data is no longer available in the requested flavor", ex);
            } catch (FetcherException ex) {
                LOGGER.error("Error while fetching", ex);
            }

        }
        return result;
    }
}
