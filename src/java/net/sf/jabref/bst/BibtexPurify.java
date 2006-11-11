// $Id$
package net.sf.jabref.bst;


/**
 * 
 * The |built_in| function {\.{purify\$}} pops the top (string) literal, removes
 * nonalphanumeric characters except for |white_space| and |sep_char| characters
 * (these get converted to a |space|) and removes certain alphabetic characters
 * contained in the control sequences associated with a special character, and
 * pushes the resulting string. If the literal isn't a string, it complains and
 * pushes the null string.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class BibtexPurify {

	/**
	 * 
	 * @param toPurify
	 * @param warn
	 *            may-be-null
	 * @return
	 */
	public static String purify(String toPurify, Warn warn) {

		StringBuffer sb = new StringBuffer();

		char[] cs = toPurify.toCharArray();
		int n = cs.length;
		int i = 0;

		int braceLevel = 0;

		while (i < n) {
			char c = cs[i];
			if (Character.isWhitespace(c) || c == '-' || c == '~') {
				sb.append(' ');
			} else if (Character.isLetterOrDigit(c)) {
				sb.append(c);
			} else if (c == '{') {
				braceLevel++;
				if (braceLevel == 1 && i + 1 < n && (cs[i + 1] == '\\')) {
					i++; // skip brace
					while (i < n && braceLevel > 0) {
						i++; // skip backslash
						String specialStart = BibtexCaseChanger.findSpecialChar(cs, i);
						if (specialStart != null) {
							sb.append(specialStart);
						} 
						while (i < n && Character.isLetter(cs[i])) {
							i++;
						}
						while (i < n && braceLevel > 0 && (c = cs[i]) != '\\') {
							if (Character.isLetterOrDigit(c)) {
								sb.append(c);
							} else if (c == '}') {
								braceLevel--;
							} else if (c == '{') {
								braceLevel++;
							}
							i++;
						}
					}
					continue;
				}
			} else if (c == '}') {
				if (braceLevel > 0) {
					braceLevel--;
				} else {
					if (warn != null)
						warn.warn("Unbalanced brace in string for purify$: " + toPurify);	
				}
			}
			i++;
		}
		if (braceLevel != 0 && warn != null)
			warn.warn("Unbalanced brace in string for purify$: " + toPurify);

		return sb.toString();
	}
}
