/*  Copyright (C) 2011 Sascha Hunold.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.imports;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.BibtexEntry;

public class DBLPHelper {

	private final DBLPQueryCleaner cleaner = new DBLPQueryCleaner();

	/*
	 * This is a small helper class that cleans the user submitted query. Right
	 * now, we cannot search for ":" on dblp.org. So, we remove colons from the
	 * user submitted search string. Also, the search is case sensitive if we
	 * use capitals. So, we better change the text to lower case.
	 */

	class DBLPQueryCleaner {

		public String cleanQuery(final String query) {
			String cleaned = query;

			cleaned = cleaned.replaceAll("-", " ");
			cleaned = cleaned.replaceAll(" ", "%20");
			cleaned = cleaned.replaceAll(":", "");
			cleaned = cleaned.toLowerCase();

			return cleaned;
		}
	}

	/**
	 *
	 * @param a
	 *            string with the user query
	 * @return a string with the user query, but compatible with dblp.org
	 */
	public String cleanDBLPQuery(String query) {
		return cleaner.cleanQuery(query);
	}

	/**
	 * Takes an HTML file (as String) as input and extracts the bibtex
	 * information. After that, it will convert it into a BibtexEntry and return
	 * it (them).
	 *
	 * @param html
	 *            page as String
	 * @return list of BibtexEntry
	 */
	public List<BibtexEntry> getBibTexFromPage(final String page)
			throws DBLPParseException {
		final List<BibtexEntry> bibtexList = new ArrayList<BibtexEntry>();
		final String startPattern = "<pre>";
		final String endPattern = "</pre>";

		String tmpStr = page;
		int startIdx = tmpStr.indexOf(startPattern);
		int endIdx = tmpStr.indexOf(endPattern);

		// this entry exists for sure
		String entry1 = tmpStr.substring(startIdx + startPattern.length(),
				endIdx);
		entry1 = cleanEntry(entry1);
		bibtexList.add(BibtexParser.singleFromString(entry1));
		//System.out.println("'" + entry1 + "'");

		// let's see whether there is another entry (crossref)
		tmpStr = tmpStr
				.substring(endIdx + endPattern.length(), tmpStr.length());
		startIdx = tmpStr.indexOf(startPattern);
		if (startIdx != -1) {
			endIdx = tmpStr.indexOf(endPattern);
			// this entry exists for sure
			String entry2 = tmpStr.substring(startIdx + startPattern.length(),
					endIdx);
			entry2 = cleanEntry(entry2);
			bibtexList.add(BibtexParser.singleFromString(entry2));
		}

		return bibtexList;
	}

	private String cleanEntry(final String bibEntry) {
		String retStr = bibEntry
				.replaceFirst("<a href=\".*\">DBLP</a>", "DBLP");
		return retStr;
	}

}
