package net.sf.jabref.gui;

import net.sf.jabref.BibtexEntry;

import java.util.Comparator;

/**
 * Comparator that handles icon columns.
 */
public class IconComparator implements Comparator<BibtexEntry> {

    private String[] fields;

    public IconComparator(String[] fields) {
        this.fields = fields;
    }

    public int compare(BibtexEntry e1, BibtexEntry e2) {

        for (int i=0; i<fields.length; i++) {
            String val1 = e1.getField(fields[i]),
                    val2 = e2.getField(fields[i]);
            if (val1 == null) {
                if (val2 != null)
                    return 1;
            } else {
                if (val2 == null)
                    return -1;
            }
        }
        return 0;
    }
}
