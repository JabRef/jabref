package org.jabref.logic.bst.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * The |built_in| function {\.{purify\$}} pops the top (string) literal, removes
 * nonalphanumeric characters except for |white_space| and |sep_char| characters
 * (these get converted to a |space|) and removes certain alphabetic characters
 * contained in the control sequences associated with a special character, and
 * pushes the resulting string. If the literal isn't a string, it complains and
 * pushes the null string.
 *
 */
public class BstWidthCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BstWidthCalculator.class);

    /*
     * Quoted from Bibtex:
     *
     * Now we initialize the system-dependent |char_width| array, for which
     * |space| is the only |white_space| character given a nonzero printing
     * width. The widths here are taken from Stanford's June~'87 $cmr10$~font
     * and represent hundredths of a point (rounded), but since they're used
     * only for relative comparisons, the units have no meaning.
     */

    private static int[] widths;

    static {
        if (BstWidthCalculator.widths == null) {
            BstWidthCalculator.widths = new int[128];

            for (int i = 0; i < 128; i++) {
                BstWidthCalculator.widths[i] = 0;
            }
            BstWidthCalculator.widths[32] = 278;
            BstWidthCalculator.widths[33] = 278;
            BstWidthCalculator.widths[34] = 500;
            BstWidthCalculator.widths[35] = 833;
            BstWidthCalculator.widths[36] = 500;
            BstWidthCalculator.widths[37] = 833;
            BstWidthCalculator.widths[38] = 778;
            BstWidthCalculator.widths[39] = 278;
            BstWidthCalculator.widths[40] = 389;
            BstWidthCalculator.widths[41] = 389;
            BstWidthCalculator.widths[42] = 500;
            BstWidthCalculator.widths[43] = 778;
            BstWidthCalculator.widths[44] = 278;
            BstWidthCalculator.widths[45] = 333;
            BstWidthCalculator.widths[46] = 278;
            BstWidthCalculator.widths[47] = 500;
            BstWidthCalculator.widths[48] = 500;
            BstWidthCalculator.widths[49] = 500;
            BstWidthCalculator.widths[50] = 500;
            BstWidthCalculator.widths[51] = 500;
            BstWidthCalculator.widths[52] = 500;
            BstWidthCalculator.widths[53] = 500;
            BstWidthCalculator.widths[54] = 500;
            BstWidthCalculator.widths[55] = 500;
            BstWidthCalculator.widths[56] = 500;
            BstWidthCalculator.widths[57] = 500;
            BstWidthCalculator.widths[58] = 278;
            BstWidthCalculator.widths[59] = 278;
            BstWidthCalculator.widths[60] = 278;
            BstWidthCalculator.widths[61] = 778;
            BstWidthCalculator.widths[62] = 472;
            BstWidthCalculator.widths[63] = 472;
            BstWidthCalculator.widths[64] = 778;
            BstWidthCalculator.widths[65] = 750;
            BstWidthCalculator.widths[66] = 708;
            BstWidthCalculator.widths[67] = 722;
            BstWidthCalculator.widths[68] = 764;
            BstWidthCalculator.widths[69] = 681;
            BstWidthCalculator.widths[70] = 653;
            BstWidthCalculator.widths[71] = 785;
            BstWidthCalculator.widths[72] = 750;
            BstWidthCalculator.widths[73] = 361;
            BstWidthCalculator.widths[74] = 514;
            BstWidthCalculator.widths[75] = 778;
            BstWidthCalculator.widths[76] = 625;
            BstWidthCalculator.widths[77] = 917;
            BstWidthCalculator.widths[78] = 750;
            BstWidthCalculator.widths[79] = 778;
            BstWidthCalculator.widths[80] = 681;
            BstWidthCalculator.widths[81] = 778;
            BstWidthCalculator.widths[82] = 736;
            BstWidthCalculator.widths[83] = 556;
            BstWidthCalculator.widths[84] = 722;
            BstWidthCalculator.widths[85] = 750;
            BstWidthCalculator.widths[86] = 750;
            BstWidthCalculator.widths[87] = 1028;
            BstWidthCalculator.widths[88] = 750;
            BstWidthCalculator.widths[89] = 750;
            BstWidthCalculator.widths[90] = 611;
            BstWidthCalculator.widths[91] = 278;
            BstWidthCalculator.widths[92] = 500;
            BstWidthCalculator.widths[93] = 278;
            BstWidthCalculator.widths[94] = 500;
            BstWidthCalculator.widths[95] = 278;
            BstWidthCalculator.widths[96] = 278;
            BstWidthCalculator.widths[97] = 500;
            BstWidthCalculator.widths[98] = 556;
            BstWidthCalculator.widths[99] = 444;
            BstWidthCalculator.widths[100] = 556;
            BstWidthCalculator.widths[101] = 444;
            BstWidthCalculator.widths[102] = 306;
            BstWidthCalculator.widths[103] = 500;
            BstWidthCalculator.widths[104] = 556;
            BstWidthCalculator.widths[105] = 278;
            BstWidthCalculator.widths[106] = 306;
            BstWidthCalculator.widths[107] = 528;
            BstWidthCalculator.widths[108] = 278;
            BstWidthCalculator.widths[109] = 833;
            BstWidthCalculator.widths[110] = 556;
            BstWidthCalculator.widths[111] = 500;
            BstWidthCalculator.widths[112] = 556;
            BstWidthCalculator.widths[113] = 528;
            BstWidthCalculator.widths[114] = 392;
            BstWidthCalculator.widths[115] = 394;
            BstWidthCalculator.widths[116] = 389;
            BstWidthCalculator.widths[117] = 556;
            BstWidthCalculator.widths[118] = 528;
            BstWidthCalculator.widths[119] = 722;
            BstWidthCalculator.widths[120] = 528;
            BstWidthCalculator.widths[121] = 528;
            BstWidthCalculator.widths[122] = 444;
            BstWidthCalculator.widths[123] = 500;
            BstWidthCalculator.widths[124] = 1000;
            BstWidthCalculator.widths[125] = 500;
            BstWidthCalculator.widths[126] = 500;
        }
    }

    private BstWidthCalculator() {
    }

    private static int getSpecialCharWidth(char[] c, int pos) {
        if ((pos + 1) < c.length) {
            if ((c[pos] == 'o') && (c[pos + 1] == 'e')) {
                return 778;
            }
            if ((c[pos] == 'O') && (c[pos + 1] == 'E')) {
                return 1014;
            }
            if ((c[pos] == 'a') && (c[pos + 1] == 'e')) {
                return 722;
            }
            if ((c[pos] == 'A') && (c[pos + 1] == 'E')) {
                return 903;
            }
            if ((c[pos] == 's') && (c[pos + 1] == 's')) {
                return 500;
            }
        }
        return BstWidthCalculator.getCharWidth(c[pos]);
    }

    public static int getCharWidth(char c) {
        if ((c >= 0) && (c < 128)) {
            return BstWidthCalculator.widths[c];
        } else {
            return 0;
        }
    }

    public static int width(String toMeasure) {
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
        while (i < n) {
            if (c[i] == '{') {
                braceLevel++;
                if ((braceLevel == 1) && ((i + 1) < n) && (c[i + 1] == '\\')) {
                    i++; // skip brace
                    while ((i < n) && (braceLevel > 0)) {
                        i++; // skip backslash

                        int afterBackslash = i;
                        while ((i < n) && Character.isLetter(c[i])) {
                            i++;
                        }
                        if ((i < n) && (i == afterBackslash)) {
                            i++; // Skip non-alpha control seq
                        } else {
                            if (BstCaseChanger.findSpecialChar(c, afterBackslash).isPresent()) {
                                result += BstWidthCalculator.getSpecialCharWidth(c, afterBackslash);
                            }
                        }
                        while ((i < n) && Character.isWhitespace(c[i])) {
                            i++;
                        }
                        while ((i < n) && (braceLevel > 0) && (c[i] != '\\')) {
                            if (c[i] == '}') {
                                braceLevel--;
                            } else if (c[i] == '{') {
                                braceLevel++;
                            } else {
                                result += BstWidthCalculator.getCharWidth(c[i]);
                            }
                            i++;
                        }
                    }
                    continue;
                }
            } else if (c[i] == '}') {
                if (braceLevel > 0) {
                    braceLevel--;
                } else {
                    LOGGER.warn("Too many closing braces in string: " + toMeasure);
                }
            }
            result += BstWidthCalculator.getCharWidth(c[i]);
            i++;
        }
        if (braceLevel > 0) {
            LOGGER.warn("No enough closing braces in string: " + toMeasure);
        }
        return result;
    }
}
