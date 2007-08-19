package net.sf.jabref.gui;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Util;

import java.util.Comparator;

public class IsMarkedComparator implements Comparator<BibtexEntry> {

	public int compare(BibtexEntry e1, BibtexEntry e2) {

		if (Util.isMarked(e1))
			return Util.isMarked(e2) ? 0 : -1;
		else
			return Util.isMarked(e2) ? 1 : 0;
	}

}
