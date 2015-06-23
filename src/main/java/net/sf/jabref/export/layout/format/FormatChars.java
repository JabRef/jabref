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
package net.sf.jabref.export.layout.format;

import net.sf.jabref.Globals;
import net.sf.jabref.Util;
import net.sf.jabref.export.layout.LayoutFormatter;

import java.util.HashMap;

/**
 * This formatter converts LaTeX character sequences their equicalent unicode characters,
 * and removes other LaTeX commands without handling them.
 */
public class FormatChars implements LayoutFormatter {

    private static final HashMap<String, String> CHARS = new HashMap<String, String>();

    static {
        FormatChars.CHARS.put("`A", "À"); // #192
        FormatChars.CHARS.put("'A", "�?"); // #193
        FormatChars.CHARS.put("^A", "Â"); // #194
        FormatChars.CHARS.put("~A", "Ã"); // #195
        FormatChars.CHARS.put("\"A", "Ä"); // #196
        FormatChars.CHARS.put("AA", "Å"); // #197
        FormatChars.CHARS.put("AE", "Æ"); // #198
        FormatChars.CHARS.put("cC", "Ç"); // #199
        FormatChars.CHARS.put("`E", "È"); // #200
        FormatChars.CHARS.put("'E", "É"); // #201
        FormatChars.CHARS.put("^E", "Ê"); // #202
        FormatChars.CHARS.put("\"E", "Ë"); // #203
        FormatChars.CHARS.put("`I", "Ì"); // #204
        FormatChars.CHARS.put("'I", "�?"); // #205
        FormatChars.CHARS.put("^I", "Î"); // #206
        FormatChars.CHARS.put("\"I", "�?"); // #207
        FormatChars.CHARS.put("DH", "�?"); // #208
        FormatChars.CHARS.put("~N", "Ñ"); // #209
        FormatChars.CHARS.put("`O", "Ò"); // #210
        FormatChars.CHARS.put("'O", "Ó"); // #211
        FormatChars.CHARS.put("^O", "Ô"); // #212
        FormatChars.CHARS.put("~O", "Õ"); // #213
        FormatChars.CHARS.put("\"O", "Ö"); // #214
        // According to ISO 8859-1 the "\times" symbol should be placed here
        // (#215).
        // Omitting this, because it is a mathematical symbol.
        FormatChars.CHARS.put("O", "Ø"); // #216
        FormatChars.CHARS.put("`U", "Ù"); // #217
        FormatChars.CHARS.put("'U", "Ú"); // #218
        FormatChars.CHARS.put("^U", "Û"); // #219
        FormatChars.CHARS.put("\"U", "Ü"); // #220
        FormatChars.CHARS.put("'Y", "�?"); // #221
        FormatChars.CHARS.put("TH", "Þ"); // #222
        FormatChars.CHARS.put("ss", "ß"); // #223
        FormatChars.CHARS.put("`a", "à"); // #224
        FormatChars.CHARS.put("'a", "á"); // #225
        FormatChars.CHARS.put("^a", "â"); // #226
        FormatChars.CHARS.put("~a", "ã"); // #227
        FormatChars.CHARS.put("\"a", "ä"); // #228
        FormatChars.CHARS.put("aa", "å"); // #229
        FormatChars.CHARS.put("ae", "æ"); // #230
        FormatChars.CHARS.put("cc", "ç"); // #231
        FormatChars.CHARS.put("`e", "è"); // #232
        FormatChars.CHARS.put("'e", "é"); // #233
        FormatChars.CHARS.put("^e", "ê"); // #234
        FormatChars.CHARS.put("\"e", "ë"); // #235
        FormatChars.CHARS.put("`i", "ì"); // #236
        FormatChars.CHARS.put("'i", "í"); // #237
        FormatChars.CHARS.put("^i", "î"); // #238
        FormatChars.CHARS.put("\"i", "ï"); // #239
        FormatChars.CHARS.put("dh", "ð"); // #240
        FormatChars.CHARS.put("~n", "ñ"); // #241
        FormatChars.CHARS.put("`o", "ò"); // #242
        FormatChars.CHARS.put("'o", "ó"); // #243
        FormatChars.CHARS.put("^o", "ô"); // #244
        FormatChars.CHARS.put("~o", "õ"); // #245
        FormatChars.CHARS.put("\"o", "ö"); // #246
        // According to ISO 8859-1 the "\div" symbol should be placed here
        // (#247).
        // Omitting this, because it is a mathematical symbol.
        FormatChars.CHARS.put("o", "ø"); // #248
        FormatChars.CHARS.put("`u", "ù"); // #249
        FormatChars.CHARS.put("'u", "ú"); // #250
        FormatChars.CHARS.put("^u", "û"); // #251
        FormatChars.CHARS.put("\"u", "ü"); // #252
        FormatChars.CHARS.put("'y", "ý"); // #253
        FormatChars.CHARS.put("th", "þ"); // #254
        FormatChars.CHARS.put("\"y", "ÿ"); // #255

        // HTML special characters without names (UNICODE Latin Extended-A),
        // indicated by UNICODE number
        FormatChars.CHARS.put("=A", "Ā"); // "Amacr"
        FormatChars.CHARS.put("=a", "�?"); // "amacr"
        FormatChars.CHARS.put("uA", "Ă"); // "Abreve"
        FormatChars.CHARS.put("ua", "ă"); // "abreve"
        FormatChars.CHARS.put("kA", "Ą"); // "Aogon"
        FormatChars.CHARS.put("ka", "ą"); // "aogon"
        FormatChars.CHARS.put("'C", "Ć"); // "Cacute"
        FormatChars.CHARS.put("'c", "ć"); // "cacute"
        FormatChars.CHARS.put("^C", "Ĉ"); // "Ccirc"
        FormatChars.CHARS.put("^c", "ĉ"); // "ccirc"
        FormatChars.CHARS.put(".C", "Ċ"); // "Cdot"
        FormatChars.CHARS.put(".c", "ċ"); // "cdot"
        FormatChars.CHARS.put("vC", "Č"); // "Ccaron"
        FormatChars.CHARS.put("vc", "�?"); // "ccaron"
        FormatChars.CHARS.put("vD", "Ď"); // "Dcaron"
        // Symbol #271 (d�) has no special Latex command
        FormatChars.CHARS.put("DJ", "�?"); // "Dstrok"
        FormatChars.CHARS.put("dj", "đ"); // "dstrok"
        FormatChars.CHARS.put("=E", "Ē"); // "Emacr"
        FormatChars.CHARS.put("=e", "ē"); // "emacr"
        FormatChars.CHARS.put("uE", "Ĕ"); // "Ebreve"
        FormatChars.CHARS.put("ue", "ĕ"); // "ebreve"
        FormatChars.CHARS.put(".E", "Ė"); // "Edot"
        FormatChars.CHARS.put(".e", "ė"); // "edot"
        FormatChars.CHARS.put("kE", "Ę"); // "Eogon"
        FormatChars.CHARS.put("ke", "ę"); // "eogon"
        FormatChars.CHARS.put("vE", "Ě"); // "Ecaron"
        FormatChars.CHARS.put("ve", "ě"); // "ecaron"
        FormatChars.CHARS.put("^G", "Ĝ"); // "Gcirc"
        FormatChars.CHARS.put("^g", "�?"); // "gcirc"
        FormatChars.CHARS.put("uG", "Ğ"); // "Gbreve"
        FormatChars.CHARS.put("ug", "ğ"); // "gbreve"
        FormatChars.CHARS.put(".G", "Ġ"); // "Gdot"
        FormatChars.CHARS.put(".g", "ġ"); // "gdot"
        FormatChars.CHARS.put("cG", "Ģ"); // "Gcedil"
        FormatChars.CHARS.put("'g", "ģ"); // "gacute"
        FormatChars.CHARS.put("^H", "Ĥ"); // "Hcirc"
        FormatChars.CHARS.put("^h", "ĥ"); // "hcirc"
        FormatChars.CHARS.put("Hstrok", "Ħ"); // "Hstrok"
        FormatChars.CHARS.put("hstrok", "ħ"); // "hstrok"
        FormatChars.CHARS.put("~I", "Ĩ"); // "Itilde"
        FormatChars.CHARS.put("~i", "ĩ"); // "itilde"
        FormatChars.CHARS.put("=I", "Ī"); // "Imacr"
        FormatChars.CHARS.put("=i", "ī"); // "imacr"
        FormatChars.CHARS.put("uI", "Ĭ"); // "Ibreve"
        FormatChars.CHARS.put("ui", "ĭ"); // "ibreve"
        FormatChars.CHARS.put("kI", "Į"); // "Iogon"
        FormatChars.CHARS.put("ki", "į"); // "iogon"
        FormatChars.CHARS.put(".I", "İ"); // "Idot"
        FormatChars.CHARS.put("i", "ı"); // "inodot"
        // Symbol #306 (IJ) has no special Latex command
        // Symbol #307 (ij) has no special Latex command
        FormatChars.CHARS.put("^J", "Ĵ"); // "Jcirc"
        FormatChars.CHARS.put("^j", "ĵ"); // "jcirc"
        FormatChars.CHARS.put("cK", "Ķ"); // "Kcedil"
        FormatChars.CHARS.put("ck", "ķ"); // "kcedil"
        // Symbol #312 (k) has no special Latex command
        FormatChars.CHARS.put("'L", "Ĺ"); // "Lacute"
        FormatChars.CHARS.put("'l", "ĺ"); // "lacute"
        FormatChars.CHARS.put("cL", "Ļ"); // "Lcedil"
        FormatChars.CHARS.put("cl", "ļ"); // "lcedil"
        // Symbol #317 (L�) has no special Latex command
        // Symbol #318 (l�) has no special Latex command
        FormatChars.CHARS.put("Lmidot", "Ŀ"); // "Lmidot"
        FormatChars.CHARS.put("lmidot", "ŀ"); // "lmidot"
        FormatChars.CHARS.put("L", "�?"); // "Lstrok"
        FormatChars.CHARS.put("l", "ł"); // "lstrok"
        FormatChars.CHARS.put("'N", "Ń"); // "Nacute"
        FormatChars.CHARS.put("'n", "ń"); // "nacute"
        FormatChars.CHARS.put("cN", "Ņ"); // "Ncedil"
        FormatChars.CHARS.put("cn", "ņ"); // "ncedil"
        FormatChars.CHARS.put("vN", "Ň"); // "Ncaron"
        FormatChars.CHARS.put("vn", "ň"); // "ncaron"
        // Symbol #329 (�n) has no special Latex command
        FormatChars.CHARS.put("NG", "Ŋ"); // "ENG"
        FormatChars.CHARS.put("ng", "ŋ"); // "eng"
        FormatChars.CHARS.put("=O", "Ō"); // "Omacr"
        FormatChars.CHARS.put("=o", "�?"); // "omacr"
        FormatChars.CHARS.put("uO", "Ŏ"); // "Obreve"
        FormatChars.CHARS.put("uo", "�?"); // "obreve"
        FormatChars.CHARS.put("HO", "�?"); // "Odblac"
        FormatChars.CHARS.put("Ho", "ő"); // "odblac"
        FormatChars.CHARS.put("OE", "Œ"); // "OElig"
        FormatChars.CHARS.put("oe", "œ"); // "oelig"
        FormatChars.CHARS.put("'R", "Ŕ"); // "Racute"
        FormatChars.CHARS.put("'r", "ŕ"); // "racute"
        FormatChars.CHARS.put("cR", "Ŗ"); // "Rcedil"
        FormatChars.CHARS.put("cr", "ŗ"); // "rcedil"
        FormatChars.CHARS.put("vR", "Ř"); // "Rcaron"
        FormatChars.CHARS.put("vr", "ř"); // "rcaron"
        FormatChars.CHARS.put("'S", "Ś"); // "Sacute"
        FormatChars.CHARS.put("'s", "ś"); // "sacute"
        FormatChars.CHARS.put("^S", "Ŝ"); // "Scirc"
        FormatChars.CHARS.put("^s", "�?"); // "scirc"
        FormatChars.CHARS.put("cS", "Ş"); // "Scedil"
        FormatChars.CHARS.put("cs", "ş"); // "scedil"
        FormatChars.CHARS.put("vS", "Š"); // "Scaron"
        FormatChars.CHARS.put("vs", "š"); // "scaron"
        FormatChars.CHARS.put("cT", "Ţ"); // "Tcedil"
        FormatChars.CHARS.put("ct", "ţ"); // "tcedil"
        FormatChars.CHARS.put("vT", "Ť"); // "Tcaron"
        // Symbol #357 (t�) has no special Latex command
        FormatChars.CHARS.put("Tstrok", "Ŧ"); // "Tstrok"
        FormatChars.CHARS.put("tstrok", "ŧ"); // "tstrok"
        FormatChars.CHARS.put("~U", "Ũ"); // "Utilde"
        FormatChars.CHARS.put("~u", "ũ"); // "utilde"
        FormatChars.CHARS.put("=U", "Ū"); // "Umacr"
        FormatChars.CHARS.put("=u", "ū"); // "umacr"
        FormatChars.CHARS.put("uU", "Ŭ"); // "Ubreve"
        FormatChars.CHARS.put("uu", "ŭ"); // "ubreve"
        FormatChars.CHARS.put("rU", "Ů"); // "Uring"
        FormatChars.CHARS.put("ru", "ů"); // "uring"
        FormatChars.CHARS.put("HU", "ů"); // "Odblac"
        FormatChars.CHARS.put("Hu", "ű"); // "odblac"
        FormatChars.CHARS.put("kU", "Ų"); // "Uogon"
        FormatChars.CHARS.put("ku", "ų"); // "uogon"
        FormatChars.CHARS.put("^W", "Ŵ"); // "Wcirc"
        FormatChars.CHARS.put("^w", "ŵ"); // "wcirc"
        FormatChars.CHARS.put("^Y", "Ŷ"); // "Ycirc"
        FormatChars.CHARS.put("^y", "ŷ"); // "ycirc"
        FormatChars.CHARS.put("\"Y", "Ÿ"); // "Yuml"
        FormatChars.CHARS.put("'Z", "Ź"); // "Zacute"
        FormatChars.CHARS.put("'z", "ź"); // "zacute"
        FormatChars.CHARS.put(".Z", "Ż"); // "Zdot"
        FormatChars.CHARS.put(".z", "ż"); // "zdot"
        FormatChars.CHARS.put("vZ", "Ž"); // "Zcaron"
        FormatChars.CHARS.put("vz", "ž"); // "zcaron"
        // Symbol #383 (f) has no special Latex command
        FormatChars.CHARS.put("%", "%"); // percent sign
    }


    @Override
    public String format(String field) {
        int i;
        field = field.replaceAll("&|\\\\&", "&amp;").replaceAll("[\\n]{1,}", "<p>");

        StringBuffer sb = new StringBuffer();
        StringBuffer currentCommand = null;

        char c;
        boolean escaped = false, incommand = false;

        for (i = 0; i < field.length(); i++) {
            c = field.charAt(i);
            if (escaped && (c == '\\')) {
                sb.append('\\');
                escaped = false;
            } else if (c == '\\') {
                if (incommand) {
                    /* Close Command */
                    String command = currentCommand.toString();
                    Object result = FormatChars.CHARS.get(command);
                    if (result != null) {
                        sb.append((String) result);
                    } else {
                        sb.append(command);
                    }
                }
                escaped = true;
                incommand = true;
                currentCommand = new StringBuffer();
            } else if (!incommand && ((c == '{') || (c == '}'))) {
                // Swallow the brace.
            } else if (Character.isLetter(c) || (c == '%')
                    || (Globals.SPECIAL_COMMAND_CHARS.contains(String.valueOf(c)))) {
                escaped = false;

                if (!incommand) {
                    sb.append(c);
                } else {
                    currentCommand.append(c);
                    testCharCom: if ((currentCommand.length() == 1)
                            && (Globals.SPECIAL_COMMAND_CHARS.contains(currentCommand.toString()))) {
                        // This indicates that we are in a command of the type
                        // \^o or \~{n}
                        if (i >= (field.length() - 1)) {
                            break testCharCom;
                        }

                        String command = currentCommand.toString();
                        i++;
                        c = field.charAt(i);
                        // System.out.println("next: "+(char)c);
                        String combody;
                        if (c == '{') {
                            String part = Util.getPart(field, i, false);
                            i += part.length();
                            combody = part;
                        } else {
                            combody = field.substring(i, i + 1);
                            // System.out.println("... "+combody);
                        }
                        Object result = FormatChars.CHARS.get(command + combody);

                        if (result != null) {
                            sb.append((String) result);
                        }

                        incommand = false;
                        escaped = false;
                    } else {
                        //	Are we already at the end of the string?
                        if ((i + 1) == field.length()) {
                            String command = currentCommand.toString();
                            Object result = FormatChars.CHARS.get(command);
                            /* If found, then use translated version. If not,
                             * then keep
                             * the text of the parameter intact.
                             */
                            if (result != null) {
                                sb.append((String) result);
                            } else {
                                sb.append(command);
                            }

                        }
                    }
                }
            } else {
                String argument;

                if (!incommand) {
                    sb.append(c);
                } else if (Character.isWhitespace(c) || (c == '{') || (c == '}')) {
                    // First test if we are already at the end of the string.
                    // if (i >= field.length()-1)
                    // break testContent;

                    String command = currentCommand.toString();

                    if (c == '{') {
                        String part = Util.getPart(field, i, true);
                        i += part.length();
                        argument = part;
                        if (argument != null) {
                            // handle common case of general latex command
                            Object result = FormatChars.CHARS.get(command + argument);
                            // System.out.print("command: "+command+", arg: "+argument);
                            // System.out.print(", result: ");
                            // If found, then use translated version. If not, then keep
                            // the
                            // text of the parameter intact.
                            if (result != null) {
                                sb.append((String) result);
                            } else {
                                sb.append(argument);
                            }
                        }
                    } else if (c == '}') {
                        // This end brace terminates a command. This can be the case in
                        // constructs like {\aa}. The correct behaviour should be to
                        // substitute the evaluated command and swallow the brace:
                        Object result = FormatChars.CHARS.get(command);
                        if (result != null) {
                            sb.append((String) result);
                        } else {
                            // If the command is unknown, just print it:
                            sb.append(command);
                        }
                    } else {
                        Object result = FormatChars.CHARS.get(command);
                        if (result != null) {
                            sb.append((String) result);
                        } else {
                            sb.append(command);
                        }
                        sb.append(' ');
                    }
                }/* else if (c == '}') {
                    System.out.printf("com term by }: '%s'\n", currentCommand.toString());

                    argument = "";
                 }*/else {
                    /*
                     * TODO: this point is reached, apparently, if a command is
                     * terminated in a strange way, such as with "$\omega$".
                     * Also, the command "\&" causes us to get here. The former
                     * issue is maybe a little difficult to address, since it
                     * involves the LaTeX math mode. We don't have a complete
                     * LaTeX parser, so maybe it's better to ignore these
                     * commands?
                     */
                }

                incommand = false;
                escaped = false;
            }
        }

        return sb.toString();
    }

}
