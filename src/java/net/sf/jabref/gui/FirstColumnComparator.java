package net.sf.jabref.gui;

import java.util.Comparator;

import net.sf.jabref.BibtexEntry;

public class FirstColumnComparator implements Comparator<BibtexEntry> {

	public int compare(BibtexEntry e1, BibtexEntry e2) {

		int score1 = 0, score2 = 0;

		if (e1.hasAllRequiredFields())
			score1++;

		if (e2.hasAllRequiredFields())
			score2++;

		return score1 - score2;
	}

}
