package org.jabref.gui.fieldeditors;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class XmlTransferable implements Transferable {

    private static final DataFlavor XML_FLAVOR = new DataFlavor("application/xml;charset=utf-8;class=java.lang.String", "XML Format");
    private static final DataFlavor TEXT_FLAVOR = DataFlavor.stringFlavor;

    private static final List<DataFlavor> ALL_FLAVORS = Arrays.asList(XML_FLAVOR, HtmlTransferable.HTML_FLAVOR, TEXT_FLAVOR);

    private final String xmlText;
    private final String plainText;

    public XmlTransferable(String text) {
        this(text, text);
    }

    public XmlTransferable(String xmlText, String plainText) {
        this.xmlText = xmlText;
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
        if (flavor.equals(XML_FLAVOR) || flavor.equals(HtmlTransferable.HTML_FLAVOR)) {
            return xmlText;
        } else if (flavor.equals(TEXT_FLAVOR)) {
            return plainText;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

}
