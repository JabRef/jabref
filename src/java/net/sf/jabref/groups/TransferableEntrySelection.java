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

import java.awt.datatransfer.*;
import java.io.IOException;

import net.sf.jabref.BibtexEntry;

public class TransferableEntrySelection implements Transferable {
    public static DataFlavor flavor;
    public static DataFlavor[] flavors;
    public final BibtexEntry[] selectedEntries;
    
    public TransferableEntrySelection(BibtexEntry[] selectedEntries) {
        this.selectedEntries = selectedEntries;
        try {
            flavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
                    + ";class=net.sf.jabref.groups.TransferableEntrySelection");
            flavors = new DataFlavor[] { flavor };
        } catch (ClassNotFoundException e) {
            // never happens
        }
    }

    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    public boolean isDataFlavorSupported(DataFlavor someFlavor) {
        return someFlavor.equals(flavor);
    }

    public Object getTransferData(DataFlavor someFlavor) throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(someFlavor))
            throw new UnsupportedFlavorException(someFlavor);
        return this;
    }
    
    public BibtexEntry[] getSelection() {
        return selectedEntries;
    }

}
