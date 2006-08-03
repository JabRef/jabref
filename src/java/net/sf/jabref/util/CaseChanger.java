package net.sf.jabref.util;

/* Mp3dings - manage mp3 meta-information
 * Copyright (C) 2003 Moritz Ringler
 * $Id$
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 
 * Class with static methods for changing the case of strings and arrays of
 * strings.
 * 
 * @author Moritz Ringler
 * 
 * @version $Revision$ ($Date$)
 */
public class CaseChanger {

	/** Lowercase */
	public final static int LOWER = 0;

	/** Uppercase */
	public final static int UPPER = 1;

	/** First letter of string uppercase */
	public final static int UPPER_FIRST = 2;

	/** First letter of each word uppercase */
	public final static int UPPER_EACH_FIRST = 3;

	/**
	 * I don't think it is thread-safe to use the same matcher at the same time for all calls.
	 */
	private final static Pattern UF_PATTERN = Pattern.compile("\\b\\w");

	// private final static Matcher UF_MATCHER =
	// // Pattern.compile("(?i)\\b\\w").matcher("");
	// Pattern.compile("\\b\\w").matcher("");

	/* you can add more modes here */
	private final static int numModes = 4;

	private final static String[] modeNames = { "lower", "UPPER", "Upper first", "Upper Each First" };

	/**
	 * Gets the name of a case changing mode
	 * 
	 * @param mode
	 *            by default one of LOWER, UPPER, UPPER_FIRST or
	 *            UPPER_EACH_FIRST
	 */
	public static String getModeName(int mode) {
		return modeNames[mode];
	}

	/** Gets the names of all available case changing modes */
	public static String[] getModeNames() {
		return modeNames;
	}

	/** Gets the number of available case changing modes */
	public static int getNumModes() {
		return numModes;
	}

	/**
	 * Changes the case of the specified strings. wrapper for
	 * {@link #changeCase(String input, int mode)}
	 * 
	 * @see #changeCase(String input, int mode)
	 */
	public static String[] changeCase(String[] input, int mode) {
		int n = input.length;
		String[] output = new String[n];
		for (int i = 0; i < n; i++) {
			output[i] = changeCase(input[i], mode);
		}
		return output;
	}

	/**
	 * Changes the case of the specified string
	 * 
	 * @param input
	 *            String to change
	 * @param mode
	 *            by default one of LOWER, UPPER, UPPER_FIRST or
	 *            UPPER_EACH_FIRST
	 * @return casechanged string
	 */
	public static String changeCase(String input, int mode) {
		switch (mode) {
		case UPPER:
			return input.toUpperCase();
		case LOWER:
			return input.toLowerCase();
		case UPPER_FIRST: {
			String s = input.toLowerCase();

			Matcher matcher = UF_PATTERN.matcher(s);
			if (matcher.find()) {
				return matcher.replaceFirst(matcher.group(0).toUpperCase());
			} else {
				return input;
			}
		}
		case UPPER_EACH_FIRST: {
			String s = input.toLowerCase();
			StringBuffer sb = new StringBuffer();
			boolean found = false;
			Matcher matcher = UF_PATTERN.matcher(s);
			while (matcher.find()) {
				matcher.appendReplacement(sb, matcher.group(0).toUpperCase());
				found = true;
			}
			if (found) {
				matcher.appendTail(sb);
				return sb.toString();
			} else {
				return input;
			}
		}
		default:
			return input;
		}
	}
}
