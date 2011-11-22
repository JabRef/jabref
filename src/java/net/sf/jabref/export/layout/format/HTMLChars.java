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
import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * This formatter escapes characters so they are suitable for HTML.
 * 
 * @version $Revision$ ($Date$)
 */
public class HTMLChars implements LayoutFormatter {

	public String format(String field) {
		int i;
		field = field.replaceAll("&|\\\\&", "&amp;").replaceAll("[\\n]{2,}", "<p>")
                .replaceAll("\\n", "<br>");

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
					Object result = Globals.HTMLCHARS.get(command);
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
						Object result = Globals.HTMLCHARS.get(command + combody);

						if (result != null)
							sb.append((String) result);

						incommand = false;
						escaped = false;
					} else { 
						//	Are we already at the end of the string?
						if (i + 1 == field.length()){
							String command = currentCommand.toString();
                            Object result = Globals.HTMLCHARS.get(command);
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
						IntAndString part = getPart(field, i, true);

						i += part.i;
						sb.append("<em>").append(part.s).append("</em>");
					} else if (command.equals("textbf")) {
						IntAndString part = getPart(field, i, true);
						i += part.i;
						sb.append("<b>").append(part.s).append("</b>");
					} else if (c == '{') {
						IntAndString part = getPart(field, i, true);
						i += part.i;
						argument = part.s;
						if (argument != null) {
							// handle common case of general latex command
							Object result = Globals.HTMLCHARS.get(command + argument);
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
                        Object result = Globals.HTMLCHARS.get(command);
                        if (result != null) {
                            sb.append((String) result);
                        } else {
                            // If the command is unknown, just print it:
                            sb.append(command);
                        }
                    } else {
						Object result = Globals.HTMLCHARS.get(command);
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
