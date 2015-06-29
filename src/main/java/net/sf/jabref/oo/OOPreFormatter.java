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
package net.sf.jabref.oo;

import net.sf.jabref.Globals;
import net.sf.jabref.StringUtil;
import net.sf.jabref.export.layout.LayoutFormatter;
import java.util.HashMap;

/**
 * This formatter preprocesses JabRef fields before they are run through the layout of the
 * bibliography style. It handles translation of LaTeX italic/bold commands into HTML tags.
 *
 * @version $Revision: 2568 $ ($Date: 2008-01-15 18:40:26 +0100 (Tue, 15 Jan 2008) $)
 */
public class OOPreFormatter implements LayoutFormatter {

    private static final HashMap<String, String> CHARS = new HashMap<String, String>();

    static {
        // Following character definitions contributed by Ervin Kolenovic:
        // HTML named entities from #192 - #255 (UNICODE Latin-1)
        OOPreFormatter.CHARS.put("`A", "À"); // #192
        OOPreFormatter.CHARS.put("'A", "�?"); // #193
        OOPreFormatter.CHARS.put("^A", "Â"); // #194
        OOPreFormatter.CHARS.put("~A", "Ã"); // #195
        OOPreFormatter.CHARS.put("\"A", "Ä"); // #196
        OOPreFormatter.CHARS.put("AA", "Å"); // #197
        OOPreFormatter.CHARS.put("AE", "Æ"); // #198
        OOPreFormatter.CHARS.put("cC", "Ç"); // #199
        OOPreFormatter.CHARS.put("`E", "È"); // #200
        OOPreFormatter.CHARS.put("'E", "É"); // #201
        OOPreFormatter.CHARS.put("^E", "Ê"); // #202
        OOPreFormatter.CHARS.put("\"E", "Ë"); // #203
        OOPreFormatter.CHARS.put("`I", "Ì"); // #204
        OOPreFormatter.CHARS.put("'I", "�?"); // #205
        OOPreFormatter.CHARS.put("^I", "Î"); // #206
        OOPreFormatter.CHARS.put("\"I", "�?"); // #207
        OOPreFormatter.CHARS.put("DH", "�?"); // #208
        OOPreFormatter.CHARS.put("~N", "Ñ"); // #209
        OOPreFormatter.CHARS.put("`O", "Ò"); // #210
        OOPreFormatter.CHARS.put("'O", "Ó"); // #211
        OOPreFormatter.CHARS.put("^O", "Ô"); // #212
        OOPreFormatter.CHARS.put("~O", "Õ"); // #213
        OOPreFormatter.CHARS.put("\"O", "Ö"); // #214
        // According to ISO 8859-1 the "\times" symbol should be placed here
        // (#215).
        // Omitting this, because it is a mathematical symbol.
        OOPreFormatter.CHARS.put("O", "Ø"); // #216
        OOPreFormatter.CHARS.put("`U", "Ù"); // #217
        OOPreFormatter.CHARS.put("'U", "Ú"); // #218
        OOPreFormatter.CHARS.put("^U", "Û"); // #219
        OOPreFormatter.CHARS.put("\"U", "Ü"); // #220
        OOPreFormatter.CHARS.put("'Y", "�?"); // #221
        OOPreFormatter.CHARS.put("TH", "Þ"); // #222
        OOPreFormatter.CHARS.put("ss", "ß"); // #223
        OOPreFormatter.CHARS.put("`a", "à"); // #224
        OOPreFormatter.CHARS.put("'a", "á"); // #225
        OOPreFormatter.CHARS.put("^a", "â"); // #226
        OOPreFormatter.CHARS.put("~a", "ã"); // #227
        OOPreFormatter.CHARS.put("\"a", "ä"); // #228
        OOPreFormatter.CHARS.put("aa", "å"); // #229
        OOPreFormatter.CHARS.put("ae", "æ"); // #230
        OOPreFormatter.CHARS.put("cc", "ç"); // #231
        OOPreFormatter.CHARS.put("`e", "è"); // #232
        OOPreFormatter.CHARS.put("'e", "é"); // #233
        OOPreFormatter.CHARS.put("^e", "ê"); // #234
        OOPreFormatter.CHARS.put("\"e", "ë"); // #235
        OOPreFormatter.CHARS.put("`i", "ì"); // #236
        OOPreFormatter.CHARS.put("'i", "í"); // #237
        OOPreFormatter.CHARS.put("^i", "î"); // #238
        OOPreFormatter.CHARS.put("\"i", "ï"); // #239
        OOPreFormatter.CHARS.put("dh", "ð"); // #240
        OOPreFormatter.CHARS.put("~n", "ñ"); // #241
        OOPreFormatter.CHARS.put("`o", "ò"); // #242
        OOPreFormatter.CHARS.put("'o", "ó"); // #243
        OOPreFormatter.CHARS.put("^o", "ô"); // #244
        OOPreFormatter.CHARS.put("~o", "õ"); // #245
        OOPreFormatter.CHARS.put("\"o", "ö"); // #246
        // According to ISO 8859-1 the "\div" symbol should be placed here
        // (#247).
        // Omitting this, because it is a mathematical symbol.
        OOPreFormatter.CHARS.put("o", "ø"); // #248
        OOPreFormatter.CHARS.put("`u", "ù"); // #249
        OOPreFormatter.CHARS.put("'u", "ú"); // #250
        OOPreFormatter.CHARS.put("^u", "û"); // #251
        OOPreFormatter.CHARS.put("\"u", "ü"); // #252
        OOPreFormatter.CHARS.put("'y", "ý"); // #253
        OOPreFormatter.CHARS.put("th", "þ"); // #254
        OOPreFormatter.CHARS.put("\"y", "ÿ"); // #255

        // HTML special characters without names (UNICODE Latin Extended-A),
        // indicated by UNICODE number
        OOPreFormatter.CHARS.put("=A", "Ā"); // "Amacr"
        OOPreFormatter.CHARS.put("=a", "�?"); // "amacr"
        OOPreFormatter.CHARS.put("uA", "Ă"); // "Abreve"
        OOPreFormatter.CHARS.put("ua", "ă"); // "abreve"
        OOPreFormatter.CHARS.put("kA", "Ą"); // "Aogon"
        OOPreFormatter.CHARS.put("ka", "ą"); // "aogon"
        OOPreFormatter.CHARS.put("'C", "Ć"); // "Cacute"
        OOPreFormatter.CHARS.put("'c", "ć"); // "cacute"
        OOPreFormatter.CHARS.put("^C", "Ĉ"); // "Ccirc"
        OOPreFormatter.CHARS.put("^c", "ĉ"); // "ccirc"
        OOPreFormatter.CHARS.put(".C", "Ċ"); // "Cdot"
        OOPreFormatter.CHARS.put(".c", "ċ"); // "cdot"
        OOPreFormatter.CHARS.put("vC", "Č"); // "Ccaron"
        OOPreFormatter.CHARS.put("vc", "�?"); // "ccaron"
        OOPreFormatter.CHARS.put("vD", "Ď"); // "Dcaron"
        // Symbol #271 (d�) has no special Latex command
        OOPreFormatter.CHARS.put("DJ", "�?"); // "Dstrok"
        OOPreFormatter.CHARS.put("dj", "đ"); // "dstrok"
        OOPreFormatter.CHARS.put("=E", "Ē"); // "Emacr"
        OOPreFormatter.CHARS.put("=e", "ē"); // "emacr"
        OOPreFormatter.CHARS.put("uE", "Ĕ"); // "Ebreve"
        OOPreFormatter.CHARS.put("ue", "ĕ"); // "ebreve"
        OOPreFormatter.CHARS.put(".E", "Ė"); // "Edot"
        OOPreFormatter.CHARS.put(".e", "ė"); // "edot"
        OOPreFormatter.CHARS.put("kE", "Ę"); // "Eogon"
        OOPreFormatter.CHARS.put("ke", "ę"); // "eogon"
        OOPreFormatter.CHARS.put("vE", "Ě"); // "Ecaron"
        OOPreFormatter.CHARS.put("ve", "ě"); // "ecaron"
        OOPreFormatter.CHARS.put("^G", "Ĝ"); // "Gcirc"
        OOPreFormatter.CHARS.put("^g", "�?"); // "gcirc"
        OOPreFormatter.CHARS.put("uG", "Ğ"); // "Gbreve"
        OOPreFormatter.CHARS.put("ug", "ğ"); // "gbreve"
        OOPreFormatter.CHARS.put(".G", "Ġ"); // "Gdot"
        OOPreFormatter.CHARS.put(".g", "ġ"); // "gdot"
        OOPreFormatter.CHARS.put("cG", "Ģ"); // "Gcedil"
        OOPreFormatter.CHARS.put("'g", "ģ"); // "gacute"
        OOPreFormatter.CHARS.put("^H", "Ĥ"); // "Hcirc"
        OOPreFormatter.CHARS.put("^h", "ĥ"); // "hcirc"
        OOPreFormatter.CHARS.put("Hstrok", "Ħ"); // "Hstrok"
        OOPreFormatter.CHARS.put("hstrok", "ħ"); // "hstrok"
        OOPreFormatter.CHARS.put("~I", "Ĩ"); // "Itilde"
        OOPreFormatter.CHARS.put("~i", "ĩ"); // "itilde"
        OOPreFormatter.CHARS.put("=I", "Ī"); // "Imacr"
        OOPreFormatter.CHARS.put("=i", "ī"); // "imacr"
        OOPreFormatter.CHARS.put("uI", "Ĭ"); // "Ibreve"
        OOPreFormatter.CHARS.put("ui", "ĭ"); // "ibreve"
        OOPreFormatter.CHARS.put("kI", "Į"); // "Iogon"
        OOPreFormatter.CHARS.put("ki", "į"); // "iogon"
        OOPreFormatter.CHARS.put(".I", "İ"); // "Idot"
        OOPreFormatter.CHARS.put("i", "ı"); // "inodot"
        // Symbol #306 (IJ) has no special Latex command
        // Symbol #307 (ij) has no special Latex command
        OOPreFormatter.CHARS.put("^J", "Ĵ"); // "Jcirc"
        OOPreFormatter.CHARS.put("^j", "ĵ"); // "jcirc"
        OOPreFormatter.CHARS.put("cK", "Ķ"); // "Kcedil"
        OOPreFormatter.CHARS.put("ck", "ķ"); // "kcedil"
        // Symbol #312 (k) has no special Latex command
        OOPreFormatter.CHARS.put("'L", "Ĺ"); // "Lacute"
        OOPreFormatter.CHARS.put("'l", "ĺ"); // "lacute"
        OOPreFormatter.CHARS.put("cL", "Ļ"); // "Lcedil"
        OOPreFormatter.CHARS.put("cl", "ļ"); // "lcedil"
        // Symbol #317 (L�) has no special Latex command
        // Symbol #318 (l�) has no special Latex command
        OOPreFormatter.CHARS.put("Lmidot", "Ŀ"); // "Lmidot"
        OOPreFormatter.CHARS.put("lmidot", "ŀ"); // "lmidot"
        OOPreFormatter.CHARS.put("L", "�?"); // "Lstrok"
        OOPreFormatter.CHARS.put("l", "ł"); // "lstrok"
        OOPreFormatter.CHARS.put("'N", "Ń"); // "Nacute"
        OOPreFormatter.CHARS.put("'n", "ń"); // "nacute"
        OOPreFormatter.CHARS.put("cN", "Ņ"); // "Ncedil"
        OOPreFormatter.CHARS.put("cn", "ņ"); // "ncedil"
        OOPreFormatter.CHARS.put("vN", "Ň"); // "Ncaron"
        OOPreFormatter.CHARS.put("vn", "ň"); // "ncaron"
        // Symbol #329 (�n) has no special Latex command
        OOPreFormatter.CHARS.put("NG", "Ŋ"); // "ENG"
        OOPreFormatter.CHARS.put("ng", "ŋ"); // "eng"
        OOPreFormatter.CHARS.put("=O", "Ō"); // "Omacr"
        OOPreFormatter.CHARS.put("=o", "�?"); // "omacr"
        OOPreFormatter.CHARS.put("uO", "Ŏ"); // "Obreve"
        OOPreFormatter.CHARS.put("uo", "�?"); // "obreve"
        OOPreFormatter.CHARS.put("HO", "�?"); // "Odblac"
        OOPreFormatter.CHARS.put("Ho", "ő"); // "odblac"
        OOPreFormatter.CHARS.put("OE", "Œ"); // "OElig"
        OOPreFormatter.CHARS.put("oe", "œ"); // "oelig"
        OOPreFormatter.CHARS.put("'R", "Ŕ"); // "Racute"
        OOPreFormatter.CHARS.put("'r", "ŕ"); // "racute"
        OOPreFormatter.CHARS.put("cR", "Ŗ"); // "Rcedil"
        OOPreFormatter.CHARS.put("cr", "ŗ"); // "rcedil"
        OOPreFormatter.CHARS.put("vR", "Ř"); // "Rcaron"
        OOPreFormatter.CHARS.put("vr", "ř"); // "rcaron"
        OOPreFormatter.CHARS.put("'S", "Ś"); // "Sacute"
        OOPreFormatter.CHARS.put("'s", "ś"); // "sacute"
        OOPreFormatter.CHARS.put("^S", "Ŝ"); // "Scirc"
        OOPreFormatter.CHARS.put("^s", "�?"); // "scirc"
        OOPreFormatter.CHARS.put("cS", "Ş"); // "Scedil"
        OOPreFormatter.CHARS.put("cs", "ş"); // "scedil"
        OOPreFormatter.CHARS.put("vS", "Š"); // "Scaron"
        OOPreFormatter.CHARS.put("vs", "š"); // "scaron"
        OOPreFormatter.CHARS.put("cT", "Ţ"); // "Tcedil"
        OOPreFormatter.CHARS.put("ct", "ţ"); // "tcedil"
        OOPreFormatter.CHARS.put("vT", "Ť"); // "Tcaron"
        // Symbol #357 (t�) has no special Latex command
        OOPreFormatter.CHARS.put("Tstrok", "Ŧ"); // "Tstrok"
        OOPreFormatter.CHARS.put("tstrok", "ŧ"); // "tstrok"
        OOPreFormatter.CHARS.put("~U", "Ũ"); // "Utilde"
        OOPreFormatter.CHARS.put("~u", "ũ"); // "utilde"
        OOPreFormatter.CHARS.put("=U", "Ū"); // "Umacr"
        OOPreFormatter.CHARS.put("=u", "ū"); // "umacr"
        OOPreFormatter.CHARS.put("uU", "Ŭ"); // "Ubreve"
        OOPreFormatter.CHARS.put("uu", "ŭ"); // "ubreve"
        OOPreFormatter.CHARS.put("rU", "Ů"); // "Uring"
        OOPreFormatter.CHARS.put("ru", "ů"); // "uring"
        OOPreFormatter.CHARS.put("HU", "ů"); // "Odblac"
        OOPreFormatter.CHARS.put("Hu", "ű"); // "odblac"
        OOPreFormatter.CHARS.put("kU", "Ų"); // "Uogon"
        OOPreFormatter.CHARS.put("ku", "ų"); // "uogon"
        OOPreFormatter.CHARS.put("^W", "Ŵ"); // "Wcirc"
        OOPreFormatter.CHARS.put("^w", "ŵ"); // "wcirc"
        OOPreFormatter.CHARS.put("^Y", "Ŷ"); // "Ycirc"
        OOPreFormatter.CHARS.put("^y", "ŷ"); // "ycirc"
        OOPreFormatter.CHARS.put("\"Y", "Ÿ"); // "Yuml"
        OOPreFormatter.CHARS.put("'Z", "Ź"); // "Zacute"
        OOPreFormatter.CHARS.put("'z", "ź"); // "zacute"
        OOPreFormatter.CHARS.put(".Z", "Ż"); // "Zdot"
        OOPreFormatter.CHARS.put(".z", "ż"); // "zdot"
        OOPreFormatter.CHARS.put("vZ", "Ž"); // "Zcaron"
        OOPreFormatter.CHARS.put("vz", "ž"); // "zcaron"
        // Symbol #383 (f) has no special Latex command
        OOPreFormatter.CHARS.put("%", "%"); // percent sign
    }


    @Override
    public String format(String field) {
        int i;
        field = field.replaceAll("&|\\\\&", "&");

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
                    Object result = OOPreFormatter.CHARS.get(command);
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
                            String part = StringUtil.getPart(field, i, false);
                            i += part.length();
                            combody = part;
                        } else {
                            combody = field.substring(i, i + 1);
                            // System.out.println("... "+combody);
                        }
                        Object result = OOPreFormatter.CHARS.get(command + combody);

                        if (result != null) {
                            sb.append((String) result);
                        }

                        incommand = false;
                        escaped = false;
                    } else {
                        //	Are we already at the end of the string?
                        if ((i + 1) == field.length()) {
                            String command = currentCommand.toString();
                            Object result = OOPreFormatter.CHARS.get(command);
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
                String argument = null;

                if (!incommand) {
                    sb.append(c);
                } else if (Character.isWhitespace(c) || (c == '{') || (c == '}')) {
                    // First test if we are already at the end of the string.
                    // if (i >= field.length()-1)
                    // break testContent;

                    String command = currentCommand.toString();

                    // Then test if we are dealing with a italics or bold
                    // command.
                    // If so, handle.
                    if (command.equals("em") || command.equals("emph") || command.equals("textit")) {
                        String part = StringUtil.getPart(field, i, true);

                        i += part.length();
                        sb.append("<em>").append(part).append("</em>");
                    } else if (command.equals("textbf")) {
                        String part = StringUtil.getPart(field, i, true);
                        i += part.length();
                        sb.append("<b>").append(part).append("</b>");
                    } else if (c == '{') {
                        String part = StringUtil.getPart(field, i, true);
                        i += part.length();
                        argument = part;
                        if (argument != null) {
                            // handle common case of general latex command
                            Object result = OOPreFormatter.CHARS.get(command + argument);
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
                        Object result = OOPreFormatter.CHARS.get(command);
                        if (result != null) {
                            sb.append((String) result);
                        } else {
                            // If the command is unknown, just print it:
                            sb.append(command);
                        }
                    } else {
                        Object result = OOPreFormatter.CHARS.get(command);
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
