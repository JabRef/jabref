package net.sf.jabref.export;

import java.util.Comparator;

public class ExportComparator implements Comparator {
  public ExportComparator() {}// super(); }
  public int compare(Object o1, Object o2) {
    String[] s1 = (String[])o1,
        s2 = (String[])o2;
    return s1[0].compareTo(s2[0]);
  }

}
