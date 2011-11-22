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
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 *
 */
public class RTFChars implements LayoutFormatter {

	public String format(String field) {

		StringBuffer sb = new StringBuffer("");
		StringBuffer currentCommand = null;
		boolean escaped = false, incommand = false;
		for (int i = 0; i < field.length(); i++) {

            /*System.out.println("incommand="+incommand+". escaped="+escaped
                            +". currentCommand='"+(currentCommand!=null?currentCommand.toString():"")+"'");
            System.out.println("sb: '"+sb.toString()+"'");*/

            char c = field.charAt(i);

            //System.out.println("Char: '"+((char)c)+"'");

            if (escaped && (c == '\\')) {
				sb.append('\\');
				escaped = false;
			}

			else if (c == '\\') {
				escaped = true;
				incommand = true;
				currentCommand = new StringBuffer();
			} else if (!incommand && (c == '{' || c == '}')) {
				// Swallow the brace.
			} else if (Character.isLetter(c)
				|| (Globals.SPECIAL_COMMAND_CHARS.indexOf("" + c) >= 0)) {
				escaped = false;
				if (!incommand){
					sb.append(c);
				} else {
					// Else we are in a command, and should not keep the letter.
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
						String combody;
						if (c == '{') {
							IntAndString part = getPart(field, i);
							i += part.i;
							combody = part.s;
						} else {
							combody = field.substring(i, i + 1);
						}

                        String result = Globals.RTFCHARS.get(command + combody);

                        if (result != null)
							sb.append(result);

						incommand = false;
						escaped = false;
				
					}

				}

			} else {
				// if (!incommand || ((c!='{') && !Character.isWhitespace(c)))
				testContent: if (!incommand || (!Character.isWhitespace(c) && (c != '{')
                    && (c != '}')))
					sb.append(c);
				else {

                    // First test for braces that may be part of a LaTeX command:
                    if ((c == '{') && (currentCommand.length() == 0)) {
                        // We have seen something like \{, which is probably the start
                        // of a command like \{aa}. Swallow the brace.
                        continue;
                    } else if ((c == '}') && (currentCommand.length() > 0)) {
                        // Seems to be the end of a command like \{aa}. Look it up:
                        String command = currentCommand.toString();
                        String result = Globals.RTFCHARS.get(command);
                        if (result != null) {
                            sb.append(result);
                        }
                        incommand = false;
				        escaped = false;
                        continue;
                    }

                    // Then look for italics etc.,
                    // but first check if we are already at the end of the string.
					if (i >= field.length() - 1)
						break testContent;

					if ((c == '{') && (currentCommand.length() > 0)) {
                        String command = currentCommand.toString();
						// Then test if we are dealing with a italics or bold
						// command. If so, handle.
						if (command.equals("em") || command.equals("emph") || command.equals("textit")) {
							IntAndString part = getPart(field, i);
							i += part.i;
							sb.append("}{\\i ").append(part.s).append("}{");
						} else if (command.equals("textbf")) {
							IntAndString part = getPart(field, i);
							i += part.i;
							sb.append("}{\\b ").append(part.s).append("}{");
						}
                    } else
						sb.append(c);

				}
				incommand = false;
				escaped = false;
			}
		}

		char[] chars = sb.toString().toCharArray();
		sb = new StringBuffer();

		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];

			if (c < 128) 
				sb.append(c);
			 else
				sb.append("\\u").append((long) c).append('?');
		}

		return sb.toString().replaceAll("---", "{\\\\emdash}").replaceAll("--", "{\\\\endash}").replaceAll("``", "{\\\\ldblquote}").replaceAll("''","{\\\\rdblquote}");
	}

	private IntAndString getPart(String text, int i) {
		char c;
		int count = 0;
		StringBuffer part = new StringBuffer();
		while ((count >= 0) && (i < text.length())) {
			i++;
			c = text.charAt(i);
			if (c == '}')
				count--;
			else if (c == '{')
				count++;

			part.append(c);
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
