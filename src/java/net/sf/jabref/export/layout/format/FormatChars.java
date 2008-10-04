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
		CHARS.put("=A", "&#256;"); // "Amacr"
		CHARS.put("=a", "&#257;"); // "amacr"
		CHARS.put("uA", "&#258;"); // "Abreve"
		CHARS.put("ua", "&#259;"); // "abreve"
		CHARS.put("kA", "&#260;"); // "Aogon"
		CHARS.put("ka", "&#261;"); // "aogon"
		CHARS.put("'C", "&#262;"); // "Cacute"
		CHARS.put("'c", "&#263;"); // "cacute"
		CHARS.put("^C", "&#264;"); // "Ccirc"
		CHARS.put("^c", "&#265;"); // "ccirc"
		CHARS.put(".C", "&#266;"); // "Cdot"
		CHARS.put(".c", "&#267;"); // "cdot"
		CHARS.put("vC", "&#268;"); // "Ccaron"
		CHARS.put("vc", "&#269;"); // "ccaron"
		CHARS.put("vD", "&#270;"); // "Dcaron"
		// Symbol #271 (d�) has no special Latex command
		CHARS.put("DJ", "&#272;"); // "Dstrok"
		CHARS.put("dj", "&#273;"); // "dstrok"
		CHARS.put("=E", "&#274;"); // "Emacr"
		CHARS.put("=e", "&#275;"); // "emacr"
		CHARS.put("uE", "&#276;"); // "Ebreve"
		CHARS.put("ue", "&#277;"); // "ebreve"
		CHARS.put(".E", "&#278;"); // "Edot"
		CHARS.put(".e", "&#279;"); // "edot"
		CHARS.put("kE", "&#280;"); // "Eogon"
		CHARS.put("ke", "&#281;"); // "eogon"
		CHARS.put("vE", "&#282;"); // "Ecaron"
		CHARS.put("ve", "&#283;"); // "ecaron"
		CHARS.put("^G", "&#284;"); // "Gcirc"
		CHARS.put("^g", "&#285;"); // "gcirc"
		CHARS.put("uG", "&#286;"); // "Gbreve"
		CHARS.put("ug", "&#287;"); // "gbreve"
		CHARS.put(".G", "&#288;"); // "Gdot"
		CHARS.put(".g", "&#289;"); // "gdot"
		CHARS.put("cG", "&#290;"); // "Gcedil"
		CHARS.put("'g", "&#291;"); // "gacute"
		CHARS.put("^H", "&#292;"); // "Hcirc"
		CHARS.put("^h", "&#293;"); // "hcirc"
		CHARS.put("Hstrok", "&#294;"); // "Hstrok"
		CHARS.put("hstrok", "&#295;"); // "hstrok"
		CHARS.put("~I", "&#296;"); // "Itilde"
		CHARS.put("~i", "&#297;"); // "itilde"
		CHARS.put("=I", "&#298;"); // "Imacr"
		CHARS.put("=i", "&#299;"); // "imacr"
		CHARS.put("uI", "&#300;"); // "Ibreve"
		CHARS.put("ui", "&#301;"); // "ibreve"
		CHARS.put("kI", "&#302;"); // "Iogon"
		CHARS.put("ki", "&#303;"); // "iogon"
		CHARS.put(".I", "&#304;"); // "Idot"
		CHARS.put("i", "&#305;"); // "inodot"
		// Symbol #306 (IJ) has no special Latex command
		// Symbol #307 (ij) has no special Latex command
		CHARS.put("^J", "&#308;"); // "Jcirc"
		CHARS.put("^j", "&#309;"); // "jcirc"
		CHARS.put("cK", "&#310;"); // "Kcedil"
		CHARS.put("ck", "&#311;"); // "kcedil"
		// Symbol #312 (k) has no special Latex command
		CHARS.put("'L", "&#313;"); // "Lacute"
		CHARS.put("'l", "&#314;"); // "lacute"
		CHARS.put("cL", "&#315;"); // "Lcedil"
		CHARS.put("cl", "&#316;"); // "lcedil"
		// Symbol #317 (L�) has no special Latex command
		// Symbol #318 (l�) has no special Latex command
		CHARS.put("Lmidot", "&#319;"); // "Lmidot"
		CHARS.put("lmidot", "&#320;"); // "lmidot"
		CHARS.put("L", "&#321;"); // "Lstrok"
		CHARS.put("l", "&#322;"); // "lstrok"
		CHARS.put("'N", "&#323;"); // "Nacute"
		CHARS.put("'n", "&#324;"); // "nacute"
		CHARS.put("cN", "&#325;"); // "Ncedil"
		CHARS.put("cn", "&#326;"); // "ncedil"
		CHARS.put("vN", "&#327;"); // "Ncaron"
		CHARS.put("vn", "&#328;"); // "ncaron"
		// Symbol #329 (�n) has no special Latex command
		CHARS.put("NG", "&#330;"); // "ENG"
		CHARS.put("ng", "&#331;"); // "eng"
		CHARS.put("=O", "&#332;"); // "Omacr"
		CHARS.put("=o", "&#333;"); // "omacr"
		CHARS.put("uO", "&#334;"); // "Obreve"
		CHARS.put("uo", "&#335;"); // "obreve"
		CHARS.put("HO", "&#336;"); // "Odblac"
		CHARS.put("Ho", "&#337;"); // "odblac"
		CHARS.put("OE", "&#338;"); // "OElig"
		CHARS.put("oe", "&#339;"); // "oelig"
		CHARS.put("'R", "&#340;"); // "Racute"
		CHARS.put("'r", "&#341;"); // "racute"
		CHARS.put("cR", "&#342;"); // "Rcedil"
		CHARS.put("cr", "&#343;"); // "rcedil"
		CHARS.put("vR", "&#344;"); // "Rcaron"
		CHARS.put("vr", "&#345;"); // "rcaron"
		CHARS.put("'S", "&#346;"); // "Sacute"
		CHARS.put("'s", "&#347;"); // "sacute"
		CHARS.put("^S", "&#348;"); // "Scirc"
		CHARS.put("^s", "&#349;"); // "scirc"
		CHARS.put("cS", "&#350;"); // "Scedil"
		CHARS.put("cs", "&#351;"); // "scedil"
		CHARS.put("vS", "&#352;"); // "Scaron"
		CHARS.put("vs", "&#353;"); // "scaron"
		CHARS.put("cT", "&#354;"); // "Tcedil"
		CHARS.put("ct", "&#355;"); // "tcedil"
		CHARS.put("vT", "&#356;"); // "Tcaron"
		// Symbol #357 (t�) has no special Latex command
		CHARS.put("Tstrok", "&#358;"); // "Tstrok"
		CHARS.put("tstrok", "&#359;"); // "tstrok"
		CHARS.put("~U", "&#360;"); // "Utilde"
		CHARS.put("~u", "&#361;"); // "utilde"
		CHARS.put("=U", "&#362;"); // "Umacr"
		CHARS.put("=u", "&#363;"); // "umacr"
		CHARS.put("uU", "&#364;"); // "Ubreve"
		CHARS.put("uu", "&#365;"); // "ubreve"
		CHARS.put("rU", "&#366;"); // "Uring"
		CHARS.put("ru", "&#367;"); // "uring"
		CHARS.put("HU", "&#368;"); // "Odblac"
		CHARS.put("Hu", "&#369;"); // "odblac"
		CHARS.put("kU", "&#370;"); // "Uogon"
		CHARS.put("ku", "&#371;"); // "uogon"
		CHARS.put("^W", "&#372;"); // "Wcirc"
		CHARS.put("^w", "&#373;"); // "wcirc"
		CHARS.put("^Y", "&#374;"); // "Ycirc"
		CHARS.put("^y", "&#375;"); // "ycirc"
		CHARS.put("\"Y", "&#376;"); // "Yuml"
		CHARS.put("'Z", "&#377;"); // "Zacute"
		CHARS.put("'z", "&#378;"); // "zacute"
		CHARS.put(".Z", "&#379;"); // "Zdot"
		CHARS.put(".z", "&#380;"); // "zdot"
		CHARS.put("vZ", "&#381;"); // "Zcaron"
		CHARS.put("vz", "&#382;"); // "zcaron"
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
