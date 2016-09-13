package net.sf.jabref.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.bibtex.BibEntryWriter;
import net.sf.jabref.logic.bibtex.LatexFieldFormatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;

/*
 * A transferable object containing an array of BibEntry objects. Used
 * for copy-paste operations.
 */
public class TransferableBibtexEntry implements Transferable {

    private final List<BibEntry> data;
    public static final DataFlavor entryFlavor = new DataFlavor(BibEntry.class, "JabRef entry");


    public TransferableBibtexEntry(List<BibEntry> bes) {
        this.data = bes;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {TransferableBibtexEntry.entryFlavor,
                DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(TransferableBibtexEntry.entryFlavor) || flavor.equals(DataFlavor.stringFlavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException {
        if (flavor.equals(TransferableBibtexEntry.entryFlavor)) {
            return data;
        } else if (flavor.equals(DataFlavor.stringFlavor)) {
            try {
                StringWriter sw = new StringWriter();
                BibEntryWriter bibtexEntryWriter = new BibEntryWriter(
                        new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences()), false);
                for (BibEntry entry : data) {
                    bibtexEntryWriter.write(entry, sw, BibDatabaseMode.BIBTEX);
                }
                return sw.toString();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                        Localization.lang("Could not paste entry as text:") + "\n" + ex.getLocalizedMessage(),
                        Localization.lang("Clipboard"), JOptionPane.ERROR_MESSAGE);
                return "";
            }
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
