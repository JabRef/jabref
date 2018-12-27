package org.jabref.logic.bst;

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
public class BibtexWidth {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibtexWidth.class);

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
        if (BibtexWidth.widths == null) {
            BibtexWidth.widths = new int[128];

            for (int i = 0; i < 128; i++) {
                BibtexWidth.widths[i] = 0;
            }
            BibtexWidth.widths[32] = 278;
            BibtexWidth.widths[33] = 278;
            BibtexWidth.widths[34] = 500;
            BibtexWidth.widths[35] = 833;
            BibtexWidth.widths[36] = 500;
            BibtexWidth.widths[37] = 833;
            BibtexWidth.widths[38] = 778;
            BibtexWidth.widths[39] = 278;
            BibtexWidth.widths[40] = 389;
            BibtexWidth.widths[41] = 389;
            BibtexWidth.widths[42] = 500;
            BibtexWidth.widths[43] = 778;
            BibtexWidth.widths[44] = 278;
            BibtexWidth.widths[45] = 333;
            BibtexWidth.widths[46] = 278;
            BibtexWidth.widths[47] = 500;
            BibtexWidth.widths[48] = 500;
            BibtexWidth.widths[49] = 500;
            BibtexWidth.widths[50] = 500;
            BibtexWidth.widths[51] = 500;
            BibtexWidth.widths[52] = 500;
            BibtexWidth.widths[53] = 500;
            BibtexWidth.widths[54] = 500;
            BibtexWidth.widths[55] = 500;
            BibtexWidth.widths[56] = 500;
            BibtexWidth.widths[57] = 500;
            BibtexWidth.widths[58] = 278;
            BibtexWidth.widths[59] = 278;
            BibtexWidth.widths[60] = 278;
            BibtexWidth.widths[61] = 778;
            BibtexWidth.widths[62] = 472;
            BibtexWidth.widths[63] = 472;
            BibtexWidth.widths[64] = 778;
            BibtexWidth.widths[65] = 750;
            BibtexWidth.widths[66] = 708;
            BibtexWidth.widths[67] = 722;
            BibtexWidth.widths[68] = 764;
            BibtexWidth.widths[69] = 681;
            BibtexWidth.widths[70] = 653;
            BibtexWidth.widths[71] = 785;
            BibtexWidth.widths[72] = 750;
            BibtexWidth.widths[73] = 361;
            BibtexWidth.widths[74] = 514;
            BibtexWidth.widths[75] = 778;
            BibtexWidth.widths[76] = 625;
            BibtexWidth.widths[77] = 917;
            BibtexWidth.widths[78] = 750;
            BibtexWidth.widths[79] = 778;
            BibtexWidth.widths[80] = 681;
            BibtexWidth.widths[81] = 778;
            BibtexWidth.widths[82] = 736;
            BibtexWidth.widths[83] = 556;
            BibtexWidth.widths[84] = 722;
            BibtexWidth.widths[85] = 750;
            BibtexWidth.widths[86] = 750;
            BibtexWidth.widths[87] = 1028;
            BibtexWidth.widths[88] = 750;
            BibtexWidth.widths[89] = 750;
            BibtexWidth.widths[90] = 611;
            BibtexWidth.widths[91] = 278;
            BibtexWidth.widths[92] = 500;
            BibtexWidth.widths[93] = 278;
            BibtexWidth.widths[94] = 500;
            BibtexWidth.widths[95] = 278;
            BibtexWidth.widths[96] = 278;
            BibtexWidth.widths[97] = 500;
            BibtexWidth.widths[98] = 556;
            BibtexWidth.widths[99] = 444;
            BibtexWidth.widths[100] = 556;
            BibtexWidth.widths[101] = 444;
            BibtexWidth.widths[102] = 306;
            BibtexWidth.widths[103] = 500;
            BibtexWidth.widths[104] = 556;
            BibtexWidth.widths[105] = 278;
            BibtexWidth.widths[106] = 306;
            BibtexWidth.widths[107] = 528;
            BibtexWidth.widths[108] = 278;
            BibtexWidth.widths[109] = 833;
            BibtexWidth.widths[110] = 556;
            BibtexWidth.widths[111] = 500;
            BibtexWidth.widths[112] = 556;
            BibtexWidth.widths[113] = 528;
            BibtexWidth.widths[114] = 392;
            BibtexWidth.widths[115] = 394;
            BibtexWidth.widths[116] = 389;
            BibtexWidth.widths[117] = 556;
            BibtexWidth.widths[118] = 528;
            BibtexWidth.widths[119] = 722;
            BibtexWidth.widths[120] = 528;
            BibtexWidth.widths[121] = 528;
            BibtexWidth.widths[122] = 444;
            BibtexWidth.widths[123] = 500;
            BibtexWidth.widths[124] = 1000;
            BibtexWidth.widths[125] = 500;
            BibtexWidth.widths[126] = 500;
        }
    }

    private BibtexWidth() {
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
        return BibtexWidth.getCharWidth(c[pos]);
    }

    public static int getCharWidth(char c) {

        if ((c >= 0) && (c < 128)) {
            return BibtexWidth.widths[c];
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
                            if (BibtexCaseChanger.findSpecialChar(c, afterBackslash).isPresent()) {
                                result += BibtexWidth.getSpecialCharWidth(c, afterBackslash);
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
                                result += BibtexWidth.getCharWidth(c[i]);
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
            result += BibtexWidth.getCharWidth(c[i]);
            i++;
        }
        if (braceLevel > 0) {
            LOGGER.warn("No enough closing braces in string: " + toMeasure);
        }
        return result;
    }
}
