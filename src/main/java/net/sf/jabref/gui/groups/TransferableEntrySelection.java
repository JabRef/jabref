package net.sf.jabref.gui.groups;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.sf.jabref.model.entry.BibEntry;

public class TransferableEntrySelection implements Transferable {

    public static final DataFlavor FLAVOR_INTERNAL;
    private static final DataFlavor FLAVOR_EXTERNAL;
    private static final DataFlavor[] FLAVORS;
    private final List<BibEntry> selectedEntries;
    private final String selectedEntriesCiteKeys;

    private boolean includeCiteKeyword;

    static {
        DataFlavor df1 = null;
        DataFlavor df2 = null;
        try {
            df1 = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
                    + ";class=net.sf.jabref.gui.groups.TransferableEntrySelection");
            df2 = DataFlavor.getTextPlainUnicodeFlavor();
        } catch (ClassNotFoundException e) {
            // never happens
        }
        FLAVOR_INTERNAL = df1;
        FLAVOR_EXTERNAL = df2;
        FLAVORS = new DataFlavor[] {TransferableEntrySelection.FLAVOR_INTERNAL, TransferableEntrySelection.FLAVOR_EXTERNAL};
    }


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
        if (!isDataFlavorSupported(someFlavor)) {
            throw new UnsupportedFlavorException(someFlavor);
        }
        if (someFlavor.equals(TransferableEntrySelection.FLAVOR_INTERNAL)) {
            return this;
        }
        String s = includeCiteKeyword ?
                "\\cite{" + selectedEntriesCiteKeys + "}"
                : selectedEntriesCiteKeys;
        String charset = TransferableEntrySelection.FLAVOR_EXTERNAL.getParameter("charset");
        if (charset == null) {
            charset = "";
        }
        return new ByteArrayInputStream(s.getBytes(charset.trim()));
    }

    public List<BibEntry> getSelection() {
        return selectedEntries;
    }

    public void setIncludeCiteKeyword(boolean includeCiteKeyword) {
        this.includeCiteKeyword = includeCiteKeyword;
    }

}
