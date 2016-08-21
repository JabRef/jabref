package net.sf.jabref.gui.exporter;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class RtfSelection implements Transferable {

    private static final Log LOGGER = LogFactory.getLog(RtfSelection.class);

    private DataFlavor rtfFlavor;
    private DataFlavor[] supportedFlavors;
    private final String content;


    public RtfSelection(String s) {
        content = s;
        try {
            rtfFlavor = new DataFlavor("text/rtf; class=java.io.InputStream");
            supportedFlavors = new DataFlavor[] {rtfFlavor, DataFlavor.stringFlavor};
        } catch (ClassNotFoundException ex) {
            LOGGER.warn("Cannot find class", ex);
        }
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(rtfFlavor) ||
                flavor.equals(DataFlavor.stringFlavor);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return supportedFlavors;
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {

        if (flavor.equals(DataFlavor.stringFlavor)) {
            return content;
        } else if (flavor.equals(rtfFlavor)) {
            byte[] byteArray = content.getBytes();
            return new ByteArrayInputStream(byteArray);
        }
        throw new UnsupportedFlavorException(flavor);
    }
}
