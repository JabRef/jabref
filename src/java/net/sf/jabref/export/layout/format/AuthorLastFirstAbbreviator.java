package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * Uses as input the fields (author or editor) in the LastFirst format.
 * 
 * This formater enables to abbreviate the authors name in the following way:
 * 
 * Ex: Someone, Van Something will be abbreviated as Someone, V.S.
 * 
 * @author Carlos Silla
 * @author Christopher Oezbek <oezi@oezi.de>
 * 
 * @version 1.0 Created on 12/10/2004
 * @version 1.1 Fixed bug
 *          http://sourceforge.net/tracker/index.php?func=detail&aid=1466924&group_id=92314&atid=600306
 */
public class AuthorLastFirstAbbreviator implements LayoutFormatter {

	/**
	 * @see net.sf.jabref.export.layout.LayoutFormatter#format(java.lang.String)
	 */
	public String format(String fieldText) {

        /**
         * This formatter is a duplicate of AuthorAbbreviator, so we simply
         * call that one.
         *
         * TODO: Note that this makes the remaining methods in this formatter obsolete. 
         */
        return (new AuthorAbbreviator()).format(fieldText);

	}

	/**
	 * Abbreviates the names in the Last, First or Last, Jr, First format.
	 * 
	 * @param authors
	 *            List of authors.
	 * @return The abbreviated names.
	 */
	private String getAbbreviations(String[] authors) {
		if (authors.length == 0)
			return "";

		if (!isProperFormat(authors)) {
			throw new IllegalArgumentException("Author names must be formatted \"Last, First\" or \"Last, Jr., First\" before formatting with AuthorLastFirstAbbreviator");
		}

		for (int i = 0; i < authors.length; i++) {
			authors[i] = getAbbreviation(authors[i]);
		}

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < authors.length - 1; i++) {
			sb.append(authors[i]).append(" and ");
		}
		sb.append(authors[authors.length - 1]);

		return sb.toString();
	}

	/**
	 * Method that verifies if the author names are in the Last, First or Last,
	 * Jr, First format.
	 * 
	 * If the name contains a space, but does not have the comma it is not in
	 * the appropriate format.
	 * 
	 * @param authors
	 *            List of authors to verify
	 */
	private boolean isProperFormat(String[] authors) {
        for (int i = 0; i < authors.length; i++) {
			if ((authors[i].indexOf(' ') >= 0)
					&& (authors[i].indexOf(',') == -1)) {
				return false;
			}

		}
		return true;
	}

	/**
	 * Abbreviates all first names of the author.
	 * 
	 * @param author
	 * @return
	 */
	private String getAbbreviation(String author) {

		String[] parts = author.split(",");

		String last, first;

		switch (parts.length) {
		case 1:
			// If there is no comma in the name, we return it as it is
			return author;
		case 2:
			last = parts[0].trim();
			first = parts[1].trim();
			break;
		case 3:
			last = parts[0].trim();
			// jr = parts[1];
			first = parts[2].trim();
			break;
		default:
			throw new IllegalArgumentException("Authorname contained 3 or more commas");
		}

		StringBuffer sb = new StringBuffer();
		sb.append(last);
		sb.append(", ");

		String[] firstNames = first.split(" ");
		for (int i = 0; i < firstNames.length; i++) {
			sb.append(firstNames[i].charAt(0));
			sb.append('.');
			if (i < firstNames.length - 1)
				sb.append(' ');
		}
		return sb.toString();
	}
}
