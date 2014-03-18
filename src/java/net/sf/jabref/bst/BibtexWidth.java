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
			widths = new int[128];

			for (int i = 0; i < 128; i++) {
				widths[i] = 0;
			}
			widths[32] = 278;
			widths[33] = 278;
			widths[34] = 500;
			widths[35] = 833;
			widths[36] = 500;
			widths[37] = 833;
			widths[38] = 778;
			widths[39] = 278;
			widths[40] = 389;
			widths[41] = 389;
			widths[42] = 500;
			widths[43] = 778;
			widths[44] = 278;
			widths[45] = 333;
			widths[46] = 278;
			widths[47] = 500;
			widths[48] = 500;
			widths[49] = 500;
			widths[50] = 500;
			widths[51] = 500;
			widths[52] = 500;
			widths[53] = 500;
			widths[54] = 500;
			widths[55] = 500;
			widths[56] = 500;
			widths[57] = 500;
			widths[58] = 278;
			widths[59] = 278;
			widths[60] = 278;
			widths[61] = 778;
			widths[62] = 472;
			widths[63] = 472;
			widths[64] = 778;
			widths[65] = 750;
			widths[66] = 708;
			widths[67] = 722;
			widths[68] = 764;
			widths[69] = 681;
			widths[70] = 653;
			widths[71] = 785;
			widths[72] = 750;
			widths[73] = 361;
			widths[74] = 514;
			widths[75] = 778;
			widths[76] = 625;
			widths[77] = 917;
			widths[78] = 750;
			widths[79] = 778;
			widths[80] = 681;
			widths[81] = 778;
			widths[82] = 736;
			widths[83] = 556;
			widths[84] = 722;
			widths[85] = 750;
			widths[86] = 750;
			widths[87] = 1028;
			widths[88] = 750;
			widths[89] = 750;
			widths[90] = 611;
			widths[91] = 278;
			widths[92] = 500;
			widths[93] = 278;
			widths[94] = 500;
			widths[95] = 278;
			widths[96] = 278;
			widths[97] = 500;
			widths[98] = 556;
			widths[99] = 444;
			widths[100] = 556;
			widths[101] = 444;
			widths[102] = 306;
			widths[103] = 500;
			widths[104] = 556;
			widths[105] = 278;
			widths[106] = 306;
			widths[107] = 528;
			widths[108] = 278;
			widths[109] = 833;
			widths[110] = 556;
			widths[111] = 500;
			widths[112] = 556;
			widths[113] = 528;
			widths[114] = 392;
			widths[115] = 394;
			widths[116] = 389;
			widths[117] = 556;
			widths[118] = 528;
			widths[119] = 722;
			widths[120] = 528;
			widths[121] = 528;
			widths[122] = 444;
			widths[123] = 500;
			widths[124] = 1000;
			widths[125] = 500;
			widths[126] = 500;
		}

		if (0 <= c && c < 128) {
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
