package org.jabref.logic.bibtex.comparator;

import java.util.Comparator;
import java.util.Locale;

import org.jabref.model.entry.BibtexString;

public class BibtexStringComparator implements Comparator<BibtexString> {

    private final boolean considerRefs;


    /**
     * @param considerRefs Indicates whether the strings should be
     *                     sorted according to internal references in addition to
     *                     alphabetical sorting.
     */
    public BibtexStringComparator(boolean considerRefs) {
        this.considerRefs = considerRefs;
    }

    @Override
    public int compare(BibtexString s1, BibtexString s2) {

        int res;

        // First check their names:
        String name1 = s1.getName().toLowerCase(Locale.ROOT);
        String name2 = s2.getName().toLowerCase(Locale.ROOT);

        res = name1.compareTo(name2);

        if (res == 0) {
            return res;
        }

        // Then, if we are supposed to, see if the ordering needs
        // to be changed because of one string referring to the other.x
        if (considerRefs) {

            // First order them:
            BibtexString pre;
            BibtexString post;
            if (res < 0) {
                pre = s1;
                post = s2;
            } else {
                pre = s2;
                post = s1;
            }

            // Then see if "pre" refers to "post", which is the only
            // situation when we must change the ordering:
            String namePost = post.getName().toLowerCase(Locale.ROOT);
            String textPre = pre.getContent().toLowerCase(Locale.ROOT);

            // If that is the case, reverse the order found:
            if (textPre.contains("#" + namePost + "#")) {
                res = -res;
            }

        }

        return res;
    }

}
