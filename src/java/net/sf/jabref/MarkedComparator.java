package net.sf.jabref;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Sep 1, 2005
 * Time: 11:35:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class MarkedComparator implements Comparator<BibtexEntry> {

    Comparator<BibtexEntry> next;

    public MarkedComparator(Comparator<BibtexEntry> next) {
        this.next = next;
    }
    public int compare(BibtexEntry e1, BibtexEntry e2) {

        if (e1 == e2)
            return 0;

        boolean mrk1 = Util.isMarked(e1),
                mrk2 = Util.isMarked(e2);

        if (mrk1 == mrk2)
            return (next != null ? next.compare(e1, e2) : idCompare(e1, e2));

        else if (mrk2)
            return 1;
        else return -1;
    }

    private int idCompare(BibtexEntry b1, BibtexEntry b2) {
	    return ((b1.getId())).compareTo((b2.getId()));
    }
}
