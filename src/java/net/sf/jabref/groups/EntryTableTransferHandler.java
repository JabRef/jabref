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
import java.awt.dnd.DnDConstants;
import java.awt.event.InputEvent;

import javax.swing.*;

import net.sf.jabref.EntryTable;
import net.sf.jabref.gui.MainTable;

public class EntryTableTransferHandler extends TransferHandler {
    protected final MainTable entryTable;

    public EntryTableTransferHandler(MainTable entryTable) {
        this.entryTable = entryTable;
    }

    public int getSourceActions(JComponent c) {
        return DnDConstants.ACTION_LINK;
    }

    public Transferable createTransferable(JComponent c) {
        return new TransferableEntrySelection(entryTable.getSelectedEntries());
    }

    // add-ons -----------------------

    public boolean importData(JComponent comp, Transferable t) {
        // for accepting drags (we don't to that)
        return false;
    }

    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        // for accepting drags (we don't to that)
        return false;
    }

    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        // action is always LINK
        super.exportAsDrag(comp, e, DnDConstants.ACTION_LINK);
    }

    protected void exportDone(JComponent source, Transferable data, int action) {
        // default implementation is OK
        super.exportDone(source, data, action);
    }

    public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
        // default implementation is OK
        super.exportToClipboard(comp, clip, action);
    }
}