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
package net.sf.jabref.export;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;


public class RtfSelection implements Transferable {
    DataFlavor rtfFlavor;
    DataFlavor[] supportedFlavors;
    private String content;

    public RtfSelection(String s) {
        content = s;
        try {
            rtfFlavor = new DataFlavor
                    ("text/rtf; class=java.io.InputStream");
            supportedFlavors = new DataFlavor[]
            {rtfFlavor, DataFlavor.stringFlavor};
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(rtfFlavor) ||
                flavor.equals(DataFlavor.stringFlavor);
    }

    public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
        //System.out.println("..");
        return supportedFlavors;
    }

    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {

        if (flavor.equals(DataFlavor.stringFlavor)) {
            //System.out.println("Delivering string data.");
            return content;
        } else if (flavor.equals(rtfFlavor)) {
            //System.out.println("Delivering rtf data.");
            byte[] byteArray = content.getBytes();
            return new ByteArrayInputStream(byteArray);
        }
        throw new UnsupportedFlavorException(flavor);
    }
}
