package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.StringInt;
import org.jabref.logic.util.strings.RtfCharMap;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            return "A";
        }
        if (((224 <= c) && (c <= 229)) || (c == 257) || (c == 259) || (c == 261)) {
            return "a";
        }
        if ((199 == c) || (262 == c) || (264 == c) || (266 == c) || (268 == c)) {
            return "C";
        }
        if ((231 == c) || (263 == c) || (265 == c) || (267 == c) || (269 == c)) {
            return "c";
        }
        if ((208 == c) || (272 == c)) {
            return "D";
        }
        if ((240 == c) || (273 == c)) {
            return "d";
        }
        if (((200 <= c) && (c <= 203)) || (274 == c) || (276 == c) || (278 == c) || (280 == c) || (282 == c)) {
            return "E";
        }
        if (((232 <= c) && (c <= 235)) || (275 == c) || (277 == c) || (279 == c) || (281 == c) || (283 == c)) {
            return "e";
        }
        if (((284 == c) || (286 == c)) || (288 == c) || (290 == c) || (330 == c)) {
            return "G";
        }
        if ((285 == c) || (287 == c) || (289 == c) || (291 == c) || (331 == c)) {
            return "g";
        }
        if ((292 == c) || (294 == c)) {
            return "H";
        }
        if ((293 == c) || (295 == c)) {
            return "h";
        }
        if (((204 <= c) && (c <= 207)) || (296 == c) || (298 == c) || (300 == c) || (302 == c) || (304 == c)) {
            return "I";
        }
        if (((236 <= c) && (c <= 239)) || (297 == c) || (299 == c) || (301 == c) || (303 == c)) {
            return "i";
        }
        if (308 == c) {
            return "J";
        }
        if (309 == c) {
            return "j";
        }
        if (310 == c) {
            return "K";
        }
        if (311 == c) {
            return "k";
        }
        if ((313 == c) || (315 == c) || (319 == c)) {
            return "L";
        }
        if ((314 == c) || (316 == c) || (320 == c) || (322 == c)) {
            return "l";
        }
        if ((209 == c) || (323 == c) || (325 == c) || (327 == c)) {
            return "N";
        }
        if ((241 == c) || (324 == c) || (326 == c) || (328 == c)) {
            return "n";
        }
        if (((210 <= c) && (c <= 214)) || (c == 216) || (332 == c) || (334 == c)) {
            return "O";
        }
        if (((242 <= c) && (c <= 248) && (247 != c)) || (333 == c) || (335 == c)) {
            return "o";
        }
        if ((340 == c) || (342 == c) || (344 == c)) {
            return "R";
        }
        if ((341 == c) || (343 == c) || (345 == c)) {
            return "r";
        }
        if ((346 == c) || (348 == c) || (350 == c) || (352 == c)) {
            return "S";
        }
        if ((347 == c) || (349 == c) || (351 == c) || (353 == c)) {
            return "s";
        }
        if ((354 == c) || (356 == c) || (358 == c)) {
            return "T";
        }
        if ((355 == c) || (359 == c)) {
            return "t";
        }
        if (((217 <= c) && (c <= 220)) || (360 == c) || (362 == c) || (364 == c) || (366 == c) || (370 == c)) {
            return "U";
        }
        if (((249 <= c) && (c <= 251)) || (361 == c) || (363 == c) || (365 == c) || (367 == c) || (371 == c)) {
            return "u";
        }
        if (372 == c) {
            return "W";
        }
        if (373 == c) {
            return "w";
        }
        if ((374 == c) || (376 == c) || (221 == c)) {
            return "Y";
        }
        if ((375 == c) || (255 == c)) {
            return "y";
        }
        if ((377 == c) || (379 == c) || (381 == c)) {
            return "Z";
        }
        if ((378 == c) || (380 == c) || (382 == c)) {
            return "z";
        }
        if (198 == c) {
            return "AE";
        }
        if (230 == c) {
            return "ae";
        }
        if (338 == c) {
            return "OE";
        }
        if (339 == c) {
            return "oe";
        }
        if (222 == c) {
            return "TH";
        }
        if (223 == c) {
            return "ss";
        }
        if (161 == c) {
            return "!";
        }
        return "?";
    }
}
