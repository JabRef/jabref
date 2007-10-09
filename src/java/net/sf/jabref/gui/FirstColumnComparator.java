package net.sf.jabref.gui;

import java.util.Comparator;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexDatabase;

public class FirstColumnComparator implements Comparator<BibtexEntry> {
    private BibtexDatabase database;

    public FirstColumnComparator(BibtexDatabase database) {

        this.database = database;
    }

    public int compare(BibtexEntry e1, BibtexEntry e2) {

		int score1 = 0, score2 = 0;

		if (e1.hasAllRequiredFields(database))
			score1++;

		if (e2.hasAllRequiredFields(database))
			score2++;

		return score1 - score2;
	}

}
