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



