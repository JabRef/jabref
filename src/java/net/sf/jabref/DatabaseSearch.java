/*
 Copyright (C) 2003 Nathan Dunn, Morten O. Alver

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import javax.swing.SwingUtilities;

public class DatabaseSearch extends Thread {
	BasePanel panel = null;
	BibtexDatabase thisDatabase = null;
	SearchRuleSet thisRuleSet = null;
	Map<String, String> thisSearchOptions = null;
	String searchValueField = null;
	boolean reorder, select, grayOut;
	ErrorMessageDisplay errorDisplay;
	Set<BibtexEntry> matches = new HashSet<BibtexEntry>();
	public DatabaseSearch(ErrorMessageDisplay errorDisplay,
			Map<String, String> searchOptions, SearchRuleSet searchRules,
			BasePanel panel, String searchValueField, boolean reorder,
			boolean grayOut, boolean select) {
		this.panel = panel;
		this.errorDisplay = errorDisplay;
		thisDatabase = panel.getDatabase();
		thisSearchOptions = searchOptions;
		thisRuleSet = searchRules;
		this.searchValueField = searchValueField;
		this.reorder = reorder;
		this.select = select;
		this.grayOut = grayOut;
	}

	public void run() {
		int searchScore = 0;
		matches.clear();
		BibtexEntry bes = null;
		int hits = 0;

		for (String id : thisDatabase.getKeySet()){

			// 1. search all required fields using searchString
			bes = thisDatabase.getEntryById(id);
			if (bes == null)
				continue;
			// (thisTableModel.getNameFromNumber(row));

			// 2. add score per each hit
			try {
				searchScore = thisRuleSet.applyRule(thisSearchOptions, bes);
			} catch (PatternSyntaxException ex) {
				// There is something wrong with the regexp pattern.
				errorDisplay.reportError("Malformed regular expression", ex);
				return;
			}
			// When using float search, it messes up the sort order if we retain
			// graded search scores, because the table is sorted by the score.
			// To prevent this, we let the search score saturate at 1.
			if (searchScore > 0)
				searchScore = 1;

			// 2.1 set score to search field
			bes.setField(searchValueField, String.valueOf(searchScore));

			if (searchScore > 0) {
				hits++;
				matches.add(bes);
			}
		}
		final int outputHits = hits;
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				panel.output(Globals
                    .lang("Searched database. Global number of hits")
                    + ": " + outputHits);
			}
		});
	}
	
	public Iterator<BibtexEntry> matches() {
		return matches.iterator();
	}
}
