package net.sf.jabref.gui;

import net.sf.jabref.BibtexEntry;

import java.util.Comparator;

/**
 * Comparator that handles icon columns.
 */
public class IconComparator implements Comparator {

    private String[] fields;

    public IconComparator(String[] fields) {
        this.fields = fields;
    }

    public int compare(Object o1, Object o2) {
         BibtexEntry e1 = (BibtexEntry)o1,
                 e2 = (BibtexEntry)o2;

        for (int i=0; i<fields.length; i++) {
            Object val1 = e1.getField(fields[i]),
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
