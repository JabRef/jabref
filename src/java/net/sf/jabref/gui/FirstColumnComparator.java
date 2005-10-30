package net.sf.jabref.gui;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Util;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 14, 2005
 * Time: 8:25:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class FirstColumnComparator implements Comparator {

    public int compare(Object o1, Object o2) {

        BibtexEntry e1 = (BibtexEntry)o1,
                 e2 = (BibtexEntry)o2;

        int score1=0, score2=0;

        if (Util.isMarked(e1))
            score1 -= 2;

        if (Util.isMarked(e2))
            score2 -= 2;

        if (e1.hasAllRequiredFields())
            score1++;

        if (e2.hasAllRequiredFields())
            score2++;

        return score1-score2;
    }

}
