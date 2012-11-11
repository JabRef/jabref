/* Copyright (C) 2012 JabRef contributors.

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.jabref.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
Based on http://newsgroups.derkeiler.com/Archive/De/de.comp.lang.java/2010-04/msg00203.html
*/
public class HtmlTransferable implements Transferable {
    private static final int HTML = 0;
    private static final int STRING = 1;

    public static final DataFlavor HTML_FLAVOR = new DataFlavor("text/html;charset=utf-8;class=java.lang.String", "HTML Format"); // charset could be read via JabRef.jrf.basePanel().getEncoding()

    private static final DataFlavor[] FLAVORS = { HTML_FLAVOR, DataFlavor.stringFlavor };

    private String htmlText;
    private String plainText;

    /**
     * @param htmlText the text in html 
     * @param plainText the plain text
     */
    public HtmlTransferable(String htmlText, String plainText) {
        this.htmlText = htmlText;
        this.plainText = plainText;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return FLAVORS.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (int i = 0; i < FLAVORS.length; i++) {
            if (flavor.equals(FLAVORS[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.equals(FLAVORS[STRING])) {
            return plainText;
        } else if (flavor.equals(FLAVORS[HTML])) {
            return htmlText;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
