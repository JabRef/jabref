package org.jabref.gui.journals;

import java.util.Collection;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.l10n.Localization;

public class JournalAbbreviationsUtil {

    private JournalAbbreviationsUtil() {
    }

    public static TableModel getTableModel(Collection<Abbreviation> abbreviations) {
        Object[][] cells = new Object[abbreviations.size()][2];
        int row = 0;
        for (Abbreviation abbreviation : abbreviations) {
            cells[row][0] = abbreviation.getName();
            cells[row][1] = abbreviation.getIsoAbbreviation();
            row++;
        }

        return new DefaultTableModel(cells, new Object[] {Localization.lang("Full name"),
                Localization.lang("Abbreviation")}) {

            @Override
            public boolean isCellEditable(int row1, int column) {
                return false;
            }
        };
    }
}
