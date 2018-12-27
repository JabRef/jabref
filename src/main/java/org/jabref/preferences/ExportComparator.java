package org.jabref.preferences;

import java.util.Comparator;
import java.util.List;

public class ExportComparator implements Comparator<List<String>> {

    @Override
    public int compare(List<String> s1, List<String> s2) {
        return s1.get(0).compareTo(s2.get(0));
    }

}
