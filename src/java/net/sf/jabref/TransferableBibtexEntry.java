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
package net.sf.jabref;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.StringWriter;

import javax.swing.JOptionPane;

import net.sf.jabref.export.LatexFieldFormatter;

/*
 * A transferable object containing an array of BibtexEntry objects. Used
 * for copy-paste operations.
 */
public class TransferableBibtexEntry implements Transferable {

    private BibtexEntry[] data;
    public static DataFlavor entryFlavor = new DataFlavor(BibtexEntry.class, "JabRef entry");

    public TransferableBibtexEntry(BibtexEntry[] data) {
	this.data = data;
    }

    public DataFlavor[] getTransferDataFlavors() {
	return new DataFlavor[] {TransferableBibtexEntry.entryFlavor,
	                         DataFlavor.stringFlavor};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
	return (flavor.equals(entryFlavor) ||
		flavor.equals(DataFlavor.stringFlavor));
    }

    public Object getTransferData(DataFlavor flavor)
	throws UnsupportedFlavorException {
	if (flavor.equals(entryFlavor))
	    return data;
	else if (flavor.equals(DataFlavor.stringFlavor)) {
	    try {
		StringWriter sw = new StringWriter();
		LatexFieldFormatter ff = new LatexFieldFormatter();
		for (int i=0; i<data.length; i++)
		    data[i].write(sw, ff, false);
		return sw.toString();
	    } catch (IOException ex) {
		JOptionPane.showMessageDialog
		    (null, "Could not paste entry as text:\n"+ex.getMessage(),
		     "Clipboard", JOptionPane.ERROR_MESSAGE);
		return "";
	    }
	} else
	    throw new UnsupportedFlavorException(flavor);
    }
}
