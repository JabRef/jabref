package org.jabref.gui.groups;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;

public class TransferableEntrySelection implements Transferable {

    public static final DataFlavor FLAVOR_INTERNAL;
    private static final DataFlavor FLAVOR_EXTERNAL;
    private static final DataFlavor[] FLAVORS;

    static {
        DataFlavor df1 = null;
        DataFlavor df2 = null;
        try {
            df1 = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
                    + ";class=org.jabref.gui.groups.TransferableEntrySelection");
            df2 = DataFlavor.getTextPlainUnicodeFlavor();
        } catch (ClassNotFoundException e) {
            // never happens
        }
        FLAVOR_INTERNAL = df1;
        FLAVOR_EXTERNAL = df2;
        FLAVORS = new DataFlavor[] {TransferableEntrySelection.FLAVOR_INTERNAL,
                TransferableEntrySelection.FLAVOR_EXTERNAL};
    }

    private final List<BibEntry> selectedEntries;
    private final String selectedEntriesCiteKeys;
    private boolean includeCiteKeyword;

    public TransferableEntrySelection(List<BibEntry> list) {
        this.selectedEntries = list;
        selectedEntriesCiteKeys = String.join(",",
                this.selectedEntries.stream().map(BibEntry::getCiteKeyOptional).filter(Optional::isPresent)
                        .map(Optional::get).collect(Collectors.toList()));
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return TransferableEntrySelection.FLAVORS;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor someFlavor) {
        return someFlavor.equals(TransferableEntrySelection.FLAVOR_INTERNAL)
                || someFlavor.equals(TransferableEntrySelection.FLAVOR_EXTERNAL);
    }

    @Override
    public Object getTransferData(DataFlavor someFlavor)
            throws UnsupportedFlavorException, IOException {

        String s = includeCiteKeyword ? "\\cite{" + selectedEntriesCiteKeys + "}" : selectedEntriesCiteKeys;

        if (someFlavor.equals(TransferableEntrySelection.FLAVOR_INTERNAL)) {
            return this;
        }

        else if (someFlavor.equals(DataFlavor.getTextPlainUnicodeFlavor())) {

            String charsetName = TransferableEntrySelection.FLAVOR_EXTERNAL.getParameter("charset");
            if (charsetName == null) {
                charsetName = "";
            }
            Charset charset = Charset.forName(charsetName.trim());
            return new ByteArrayInputStream(s.getBytes(charset));
        }

        //The text/plain DataFormat of javafx uses the String.class directly as representative class and no longer an InputStream
        return s;
    }

    public List<BibEntry> getSelection() {
        return selectedEntries;
    }

    public void setIncludeCiteKeyword(boolean includeCiteKeyword) {
        this.includeCiteKeyword = includeCiteKeyword;
    }

}
