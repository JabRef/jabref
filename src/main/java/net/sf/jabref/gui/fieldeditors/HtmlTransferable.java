package net.sf.jabref.gui.fieldeditors;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


/**
 Based on http://newsgroups.derkeiler.com/Archive/De/de.comp.lang.java/2010-04/msg00203.html
 */
public class HtmlTransferable implements Transferable {

    public static final DataFlavor HTML_FLAVOR = new DataFlavor("text/html;charset=utf-8;class=java.lang.String", "HTML Format");
    public static final DataFlavor TEXT_FLAVOR = DataFlavor.stringFlavor;

    private static final List<DataFlavor> ALL_FLAVORS = Arrays.asList(HTML_FLAVOR, TEXT_FLAVOR);

    private final String htmlText;
    private final String plainText;


    public HtmlTransferable(String text) {
        this.htmlText = text;
        this.plainText = text;
    }

    public HtmlTransferable(String htmlText, String plainText) {
        this.htmlText = htmlText;
        this.plainText = plainText;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return ALL_FLAVORS.toArray(new DataFlavor[ALL_FLAVORS.size()]);
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return ALL_FLAVORS.parallelStream().anyMatch(flavor::equals);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.equals(HTML_FLAVOR)) {
            return htmlText;
        } else if (flavor.equals(TEXT_FLAVOR)) {
            return plainText;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

}
