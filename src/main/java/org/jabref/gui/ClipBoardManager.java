package org.jabref.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.Logger;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;

public class ClipBoardManager implements ClipboardOwner {

    private static final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();

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
        CLIPBOARD.setContents(transferable, this);
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
        Transferable contents = CLIPBOARD.getContents(null);
        if ((contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                //highly unlikely since we are using a standard DataFlavor
                Logger.info(this, "problem with getting clipboard contents", e);
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
        CLIPBOARD.setContents(stringSelection, this);
    }

    public List<BibEntry> extractBibEntriesFromClipboard() {
        // Get clipboard contents, and see if TransferableBibtexEntry is among the content flavors offered
        Transferable content = CLIPBOARD.getContents(null);
        List<BibEntry> result = new ArrayList<>();

        if (content.isDataFlavorSupported(TransferableBibtexEntry.ENTRY_FLAVOR)) {
            // We have determined that the clipboard data is a set of entries.
            try  {
                @SuppressWarnings("unchecked")
                List<BibEntry> contents = (List<BibEntry>) content.getTransferData(TransferableBibtexEntry.ENTRY_FLAVOR);
                result = contents;
            } catch (UnsupportedFlavorException | ClassCastException ex) {
                Logger.warn(this, "Could not paste this type", ex);
            } catch (IOException ex) {
                Logger.warn(this, "Could not paste", ex);
            }
        } else if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String data = (String) content.getTransferData(DataFlavor.stringFlavor);
                // fetch from doi
                if (DOI.parse(data).isPresent()) {
                    Logger.info(this, "Found DOI in clipboard");
                    Optional<BibEntry> entry = new DoiFetcher(Globals.prefs.getImportFormatPreferences()).performSearchById(new DOI(data).getDOI());
                    entry.ifPresent(result::add);
                } else {
                    // parse bibtex string
                    BibtexParser bp = new BibtexParser(Globals.prefs.getImportFormatPreferences());
                    BibDatabase db = bp.parse(new StringReader(data)).getDatabase();
                    Logger.info(this, "Parsed " + db.getEntryCount() + " entries from clipboard text");
                    if (db.hasEntries()) {
                        result = db.getEntries();
                    }
                }
            } catch (UnsupportedFlavorException ex) {
                Logger.warn(this, "Could not parse this type", ex);
            } catch (IOException ex) {
                Logger.warn(this, "Data is no longer available in the requested flavor", ex);
            } catch (FetcherException ex) {
                Logger.error(this, "Error while fetching", ex);
            }

        }
        return result;
    }
}
