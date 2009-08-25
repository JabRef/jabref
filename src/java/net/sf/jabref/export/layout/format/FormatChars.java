package net.sf.jabref.export.layout.format;

import net.sf.jabref.Globals;
import net.sf.jabref.export.layout.LayoutFormatter;

import java.util.HashMap;

/**
 * This formatter converts LaTeX character sequences their equicalent unicode characters,
 * and removes other LaTeX commands without handling them.
 */
public class FormatChars implements LayoutFormatter {

    public static HashMap<String, String> CHARS = new HashMap<String, String>();

    static {
		CHARS.put("`A", "À"); // #192
		CHARS.put("'A", "Á"); // #193
		CHARS.put("^A", "Â"); // #194
		CHARS.put("~A", "Ã"); // #195
		CHARS.put("\"A", "Ä"); // #196
		CHARS.put("AA", "Å"); // #197
		CHARS.put("AE", "Æ"); // #198
		CHARS.put("cC", "Ç"); // #199
        CHARS.put("`E", "È"); // #200
		CHARS.put("'E", "É"); // #201
		CHARS.put("^E", "Ê"); // #202
		CHARS.put("\"E", "Ë"); // #203
		CHARS.put("`I", "Ì"); // #204
		CHARS.put("'I", "Í"); // #205
		CHARS.put("^I", "Î"); // #206
		CHARS.put("\"I", "Ï"); // #207
		CHARS.put("DH", "Ð"); // #208
		CHARS.put("~N", "Ñ"); // #209
		CHARS.put("`O", "Ò"); // #210
		CHARS.put("'O", "Ó"); // #211
		CHARS.put("^O", "Ô"); // #212
		CHARS.put("~O", "Õ"); // #213
		CHARS.put("\"O", "Ö"); // #214
		// According to ISO 8859-1 the "\times" symbol should be placed here
		// (#215).
		// Omitting this, because it is a mathematical symbol.
		CHARS.put("O", "Ø"); // #216
		CHARS.put("`U", "Ù"); // #217
		CHARS.put("'U", "Ú"); // #218
		CHARS.put("^U", "Û"); // #219
		CHARS.put("\"U", "Ü"); // #220
		CHARS.put("'Y", "Ý"); // #221
		CHARS.put("TH", "Þ"); // #222
		CHARS.put("ss", "ß"); // #223
		CHARS.put("`a", "à"); // #224
		CHARS.put("'a", "á"); // #225
		CHARS.put("^a", "â"); // #226
		CHARS.put("~a", "ã"); // #227
		CHARS.put("\"a", "ä"); // #228
		CHARS.put("aa", "å"); // #229
		CHARS.put("ae", "æ"); // #230
		CHARS.put("cc", "ç"); // #231
		CHARS.put("`e", "è"); // #232
		CHARS.put("'e", "é"); // #233
		CHARS.put("^e", "ê"); // #234
		CHARS.put("\"e", "ë"); // #235
		CHARS.put("`i", "ì"); // #236
		CHARS.put("'i", "í"); // #237
		CHARS.put("^i", "î"); // #238
		CHARS.put("\"i", "ï"); // #239
		CHARS.put("dh", "ð"); // #240
		CHARS.put("~n", "ñ"); // #241
		CHARS.put("`o", "ò"); // #242
		CHARS.put("'o", "ó"); // #243
		CHARS.put("^o", "ô"); // #244
		CHARS.put("~o", "õ"); // #245
		CHARS.put("\"o", "ö"); // #246
		// According to ISO 8859-1 the "\div" symbol should be placed here
		// (#247).
		// Omitting this, because it is a mathematical symbol.
		CHARS.put("o", "ø"); // #248
		CHARS.put("`u", "ù"); // #249
		CHARS.put("'u", "ú"); // #250
		CHARS.put("^u", "û"); // #251
		CHARS.put("\"u", "ü"); // #252
		CHARS.put("'y", "ý"); // #253
		CHARS.put("th", "þ"); // #254
		CHARS.put("\"y", "ÿ"); // #255

		// HTML special characters without names (UNICODE Latin Extended-A),
		// indicated by UNICODE number
		CHARS.put("=A", "Ā"); // "Amacr"
		CHARS.put("=a", "ā"); // "amacr"
		CHARS.put("uA", "Ă"); // "Abreve"
		CHARS.put("ua", "ă"); // "abreve"
		CHARS.put("kA", "Ą"); // "Aogon"
		CHARS.put("ka", "ą"); // "aogon"
		CHARS.put("'C", "Ć"); // "Cacute"
		CHARS.put("'c", "ć"); // "cacute"
		CHARS.put("^C", "Ĉ"); // "Ccirc"
		CHARS.put("^c", "ĉ"); // "ccirc"
		CHARS.put(".C", "Ċ"); // "Cdot"
		CHARS.put(".c", "ċ"); // "cdot"
		CHARS.put("vC", "Č"); // "Ccaron"
		CHARS.put("vc", "č"); // "ccaron"
		CHARS.put("vD", "Ď"); // "Dcaron"
		// Symbol #271 (d�) has no special Latex command
		CHARS.put("DJ", "Đ"); // "Dstrok"
		CHARS.put("dj", "đ"); // "dstrok"
		CHARS.put("=E", "Ē"); // "Emacr"
		CHARS.put("=e", "ē"); // "emacr"
		CHARS.put("uE", "Ĕ"); // "Ebreve"
		CHARS.put("ue", "ĕ"); // "ebreve"
		CHARS.put(".E", "Ė"); // "Edot"
		CHARS.put(".e", "ė"); // "edot"
		CHARS.put("kE", "Ę"); // "Eogon"
		CHARS.put("ke", "ę"); // "eogon"
		CHARS.put("vE", "Ě"); // "Ecaron"
		CHARS.put("ve", "ě"); // "ecaron"
		CHARS.put("^G", "Ĝ"); // "Gcirc"
		CHARS.put("^g", "ĝ"); // "gcirc"
		CHARS.put("uG", "Ğ"); // "Gbreve"
		CHARS.put("ug", "ğ"); // "gbreve"
		CHARS.put(".G", "Ġ"); // "Gdot"
		CHARS.put(".g", "ġ"); // "gdot"
		CHARS.put("cG", "Ģ"); // "Gcedil"
		CHARS.put("'g", "ģ"); // "gacute"
		CHARS.put("^H", "Ĥ"); // "Hcirc"
		CHARS.put("^h", "ĥ"); // "hcirc"
		CHARS.put("Hstrok", "Ħ"); // "Hstrok"
		CHARS.put("hstrok", "ħ"); // "hstrok"
		CHARS.put("~I", "Ĩ"); // "Itilde"
		CHARS.put("~i", "ĩ"); // "itilde"
		CHARS.put("=I", "Ī"); // "Imacr"
		CHARS.put("=i", "ī"); // "imacr"
		CHARS.put("uI", "Ĭ"); // "Ibreve"
		CHARS.put("ui", "ĭ"); // "ibreve"
		CHARS.put("kI", "Į"); // "Iogon"
		CHARS.put("ki", "į"); // "iogon"
		CHARS.put(".I", "İ"); // "Idot"
		CHARS.put("i", "ı"); // "inodot"
		// Symbol #306 (IJ) has no special Latex command
		// Symbol #307 (ij) has no special Latex command
		CHARS.put("^J", "Ĵ"); // "Jcirc"
		CHARS.put("^j", "ĵ"); // "jcirc"
		CHARS.put("cK", "Ķ"); // "Kcedil"
		CHARS.put("ck", "ķ"); // "kcedil"
		// Symbol #312 (k) has no special Latex command
		CHARS.put("'L", "Ĺ"); // "Lacute"
		CHARS.put("'l", "ĺ"); // "lacute"
		CHARS.put("cL", "Ļ"); // "Lcedil"
		CHARS.put("cl", "ļ"); // "lcedil"
		// Symbol #317 (L�) has no special Latex command
		// Symbol #318 (l�) has no special Latex command
		CHARS.put("Lmidot", "Ŀ"); // "Lmidot"
		CHARS.put("lmidot", "ŀ"); // "lmidot"
		CHARS.put("L", "Ł"); // "Lstrok"
		CHARS.put("l", "ł"); // "lstrok"
		CHARS.put("'N", "Ń"); // "Nacute"
		CHARS.put("'n", "ń"); // "nacute"
		CHARS.put("cN", "Ņ"); // "Ncedil"
		CHARS.put("cn", "ņ"); // "ncedil"
		CHARS.put("vN", "Ň"); // "Ncaron"
		CHARS.put("vn", "ň"); // "ncaron"
		// Symbol #329 (�n) has no special Latex command
		CHARS.put("NG", "Ŋ"); // "ENG"
		CHARS.put("ng", "ŋ"); // "eng"
		CHARS.put("=O", "Ō"); // "Omacr"
		CHARS.put("=o", "ō"); // "omacr"
		CHARS.put("uO", "Ŏ"); // "Obreve"
		CHARS.put("uo", "ŏ"); // "obreve"
		CHARS.put("HO", "Ő"); // "Odblac"
		CHARS.put("Ho", "ő"); // "odblac"
		CHARS.put("OE", "Œ"); // "OElig"
		CHARS.put("oe", "œ"); // "oelig"
		CHARS.put("'R", "Ŕ"); // "Racute"
		CHARS.put("'r", "ŕ"); // "racute"
		CHARS.put("cR", "Ŗ"); // "Rcedil"
		CHARS.put("cr", "ŗ"); // "rcedil"
		CHARS.put("vR", "Ř"); // "Rcaron"
		CHARS.put("vr", "ř"); // "rcaron"
		CHARS.put("'S", "Ś"); // "Sacute"
		CHARS.put("'s", "ś"); // "sacute"
		CHARS.put("^S", "Ŝ"); // "Scirc"
		CHARS.put("^s", "ŝ"); // "scirc"
		CHARS.put("cS", "Ş"); // "Scedil"
		CHARS.put("cs", "ş"); // "scedil"
		CHARS.put("vS", "Š"); // "Scaron"
		CHARS.put("vs", "š"); // "scaron"
		CHARS.put("cT", "Ţ"); // "Tcedil"
		CHARS.put("ct", "ţ"); // "tcedil"
		CHARS.put("vT", "Ť"); // "Tcaron"
		// Symbol #357 (t�) has no special Latex command
		CHARS.put("Tstrok", "Ŧ"); // "Tstrok"
		CHARS.put("tstrok", "ŧ"); // "tstrok"
		CHARS.put("~U", "Ũ"); // "Utilde"
		CHARS.put("~u", "ũ"); // "utilde"
		CHARS.put("=U", "Ū"); // "Umacr"
		CHARS.put("=u", "ū"); // "umacr"
		CHARS.put("uU", "Ŭ"); // "Ubreve"
		CHARS.put("uu", "ŭ"); // "ubreve"
		CHARS.put("rU", "Ů"); // "Uring"
		CHARS.put("ru", "ů"); // "uring"
		CHARS.put("HU", "ů"); // "Odblac"
		CHARS.put("Hu", "ű"); // "odblac"
		CHARS.put("kU", "Ų"); // "Uogon"
		CHARS.put("ku", "ų"); // "uogon"
		CHARS.put("^W", "Ŵ"); // "Wcirc"
		CHARS.put("^w", "ŵ"); // "wcirc"
		CHARS.put("^Y", "Ŷ"); // "Ycirc"
		CHARS.put("^y", "ŷ"); // "ycirc"
		CHARS.put("\"Y", "Ÿ"); // "Yuml"
		CHARS.put("'Z", "Ź"); // "Zacute"
		CHARS.put("'z", "ź"); // "zacute"
		CHARS.put(".Z", "Ż"); // "Zdot"
		CHARS.put(".z", "ż"); // "zdot"
		CHARS.put("vZ", "Ž"); // "Zcaron"
		CHARS.put("vz", "ž"); // "zcaron"
		// Symbol #383 (f) has no special Latex command
        CHARS.put("%", "%"); // percent sign
    }

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
				if (incommand){
					/* Close Command */
					String command = currentCommand.toString();
					Object result = CHARS.get(command);
					if (result != null) {
						sb.append((String) result);
					} else {
						sb.append(command);
					}
				}
				escaped = true;
				incommand = true;
				currentCommand = new StringBuffer();
			} else if (!incommand && (c == '{' || c == '}')) {
				// Swallow the brace.
			} else if (Character.isLetter(c) || (c == '%')
				|| (Globals.SPECIAL_COMMAND_CHARS.indexOf(String.valueOf(c)) >= 0)) {
				escaped = false;

                if (!incommand)
					sb.append(c);
					// Else we are in a command, and should not keep the letter.
				else {
					currentCommand.append(c);
                    testCharCom: if ((currentCommand.length() == 1)
						&& (Globals.SPECIAL_COMMAND_CHARS.indexOf(currentCommand.toString()) >= 0)) {
						// This indicates that we are in a command of the type
						// \^o or \~{n}
						if (i >= field.length() - 1)
							break testCharCom;

						String command = currentCommand.toString();
						i++;
						c = field.charAt(i);
						// System.out.println("next: "+(char)c);
						String combody;
						if (c == '{') {
							IntAndString part = getPart(field, i, false);
							i += part.i;
							combody = part.s;
						} else {
							combody = field.substring(i, i + 1);
							// System.out.println("... "+combody);
						}
						Object result = CHARS.get(command + combody);

						if (result != null)
							sb.append((String) result);

						incommand = false;
						escaped = false;
					} else { 
						//	Are we already at the end of the string?
						if (i + 1 == field.length()){
							String command = currentCommand.toString();
                            Object result = CHARS.get(command);
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
                                                
                    if (c == '{') {
						IntAndString part = getPart(field, i, true);
						i += part.i;
						argument = part.s;
						if (argument != null) {
							// handle common case of general latex command
							Object result = CHARS.get(command + argument);
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
                        Object result = CHARS.get(command);
                        if (result != null) {
                            sb.append((String) result);
                        } else {
                            // If the command is unknown, just print it:
                            sb.append(command);
                        }
                    } else {
						Object result = CHARS.get(command);
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
				}*/ else {
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

	private IntAndString getPart(String text, int i, boolean terminateOnEndBraceOnly) {
		char c;
		int count = 0;
		
		StringBuffer part = new StringBuffer();
		
		// advance to first char and skip wihitespace
		i++;
		while (i < text.length() && Character.isWhitespace(text.charAt(i))){
			i++;
		}
		
		// then grab whathever is the first token (counting braces)
		while (i < text.length()){
			c = text.charAt(i);
			if (!terminateOnEndBraceOnly && count == 0 && Character.isWhitespace(c)) {
				i--; // end argument and leave whitespace for further
					 // processing
				break;
			}
			if (c == '}' && --count < 0)
				break;
			else if (c == '{')
				count++;
			part.append(c);
			i++;
		}
		return new IntAndString(part.length(), format(part.toString()));
	}

	private class IntAndString {
		public int i;

		String s;

		public IntAndString(int i, String s) {
			this.i = i;
			this.s = s;
		}
	}
}

