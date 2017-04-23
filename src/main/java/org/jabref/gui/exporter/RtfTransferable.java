package org.jabref.gui.exporter;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RtfTransferable implements Transferable {

    private static final DataFlavor RTF_FLAVOR = new DataFlavor("text/rtf; class=java.io.InputStream", "RTF Format");
    private static final DataFlavor TEXT_FLAVOR = DataFlavor.stringFlavor;

    private static final List<DataFlavor> ALL_FLAVORS = Arrays.asList(RTF_FLAVOR, TEXT_FLAVOR);

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
       if (flavor.equals(RTF_FLAVOR)) {
           return new ByteArrayInputStream(rtfText.getBytes());
        } else if (flavor.equals(TEXT_FLAVOR)) {
           return plainText;
       } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

}
