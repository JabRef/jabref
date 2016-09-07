package net.sf.jabref.gui.fieldeditors;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 Based on http://newsgroups.derkeiler.com/Archive/De/de.comp.lang.java/2010-04/msg00203.html
 */
class HtmlTransferable implements Transferable {

    private static final int HTML = 0;
    private static final int STRING = 1;

    private static final DataFlavor HTML_FLAVOR = new DataFlavor("text/html;charset=utf-8;class=java.lang.String", "HTML Format"); // charset could be read via JabRef.mainFrame.getCurrentBasePanel().getEncoding()

    private static final DataFlavor[] FLAVORS = {HtmlTransferable.HTML_FLAVOR, DataFlavor.stringFlavor};

    private final String htmlText;
    private final String plainText;


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
        return HtmlTransferable.FLAVORS.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (DataFlavor FLAVOR : HtmlTransferable.FLAVORS) {
            if (flavor.equals(FLAVOR)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.equals(HtmlTransferable.FLAVORS[HtmlTransferable.STRING])) {
            return plainText;
        } else if (flavor.equals(HtmlTransferable.FLAVORS[HtmlTransferable.HTML])) {
            return htmlText;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
