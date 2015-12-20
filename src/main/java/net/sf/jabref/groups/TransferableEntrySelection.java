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
package net.sf.jabref.groups;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import net.sf.jabref.model.entry.BibEntry;

class TransferableEntrySelection implements Transferable {

    public static final DataFlavor flavorInternal;
    private static final DataFlavor flavorExternal;
    private static final DataFlavor[] flavors;
    private final BibEntry[] selectedEntries;
    private final String selectedEntriesCiteKeys;

    private boolean includeCiteKeyword;

    static {
        DataFlavor df1 = null;
        DataFlavor df2 = null;
        try {
            df1 = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
                    + ";class=net.sf.jabref.groups.TransferableEntrySelection");
            df2 = DataFlavor.getTextPlainUnicodeFlavor();
        } catch (ClassNotFoundException e) {
            // never happens
        }
        flavorInternal = df1;
        flavorExternal = df2;
        flavors = new DataFlavor[] {TransferableEntrySelection.flavorInternal, TransferableEntrySelection.flavorExternal};
    }


    public TransferableEntrySelection(BibEntry[] selectedEntries) {
        this.selectedEntries = selectedEntries;
        StringBuilder keys = new StringBuilder();
        for (int i = 0; i < selectedEntries.length; ++i) {
            keys.append(selectedEntries[i].getCiteKey());
            if (i + 1 < selectedEntries.length) {
                keys.append(",");
            }
        }
        selectedEntriesCiteKeys = keys.toString();
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return TransferableEntrySelection.flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor someFlavor) {
        return someFlavor.equals(TransferableEntrySelection.flavorInternal)
                || someFlavor.equals(TransferableEntrySelection.flavorExternal);
    }

    @Override
    public Object getTransferData(DataFlavor someFlavor)
            throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(someFlavor)) {
            throw new UnsupportedFlavorException(someFlavor);
        }
        if (someFlavor.equals(TransferableEntrySelection.flavorInternal)) {
            return this;
        }
        String s = includeCiteKeyword ?
                "\\cite{" + selectedEntriesCiteKeys + "}"
                : selectedEntriesCiteKeys;
        return new ByteArrayInputStream(s.getBytes(
                TransferableEntrySelection.flavorExternal.getParameter("charset").trim()));
    }

    public BibEntry[] getSelection() {
        return selectedEntries;
    }

    public void setIncludeCiteKeyword(boolean includeCiteKeyword) {
        this.includeCiteKeyword = includeCiteKeyword;
    }

}
