package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.StringInt;
import org.jabref.logic.util.strings.RtfCharMap;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * Transform a LaTeX-String to RTF.
 *
 * This method will:
 *
 *   1.) Remove LaTeX-Command sequences.
 *
 *   2.) Replace LaTeX-Special chars with RTF aquivalents.
 *
 *   3.) Replace emph and textit and textbf with their RTF replacements.
 *
 *   4.) Take special care to save all unicode characters correctly.
 *
 *   5.) Replace --- by \emdash and -- by \endash.
 */
public class RTFChars implements LayoutFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LayoutFormatter.class);

    private static final RtfCharMap RTF_CHARS = new RtfCharMap();

    private static boolean[] visited = new boolean[91];

    @Override
    public String format(String field) {
        StringBuilder sb = new StringBuilder("");
        StringBuilder currentCommand = null;
        boolean escaped = false;
        boolean incommand = false;
        for (int i = 0; i < field.length(); i++) {

            char c = field.charAt(i);

            if (escaped && (c == '\\')) {
                sb.append('\\');
                escaped = false;
            }

            else if (c == '\\') {
                escaped = true;
                incommand = true;
                currentCommand = new StringBuilder();
            } else if (!incommand && ((c == '{') || (c == '}'))) {
                // Swallow the brace.
            } else if (Character.isLetter(c)
                    || StringUtil.SPECIAL_COMMAND_CHARS.contains(String.valueOf(c))) {
                escaped = false;
                if (incommand) {
                    // Else we are in a command, and should not keep the letter.
                    currentCommand.append(c);
                    testCharCom: if ((currentCommand.length() == 1)
                            && StringUtil.SPECIAL_COMMAND_CHARS.contains(currentCommand.toString())) {
                        // This indicates that we are in a command of the type
                        // \^o or \~{n}
                        if (i >= (field.length() - 1)) {
                            break testCharCom;
                        }

                        String command = currentCommand.toString();
                        i++;
                        c = field.charAt(i);
                        String combody;
                        if (c == '{') {
                            StringInt part = getPart(field, i, true);
                            i += part.i;
                            combody = part.s;
                        } else {
                            combody = field.substring(i, i + 1);
                        }

                        String result = RTF_CHARS.get(command + combody);

                        if (result != null) {
                            sb.append(result);
                        }

                        incommand = false;
                        escaped = false;

                    }
                } else {
                    sb.append(c);
                }

            } else {
                testContent: if (!incommand || (!Character.isWhitespace(c) && (c != '{') && (c != '}'))) {
                    sb.append(c);
                } else {
                    assert incommand;

                    // First test for braces that may be part of a LaTeX command:
                    if ((c == '{') && (currentCommand.length() == 0)) {
                        // We have seen something like \{, which is probably the start
                        // of a command like \{aa}. Swallow the brace.
                        continue;
                    } else if ((c == '}') && (currentCommand.length() > 0)) {
                        // Seems to be the end of a command like \{aa}. Look it up:
                        String command = currentCommand.toString();
                        String result = RTF_CHARS.get(command);
                        if (result != null) {
                            sb.append(result);
                        }
                        incommand = false;
                        escaped = false;
                        continue;
                    }

                    // Then look for italics etc.,
                    // but first check if we are already at the end of the string.
                    if (i >= (field.length() - 1)) {
                        break testContent;
                    }

                    if (((c == '{') || (c == ' ')) && (currentCommand.length() > 0)) {
                        String command = currentCommand.toString();
                        // Then test if we are dealing with a italics or bold
                        // command. If so, handle.
                        if ("em".equals(command) || "emph".equals(command) || "textit".equals(command)
                                || "it".equals(command)) {
                            StringInt part = getPart(field, i, c == '{');
                            i += part.i;
                            sb.append("{\\i ").append(part.s).append('}');
                        } else if ("textbf".equals(command) || "bf".equals(command)) {
                            StringInt part = getPart(field, i, c == '{');
                            i += part.i;
                            sb.append("{\\b ").append(part.s).append('}');
                        } else {
                            LOGGER.info("Unknown command " + command);
                        }
                        if (c == ' ') {
                            // command was separated with the content by ' '
                            // We have to add the space a
                        }
                    } else {
                        sb.append(c);
                    }

                }
                incommand = false;
                escaped = false;
            }
        }

        char[] chars = sb.toString().toCharArray();
        sb = new StringBuilder();

        for (char c : chars) {
            if (c < 128) {
                sb.append(c);
            } else {
                sb.append("\\u").append((long) c).append(transformSpecialCharacter(c));
            }
        }

        return sb.toString().replace("---", "{\\emdash}").replace("--", "{\\endash}").replace("``", "{\\ldblquote}")
                .replace("''", "{\\rdblquote}");
    }

    /**
     * @param text the text to extract the part from
     * @param i the position to start
     * @param commandNestedInBraces true if the command is nested in braces (\emph{xy}), false if spaces are sued (\emph xy)
     * @return a tuple of number of added characters and the extracted part
     */
    private StringInt getPart(String text, int i, boolean commandNestedInBraces) {
        char c;
        int count = 0;
        int icount = i;
        StringBuilder part = new StringBuilder();
        loop: while ((count >= 0) && (icount < text.length())) {
            icount++;
            c = text.charAt(icount);
            switch (c) {
            case '}':
                count--;
                break;
            case '{':
                count++;
                break;
            case ' ':
                if (!commandNestedInBraces) {
                    // in any case, a space terminates the loop
                    break loop;
                }
                break;
            default:
                break;
            }
            part.append(c);
        }
        String res = part.toString();
        // the wrong "}" at the end is removed by "format(res)"
        return new StringInt(format(res), part.length());
    }

    /**
     * This method transforms the unicode of a special character into its base character: 233 (é) - > e
     * @param c long
     * @return returns the basic character of the given unicode
     */
    private String transformSpecialCharacter(long c) {
        if (((192 <= c) && (c <= 197)) || (c == 256) || (c == 258) || (c == 260)) {            
            branchCoverage(0);
            return "A";
        } else { 
            branchCoverage(46); 
        }
        if (((224 <= c) && (c <= 229)) || (c == 257) || (c == 259) || (c == 261)) {
            branchCoverage(1);
            return "a";
        } else { 
            branchCoverage(47); 
        }
        if ((199 == c) || (262 == c) || (264 == c) || (266 == c) || (268 == c)) {
            branchCoverage(2);
            return "C";
        }
        else { 
            branchCoverage(48); 
        }
        if ((231 == c) || (263 == c) || (265 == c) || (267 == c) || (269 == c)) {
            branchCoverage(3);
            return "c";
        }
        else { 
            branchCoverage(49); 
        }
        if ((208 == c) || (272 == c)) {
            branchCoverage(4);
            return "D";
        }
        else { 
            branchCoverage(50); 
        }
        if ((240 == c) || (273 == c)) {
            branchCoverage(5);
            return "d";
        } else { 
            branchCoverage(51); 
        }
        if (((200 <= c) && (c <= 203)) || (274 == c) || (276 == c) || (278 == c) || (280 == c) || (282 == c)) {
            branchCoverage(6);
            return "E";
        } else { 
            branchCoverage(52); 
        }
        if (((232 <= c) && (c <= 235)) || (275 == c) || (277 == c) || (279 == c) || (281 == c) || (283 == c)) {
            branchCoverage(7);
            return "e";
        } else { 
            branchCoverage(53); 
        }        
        if (((284 == c) || (286 == c)) || (288 == c) || (290 == c) || (330 == c)) {
            branchCoverage(8);
            return "G";
        } else { 
            branchCoverage(54); 
        }
        if ((285 == c) || (287 == c) || (289 == c) || (291 == c) || (331 == c)) {
            branchCoverage(9);
            return "g";
        } else { 
            branchCoverage(55); 
        }
        if ((292 == c) || (294 == c)) {
            branchCoverage(10);            
            return "H";
        } else { 
            branchCoverage(56); 
        }
        if ((293 == c) || (295 == c)) {
            branchCoverage(11);
            return "h";
        }  else { 
            branchCoverage(57); 
        }
        if (((204 <= c) && (c <= 207)) || (296 == c) || (298 == c) || (300 == c) || (302 == c) || (304 == c)) {
            branchCoverage(12);
            return "I";
        } else { 
            branchCoverage(58); 
        }
        if (((236 <= c) && (c <= 239)) || (297 == c) || (299 == c) || (301 == c) || (303 == c)) {
            branchCoverage(13);
            return "i";
        } else { 
            branchCoverage(59); 
        }
        if (308 == c) {
            branchCoverage(14);
            return "J";
        } else { 
            branchCoverage(60); 
        }
        if (309 == c) {
            branchCoverage(15);
            return "j";
        } else { 
            branchCoverage(61); 
        }
        if (310 == c) {
            branchCoverage(16);
            return "K";
        } else { 
            branchCoverage(62); 
        }
        if (311 == c) {
            branchCoverage(17);
            return "k";
        } else { 
            branchCoverage(63); 
        }
        if ((313 == c) || (315 == c) || (319 == c)) {
            branchCoverage(18);
            return "L";
        } else { 
            branchCoverage(64); 
        }
        if ((314 == c) || (316 == c) || (320 == c) || (322 == c)) {
            branchCoverage(19);
            return "l";
        } else { 
            branchCoverage(65); 
        }
        if ((209 == c) || (323 == c) || (325 == c) || (327 == c)) {
            branchCoverage(20);
            return "N";
        } else { 
            branchCoverage(66); 
        }
        if ((241 == c) || (324 == c) || (326 == c) || (328 == c)) {
            branchCoverage(21);
            return "n";
        } else { 
            branchCoverage(67); 
        }
        if (((210 <= c) && (c <= 214)) || (c == 216) || (332 == c) || (334 == c)) {
            branchCoverage(22);
            return "O";
        } else { 
            branchCoverage(68); 
        }
        if (((242 <= c) && (c <= 248) && (247 != c)) || (333 == c) || (335 == c)) {
            branchCoverage(23);
            return "o";
        } else { 
            branchCoverage(69); 
        }
        if ((340 == c) || (342 == c) || (344 == c)) {
            branchCoverage(24);
            return "R";
        } else { 
            branchCoverage(70); 
        }
        if ((341 == c) || (343 == c) || (345 == c)) {
            branchCoverage(25);
            return "r";
        } else { 
            branchCoverage(71); 
        }
        if ((346 == c) || (348 == c) || (350 == c) || (352 == c)) {
            branchCoverage(26);
            return "S";
        } else { 
            branchCoverage(72); 
        }
        if ((347 == c) || (349 == c) || (351 == c) || (353 == c)) {
            branchCoverage(27);
            return "s";
        } else { 
            branchCoverage(73); 
        }
        if ((354 == c) || (356 == c) || (358 == c)) {
            branchCoverage(28);
            return "T";
        } else { 
            branchCoverage(74); 
        }
        if ((355 == c) || (359 == c)) {
            branchCoverage(29);
            return "t";
        } else { 
            branchCoverage(75); 
        }
        if (((217 <= c) && (c <= 220)) || (360 == c) || (362 == c) || (364 == c) || (366 == c) || (370 == c)) {
            branchCoverage(30);
            return "U";
        } else { 
            branchCoverage(76); 
        }
        if (((249 <= c) && (c <= 251)) || (361 == c) || (363 == c) || (365 == c) || (367 == c) || (371 == c)) {
            branchCoverage(31);
            return "u";
        } else { 
            branchCoverage(77); 
        }
        if (372 == c) {
            branchCoverage(32);
            return "W";
        } else { 
            branchCoverage(78); 
        }
        if (373 == c) {
            branchCoverage(33);
            return "w";
        } else { 
            branchCoverage(79); 
        }
        if ((374 == c) || (376 == c) || (221 == c)) {
            branchCoverage(34);
            return "Y";
        } else { 
            branchCoverage(80); 
        }
        if ((375 == c) || (255 == c)) {
            branchCoverage(35);
            return "y";
        } else { 
            branchCoverage(81); 
        }
        if ((377 == c) || (379 == c) || (381 == c)) {
            branchCoverage(36);
            return "Z";
        } else { 
            branchCoverage(82); 
        }
        if ((378 == c) || (380 == c) || (382 == c)) {
            branchCoverage(37);
            return "z";
        } else { 
            branchCoverage(83); 
        }
        if (198 == c) {
            branchCoverage(38);
            return "AE";
        } else { 
            branchCoverage(84); 
        }
        if (230 == c) {
            branchCoverage(39);
            return "ae";
        } else { 
            branchCoverage(85); 
        }
        if (338 == c) {
            branchCoverage(40);            
            return "OE";
        } else { 
            branchCoverage(86); 
        }
        if (339 == c) {
            branchCoverage(41);
            return "oe";
        } else { 
            branchCoverage(87); 
        }
        if (222 == c) {
            branchCoverage(42);
            return "TH";
        } else { 
            branchCoverage(88); 
        }
        if (223 == c) {
            branchCoverage(43);
            return "ss";
        } else { 
            branchCoverage(89); 
        }
        if (161 == c) {
            branchCoverage(44);
            return "!";
        } else { 
            branchCoverage(90); 
        }
        branchCoverage(45);
        return "?";
    }

    private static void branchCoverage(int index) {
        visited[index] = true;
        try {
            File f = new File("/tmp/transformSpecialCharacter.txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            double frac = 0;
            for(int i = 0; i < visited.length; ++i) {
                frac += (visited[i] ? 1 : 0);
                bw.write("branch " + i + " was " + (visited[i] ? " visited." : " not visited.") + "\n");
            }
            bw.write("" + frac/visited.length);
            bw.close();
        } catch (Exception exc) {
            System.err.println("ye");
        }	        
    }
}
