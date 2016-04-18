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
// created by : r.nagel 14.09.2004
//
// function : handle all clipboard action
//
// modified :

package net.sf.jabref.gui;

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

import net.sf.jabref.importer.fetcher.DOItoBibTeXFetcher;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ClipBoardManager implements ClipboardOwner {
    private static final Log LOGGER = LogFactory.getLog(ClipBoardManager.class);

    private static final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();

    /**
     * Empty implementation of the ClipboardOwner interface.
     */
    @Override
    public void lostOwnership(Clipboard aClipboard, Transferable aContents) {
        //do nothing
    }

    /**
     * Place a String on the clipboard, and make this class the
     * owner of the Clipboard's contents.
     */
    public void setClipboardContents(String aString) {
        StringSelection stringSelection = new StringSelection(aString);
        CLIPBOARD.setContents(stringSelection, this);
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
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                //highly unlikely since we are using a standard DataFlavor
                LOGGER.info("problem with getting clipboard contents", e);
            }
        }
        return result;
    }

    public List<BibEntry> extractBibEntriesFromClipboard() {
        // Get clipboard contents, and see if TransferableBibtexEntry is among the content flavors offered
        Transferable content = CLIPBOARD.getContents(null);

        List<BibEntry> result = new ArrayList<>();
        if (content.isDataFlavorSupported(TransferableBibtexEntry.entryFlavor)) {
            // We have determined that the clipboard data is a set of entries.
            try {
                result = (List<BibEntry>) content.getTransferData(TransferableBibtexEntry.entryFlavor);
            } catch (UnsupportedFlavorException | ClassCastException ex) {
                LOGGER.warn("Could not paste this type", ex);
            } catch (IOException ex) {
                LOGGER.warn("Could not paste", ex);
            }
        } else if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String data = (String) content.getTransferData(DataFlavor.stringFlavor);
                // fetch from doi
                if (DOI.build(data).isPresent()) {
                    LOGGER.info("Found DOI in clipboard");
                    BibEntry entry = new DOItoBibTeXFetcher().getEntryFromDOI(new DOI(data).getDOI(), null);
                    if (entry != null) {
                        result.add(entry);
                    }
                } else {
                    // parse bibtex string
                    BibtexParser bp = new BibtexParser(new StringReader(data));
                    BibDatabase db = bp.parse().getDatabase();
                    LOGGER.info("Parsed " + db.getEntryCount() + " entries from clipboard text");
                    if (db.hasEntries()) {
                        result = db.getEntries();
                    }
                }
            } catch (UnsupportedFlavorException ex) {
                LOGGER.warn("Could not parse this type", ex);
            } catch (IOException ex) {
                LOGGER.warn("Data is no longer available in the requested flavor", ex);
            }

        }
        return result;
    }
}
