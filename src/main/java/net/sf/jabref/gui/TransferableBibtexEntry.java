/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.gui;

import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.bibtex.BibEntryWriter;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/*
 * A transferable object containing an array of BibEntry objects. Used
 * for copy-paste operations.
 */
public class TransferableBibtexEntry implements Transferable {

    private final List<BibEntry> data;
    public static final DataFlavor entryFlavor = new DataFlavor(BibEntry.class, "JabRef entry");


    public TransferableBibtexEntry(List<BibEntry> bes) {
        this.data = bes;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {TransferableBibtexEntry.entryFlavor,
                DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(TransferableBibtexEntry.entryFlavor) || flavor.equals(DataFlavor.stringFlavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException {
        if (flavor.equals(TransferableBibtexEntry.entryFlavor)) {
            return data;
        } else if (flavor.equals(DataFlavor.stringFlavor)) {
            try {
                StringWriter sw = new StringWriter();
                BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new LatexFieldFormatter(), false);
                for (BibEntry entry : data) {
                    bibtexEntryWriter.write(entry, sw, BibDatabaseMode.BIBTEX);
                }
                return sw.toString();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        Localization.lang("Could not paste entry as text:") + "\n" + ex.getLocalizedMessage(),
                        Localization.lang("Clipboard"), JOptionPane.ERROR_MESSAGE);
                return "";
            }
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
