package net.sf.jabref;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Sep 1, 2005
 * Time: 11:35:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class MarkedComparator implements Comparator {

    Comparator next;

    public MarkedComparator(Comparator next) {
        this.next = next;
    }
    public int compare(Object o1, Object o2) {

        BibtexEntry e1 = (BibtexEntry)o1,
            e2 = (BibtexEntry)o2;

        if (e1 == e2)
            return 0;

        boolean mrk1 = Util.isMarked(e1),
                mrk2 = Util.isMarked(e2);

        if (mrk1 == mrk2)
            return (next != null ? next.compare(o1, o2) : idCompare(e1, e2));

        else if (mrk2)
            return 1;
        else return -1;
    }

    private int idCompare(BibtexEntry b1, BibtexEntry b2) {
	    return ((String)(b1.getId())).compareTo((String)(b2.getId()));
    }
}
