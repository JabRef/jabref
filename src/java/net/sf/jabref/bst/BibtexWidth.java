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
public class BibtexWidth {

	/*
	 * Quoted from Bibtex:
	 * 
	 * Now we initialize the system-dependent |char_width| array, for which
	 * |space| is the only |white_space| character given a nonzero printing
	 * width. The widths here are taken from Stanford's June~'87 $cmr10$~font
	 * and represent hundredths of a point (rounded), but since they're used
	 * only for relative comparisons, the units have no meaning.
	 */

	static int[] widths;

	static int getSpecialCharWidth(char[] c, int pos) {
		if (pos + 1 < c.length) {
			if (c[pos] == 'o' && c[pos + 1] == 'e')
				return 778;
			if (c[pos] == 'O' && c[pos + 1] == 'E')
				return 1014;
			if (c[pos] == 'a' && c[pos + 1] == 'e')
				return 722;
			if (c[pos] == 'A' && c[pos + 1] == 'E')
				return 903;
			if (c[pos] == 's' && c[pos + 1] == 's')
				return 500;
		}
		return getCharWidth(c[pos]);
	}

	public static int getCharWidth(char c) {

		if (widths == null) {
			// Watch out octals!!
			widths = new int[0200];

			for (int i = 0; i < 0200; i++) {
				widths[i] = 0;
			}
			// Watch out octals!!
			widths[040] = 278;
			widths[041] = 278;
			widths[042] = 500;
			widths[043] = 833;
			widths[044] = 500;
			widths[045] = 833;
			widths[046] = 778;
			widths[047] = 278;
			widths[050] = 389;
			widths[051] = 389;
			widths[052] = 500;
			widths[053] = 778;
			widths[054] = 278;
			widths[055] = 333;
			widths[056] = 278;
			widths[057] = 500;
			widths[060] = 500;
			widths[061] = 500;
			widths[062] = 500;
			widths[063] = 500;
			widths[064] = 500;
			widths[065] = 500;
			widths[066] = 500;
			widths[067] = 500;
			widths[070] = 500;
			widths[071] = 500;
			widths[072] = 278;
			widths[073] = 278;
			widths[074] = 278;
			widths[075] = 778;
			widths[076] = 472;
			widths[077] = 472;
			widths[0100] = 778;
			widths[0101] = 750;
			widths[0102] = 708;
			widths[0103] = 722;
			widths[0104] = 764;
			widths[0105] = 681;
			widths[0106] = 653;
			widths[0107] = 785;
			widths[0110] = 750;
			widths[0111] = 361;
			widths[0112] = 514;
			widths[0113] = 778;
			widths[0114] = 625;
			widths[0115] = 917;
			widths[0116] = 750;
			widths[0117] = 778;
			widths[0120] = 681;
			widths[0121] = 778;
			widths[0122] = 736;
			widths[0123] = 556;
			widths[0124] = 722;
			widths[0125] = 750;
			widths[0126] = 750;
			widths[0127] = 1028;
			widths[0130] = 750;
			widths[0131] = 750;
			widths[0132] = 611;
			widths[0133] = 278;
			widths[0134] = 500;
			widths[0135] = 278;
			widths[0136] = 500;
			widths[0137] = 278;
			widths[0140] = 278;
			widths[0141] = 500;
			widths[0142] = 556;
			widths[0143] = 444;
			widths[0144] = 556;
			widths[0145] = 444;
			widths[0146] = 306;
			widths[0147] = 500;
			widths[0150] = 556;
			widths[0151] = 278;
			widths[0152] = 306;
			widths[0153] = 528;
			widths[0154] = 278;
			widths[0155] = 833;
			widths[0156] = 556;
			widths[0157] = 500;
			widths[0160] = 556;
			widths[0161] = 528;
			widths[0162] = 392;
			widths[0163] = 394;
			widths[0164] = 389;
			widths[0165] = 556;
			widths[0166] = 528;
			widths[0167] = 722;
			widths[0170] = 528;
			widths[0171] = 528;
			widths[0172] = 444;
			widths[0173] = 500;
			widths[0174] = 1000;
			widths[0175] = 500;
			widths[0176] = 500;
		}

		if (0 <= c && c < 0200) {
			return widths[c];
		} else {
			return 0;
		}
	}

	/**
	 * 
	 * @param toMeasure
	 * @param warn
	 *            may-be-null
	 * @return
	 */
	public static int width(String toMeasure, Warn warn) {
		
		/*
		 * From Bibtex: We use the natural width for all but special characters,
		 * and we complain if the string isn't brace-balanced.
		 */

		int i = 0;
		int n = toMeasure.length();
		int braceLevel = 0;
		char[] c = toMeasure.toCharArray();
		int result = 0;

		
		/*
		 * From Bibtex:
		 * 
		 * We use the natural widths of all characters except that some
		 * characters have no width: braces, control sequences (except for the
		 * usual 13 accented and foreign characters, whose widths are given in
		 * the next module), and |white_space| following control sequences (even
		 * a null control sequence).
		 * 
		 */
		while (i < n){
			if (c[i] == '{'){
				braceLevel++;
				if (braceLevel == 1 && i + 1 < n && (c[i+1] == '\\')){
					i++; // skip brace
					while (i < n && braceLevel > 0){
						i++; // skip backslash
						
						int afterBackslash = i;
						while (i < n && Character.isLetter(c[i])){
							i++;
						}
						if (i < n && i == afterBackslash){
							i++; // Skip non-alpha control seq
						} else {
							if (BibtexCaseChanger.findSpecialChar(c, afterBackslash) != null) {
								result += getSpecialCharWidth(c, afterBackslash);
							}
						}
						while (i < n && Character.isWhitespace(c[i])){
							i++;
						}
						while (i < n && braceLevel > 0 && c[i] != '\\'){
							if (c[i] == '}'){
								braceLevel--;
							} else
							if (c[i] == '{'){
								braceLevel++;
							} else
								result += getCharWidth(c[i]);
							i++;
						}
					}
					continue;
				}
			} else if (c[i] == '}'){
				if (braceLevel > 0){
					braceLevel--;
				} else {
					BibtexCaseChanger.complain(toMeasure);
				}
			}
			result += getCharWidth(c[i]);
			i++;
		} 
		BibtexCaseChanger.checkBrace(toMeasure, braceLevel);
		return result;
	}
}
