package net.sf.jabref.importer.fetcher;

import java.util.Comparator;

public class LengthComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        if (o1.length() > o2.length()) {
            return -1;
        } else if (o1.length() == o2.length()) {
            return 0;
        }
        return 1;
    }
}
