package net.sf.jabref.gui.exporter;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class RtfTransferable implements Transferable {

    private static final DataFlavor RTF_Flavour = new DataFlavor("text/rtf;charset=utf-8;class=java.lang.String", "RTF Format");
    private static final DataFlavor TEXT_FLAVOR = DataFlavor.stringFlavor;

    private static final List<DataFlavor> ALL_FLAVORS = Arrays.asList(RTF_Flavour, TEXT_FLAVOR);

    private final String rtfText;
    private final String plainText;


    public RtfTransferable(String text) {
        this.rtfText = text;
        this.plainText = text;
    }

    public RtfTransferable(String rtfText, String plainText) {
        this.rtfText = rtfText;
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
       if (flavor.equals(RTF_Flavour)) {
            return rtfText;
        } else if (flavor.equals(TEXT_FLAVOR)) {
           return plainText;
       } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

}
