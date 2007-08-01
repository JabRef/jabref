/*
 All programs in this directory and subdirectories are published under the 
 GNU General Public License as described below.

 This program is free software; you can redistribute it and/or modify it 
 under the terms of the GNU General Public License as published by the Free 
 Software Foundation; either version 2 of the License, or (at your option) 
 any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT 
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 more details.

 You should have received a copy of the GNU General Public License along 
 with this program; if not, write to the Free Software Foundation, Inc., 59 
 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html
 */

package net.sf.jabref.groups;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import net.sf.jabref.BibtexEntry;

public class TransferableEntrySelection implements Transferable {
    public static final DataFlavor flavorInternal;
    public static final DataFlavor flavorExternal;
    public static final DataFlavor[] flavors;
    public final BibtexEntry[] selectedEntries;
    public final String selectedEntriesCiteKeys;
    
    protected boolean includeCiteKeyword = false;

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
        flavors = new DataFlavor[] { flavorInternal, flavorExternal };
    }

    public TransferableEntrySelection(BibtexEntry[] selectedEntries) {
        this.selectedEntries = selectedEntries;
        StringBuffer keys = new StringBuffer();
        for (int i = 0; i < selectedEntries.length; ++i) {
            keys.append(selectedEntries[i].getCiteKey());
            if (i + 1 < selectedEntries.length)
                keys.append(",");
        }
        selectedEntriesCiteKeys = keys.toString();
    }

    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    public boolean isDataFlavorSupported(DataFlavor someFlavor) {
        return someFlavor.equals(flavorInternal)
                || someFlavor.equals(flavorExternal);
    }

    public Object getTransferData(DataFlavor someFlavor)
            throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(someFlavor))
            throw new UnsupportedFlavorException(someFlavor);
        if (someFlavor.equals(flavorInternal))
            return this;
        String s = includeCiteKeyword ?
                "\\cite{" + selectedEntriesCiteKeys + "}" 
                : selectedEntriesCiteKeys;
        return new ByteArrayInputStream(s.getBytes(
                flavorExternal.getParameter("charset").trim()));
    }

    public BibtexEntry[] getSelection() {
        return selectedEntries;
    }

    public void setIncludeCiteKeyword(boolean includeCiteKeyword) {
        this.includeCiteKeyword = includeCiteKeyword;
    }
    

}
