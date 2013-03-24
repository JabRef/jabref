/*  Copyright (C) 2003-2011 JabRef contributors.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.OutputPrinter;

/**
 * Importer for COPAC format.
 * 
 * Documentation can be found online at:
 * 
 * http://copac.ac.uk/faq/#format
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class CopacImporter extends ImportFormat {
	/**
	 * Return the name of this import format.
	 */
	public String getFormatName() {
		return "Copac";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.jabref.imports.ImportFormat#getCLIId()
	 */
	public String getCLIId() {
		return "cpc";
	}

	static final Pattern copacPattern = Pattern.compile("^\\s*TI- ");

	/**
	 * Check whether the source is in the correct format for this importer.
	 */
	public boolean isRecognizedFormat(InputStream stream) throws IOException {

		BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));

		String str;

		while ((str = in.readLine()) != null) {
			if (copacPattern.matcher(str).find())
				return true;
		}

		return false;
	}

	/**
	 * Parse the entries in the source, and return a List of BibtexEntry
	 * objects.
	 */
	public List<BibtexEntry> importEntries(InputStream stream, OutputPrinter status) throws IOException {
		if (stream == null)
			throw new IOException("No stream given.");

		BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));

		List<String> entries = new LinkedList<String>();

		{ // Preprocess entries
			String str;
			StringBuffer sb = new StringBuffer();

			while ((str = in.readLine()) != null) {

				if (str.length() < 4)
					continue;

				String code = str.substring(0, 4);

				if (code.equals("    ")) {
					sb.append(" ").append(str.trim());
				} else {

					// begining of a new item
					if (str.substring(0, 4).equals("TI- ")) {
						if (sb.length() > 0) {
							entries.add(sb.toString());
						}
						sb = new StringBuffer();
					}
					sb.append('\n').append(str);
				}
			}
			if (sb.length() > 0)
				entries.add(sb.toString());
		}

		List<BibtexEntry> results = new LinkedList<BibtexEntry>();

		Iterator<String> it = entries.iterator();
		while (it.hasNext()) {

			// Copac does not contain enough information on the type of the
			// document. A book is assumed.
			BibtexEntry b = new BibtexEntry(BibtexFields.DEFAULT_BIBTEXENTRY_ID,
				BibtexEntryType.BOOK);

			String[] lines = it.next().toString().split("\n");

			for (int i = 0; i < lines.length; i++) {
				String line = lines[i].trim();
				if (line.length() < 4)
					continue;
				String code = line.substring(0, 4);

				if (code.equals("TI- "))
					setOrAppend(b, "title", line.substring(4).trim(), ", ");
				else if (code.equals("AU- "))
					setOrAppend(b, "author", line.substring(4).trim(), " and ");
				else if (code.equals("PY- "))
					setOrAppend(b, "year", line.substring(4).trim(), ", ");
				else if (code.equals("PU- "))
					setOrAppend(b, "publisher", line.substring(4).trim(), ", ");
				else if (code.equals("SE- "))
					setOrAppend(b, "series", line.substring(4).trim(), ", ");
				else if (code.equals("IS- "))
					setOrAppend(b, "isbn", line.substring(4).trim(), ", ");
				else if (code.equals("KW- "))
					setOrAppend(b, "keywords", line.substring(4).trim(), ", ");
				else if (code.equals("NT- "))
					setOrAppend(b, "note", line.substring(4).trim(), ", ");
				else if (code.equals("PD- "))
					setOrAppend(b, "physicaldimensions", line.substring(4).trim(), ", ");
				else if (code.equals("DT- "))
					setOrAppend(b, "documenttype", line.substring(4).trim(), ", ");
				else
					setOrAppend(b, code.substring(0, 2), line.substring(4).trim(), ", ");
			}
			results.add(b);
		}

		return results;
	}

	void setOrAppend(BibtexEntry b, String field, String value, String separator) {
		Object o = b.getField(field);
		if (o != null)
			b.setField(field, (String) o + separator + value);
		else
			b.setField(field, value);
	}
}
