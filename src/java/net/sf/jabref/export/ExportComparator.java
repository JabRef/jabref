package net.sf.jabref.export;

import java.util.Comparator;

public class ExportComparator implements Comparator<String[]> {
	public int compare(String[] s1, String[] s2) {
		return s1[0].compareTo(s2[0]);
	}

}
