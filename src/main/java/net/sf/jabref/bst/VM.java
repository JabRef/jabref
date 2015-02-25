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
package net.sf.jabref.bst;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.AuthorList;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

/**
 * 
 * A Bibtex Virtual machine that can execute .bst files.
 * 
 * Documentation can be found in the original bibtex distribution:
 * 
 * http://texcatalogue.sarovar.org/entries/bibtex.html#Download
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */

public class VM implements Warn {

	PrintStream out = System.out;

	public class Identifier {
		public String name;

		public Identifier(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public class Variable {
		public String name;

		public Variable(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public interface BstFunction {
		public void execute(BstEntry context);
	}

	public static final Integer FALSE = 0;

	public static final Integer TRUE = 1;

	private HashMap<String,BstFunction> buildInFunctions;

	public File file;

	public VM(File f) throws RecognitionException, IOException {
		this(new ANTLRFileStream(f.getPath()));
		this.file = f;
	}

	public VM(String s) throws RecognitionException {
		this(new ANTLRStringStream(s));
	}

	public static CommonTree charStream2CommonTree(CharStream bst) throws RecognitionException {
		BstLexer lex = new BstLexer(bst);
		CommonTokenStream tokens = new CommonTokenStream(lex);
		BstParser parser = new BstParser(tokens);
		BstParser.program_return r = parser.program();
		return (CommonTree) r.getTree();
	}

	public VM(CharStream bst) throws RecognitionException {
		this(charStream2CommonTree(bst));
	}

	public VM(CommonTree tree) {
		this.tree = tree;

		this.buildInFunctions = new HashMap<String, BstFunction>(37);

		buildInFunctions.put(">", new BstFunction() {
			/**
			 * Pops the top two (integer) literals, compares them, and pushes
			 * the integer 1 if the second is greater than the first, 0
			 * otherwise.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 2) {
					throw new VMException("Not enough operands on stack for operation >");
				}
				Object o2 = stack.pop();
				Object o1 = stack.pop();

				if (!(o1 instanceof Integer && o2 instanceof Integer)) {
					throw new VMException("Can only compare two integers with >");
				}

				if (o1 == o2) {
					stack.push(VM.FALSE);
					return;
				}

				stack.push(((Integer) o1).compareTo((Integer) o2) > 0 ? VM.TRUE : VM.FALSE);
			}
		});

		buildInFunctions.put("<", new BstFunction() {
			/** Analogous. */
			public void execute(BstEntry context) {
				if (stack.size() < 2) {
					throw new VMException("Not enough operands on stack for operation <");
				}
				Object o2 = stack.pop();
				Object o1 = stack.pop();

				if (!(o1 instanceof Integer && o2 instanceof Integer)) {
					throw new VMException("Can only compare two integers with <");
				}

				if (o1 == o2) {
					stack.push(VM.FALSE);
					return;
				}

				stack.push(((Integer) o1).compareTo((Integer) o2) < 0 ? VM.TRUE : VM.FALSE);

			}
		});

		buildInFunctions.put("=", new BstFunction() {
			/**
			 * Pops the top two (both integer or both string) literals, compares
			 * them, and pushes the integer 1 if they're equal, 0 otherwise.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 2) {
					throw new VMException("Not enough operands on stack for operation =");
				}
				Object o1 = stack.pop();
				Object o2 = stack.pop();

				if (o1 == null ^ o2 == null) {
					stack.push(VM.FALSE);
					return;
				}

				if (o1 == o2) {
					stack.push(VM.TRUE);
					return;
				}

				stack.push(o1.equals(o2) ? VM.TRUE : VM.FALSE);
			}
		});

		buildInFunctions.put("+", new BstFunction() {
			/** Pops the top two (integer) literals and pushes their sum. */
			public void execute(BstEntry context) {
				if (stack.size() < 2) {
					throw new VMException("Not enough operands on stack for operation +");
				}
				Object o2 = stack.pop();
				Object o1 = stack.pop();

				if (!(o1 instanceof Integer && o2 instanceof Integer)) {
					throw new VMException("Can only compare two integers with +");
				}

				stack.push((Integer) o1 + (Integer) o2);
			}
		});

		buildInFunctions.put("-", new BstFunction() {
			/**
			 * Pops the top two (integer) literals and pushes their difference
			 * (the first subtracted from the second).
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 2) {
					throw new VMException("Not enough operands on stack for operation -");
				}
				Object o2 = stack.pop();
				Object o1 = stack.pop();

				if (!(o1 instanceof Integer && o2 instanceof Integer)) {
					throw new VMException("Can only subtract two integers with -");
				}

				stack.push((Integer) o1 - (Integer) o2);
			}
		});

		buildInFunctions.put("*", new BstFunction() {
			/**
			 * Pops the top two (string) literals, concatenates them (in reverse
			 * order, that is, the order in which pushed), and pushes the
			 * resulting string.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 2) {
					throw new VMException("Not enough operands on stack for operation *");
				}
				Object o2 = stack.pop();
				Object o1 = stack.pop();

				if (!(o1 instanceof String && o2 instanceof String)) {
					throw new VMException("Can only concatenate two String with *");
				}

				stack.push(o1.toString() + o2.toString());
			}
		});

		buildInFunctions.put(":=", new BstFunction() {
			/**
			 * Pops the top two literals and assigns to the first (which must be
			 * a global or entry variable) the value of the second.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 2) {
					throw new VMException("Invalid call to operation :=");
				}
				Object o1 = stack.pop();
				Object o2 = stack.pop();
				assign(context, o1, o2);

			}
		});

		buildInFunctions.put("add.period$", new BstFunction() {

			Pattern p = Pattern.compile("([^\\.\\?\\!\\}\\s])(\\}|\\s)*$");

			/**
			 * Pops the top (string) literal, adds a `.' to it if the last non
			 * '}' character isn't a `.', `?', or `!', and pushes this resulting
			 * string.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 1) {
					throw new VMException("Not enough operands on stack for operation add.period$");
				}
				Object o1 = stack.pop();

				if (!(o1 instanceof String)) {
					throw new VMException("Can only add a period to a string for add.period$");
				}

				String s = (String) o1;
				Matcher m = p.matcher(s);

				if (m.find()) {
					StringBuffer sb = new StringBuffer();
					m.appendReplacement(sb, m.group(1));
					sb.append('.');
					String group2 = m.group(2);
					if (group2 != null)
						sb.append(m.group(2));
					stack.push(sb.toString());
				} else {
					stack.push(s);
				}
			}
		});

		buildInFunctions.put("call.type$", new BstFunction() {
			/**
			 * Executes the function whose name is the entry type of an entry.
			 * For example if an entry is of type book, this function executes
			 * the book function. When given as an argument to the ITERATE
			 * command, call.type$ actually produces the output for the entries.
			 * For an entry with an unknown type, it executes the function
			 * default.type. Thus you should define (before the READ command)
			 * one function for each standard entry type as well as a
			 * default.type function.
			 */
			public void execute(BstEntry context) {

				if (context == null) {
					throw new VMException(
						"Call.type$ can only be called from within a context (ITERATE or REVERSE).");
				}
				VM.this.execute(context.entry.getType().getName().toLowerCase(), context);
			}
		});

		buildInFunctions.put("change.case$", new ChangeCaseFunction(this));

		buildInFunctions.put("chr.to.int$", new BstFunction() {
			/**
			 * Pops the top (string) literal, makes sure it's a single
			 * character, converts it to the corresponding ASCII integer, and
			 * pushes this integer.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 1) {
					throw new VMException("Not enough operands on stack for operation chr.to.int$");
				}
				Object o1 = stack.pop();

				if (!(o1 instanceof String && ((String) o1).length() == 1)) {
					throw new VMException("Can only perform chr.to.int$ on string with length 1");
				}

				String s = (String) o1;

				stack.push((int) s.charAt(0));
			}
		});

		buildInFunctions.put("cite$", new BstFunction() {
			/**
			 * Pushes the string that was the \cite-command argument for this
			 * entry.
			 */
			public void execute(BstEntry context) {
				stack.push(context.entry.getCiteKey());
			}
		});

		buildInFunctions.put("duplicate$", new BstFunction() {
			/**
			 * Pops the top literal from the stack and pushes two copies of it.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 1) {
					throw new VMException("Not enough operands on stack for operation duplicate$");
				}
				Object o1 = stack.pop();

				stack.push(o1);
				stack.push(o1);
			}
		});

		buildInFunctions.put("empty$", new BstFunction() {
			/**
			 * Pops the top literal and pushes the integer 1 if it's a missing
			 * field or a string having no non-white-space characters, 0
			 * otherwise.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 1) {
					throw new VMException("Not enough operands on stack for operation empty$");
				}
				Object o1 = stack.pop();

				if (o1 == null) {
					stack.push(VM.TRUE);
					return;
				}

				if (!(o1 instanceof String)) {
					throw new VMException("Operand does not match function empty$");
				}

				String s = (String) o1;

				stack.push(s.trim().equals("") ? VM.TRUE : VM.FALSE);
			}
		});

		buildInFunctions.put("format.name$", new FormatNameFunction(this));

		buildInFunctions.put("if$", new BstFunction() {
			/**
			 * Pops the top three literals (they are two function literals and
			 * an integer literal, in that order); if the integer is greater
			 * than 0, it executes the second literal, else it executes the
			 * first.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 3) {
					throw new VMException("Not enough operands on stack for operation =");
				}
				Object f1 = stack.pop();
				Object f2 = stack.pop();
				Object i = stack.pop();

				if (!(f1 instanceof Identifier || f1 instanceof Tree)
					&& (f2 instanceof Identifier || f2 instanceof Tree) && (i instanceof Integer)) {
					throw new VMException("Expecting two functions and an integer for if$.");
				}

				Object toExe;
				if ((Integer) i > 0) {
					toExe = f2;
				} else {
					toExe = f1;
				}
				VM.this.executeInContext(toExe, context);
			}
		});

		buildInFunctions.put("int.to.chr$", new BstFunction() {
			/**
			 * Pops the top (integer) literal, interpreted as the ASCII integer
			 * value of a single character, converts it to the corresponding
			 * single-character string, and pushes this string.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 1) {
					throw new VMException("Not enough operands on stack for operation int.to.chr$");
				}
				Object o1 = stack.pop();

				if (!(o1 instanceof Integer)) {
					throw new VMException("Can only perform operation int.to.chr$ on an Integer");
				}

				Integer i = (Integer) o1;

				stack.push(String.valueOf((char) i.intValue()));
			}
		});

		buildInFunctions.put("int.to.str$", new BstFunction() {
			/**
			 * Pops the top (integer) literal, converts it to its (unique)
			 * string equivalent, and pushes this string.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 1) {
					throw new VMException("Not enough operands on stack for operation int.to.str$");
				}
				Object o1 = stack.pop();

				if (!(o1 instanceof Integer)) {
					throw new VMException(
						"Can only transform an integer to an string using int.to.str$");
				}

				stack.push(o1.toString());
			}
		});

		buildInFunctions.put("missing$", new BstFunction() {
			/**
			 * Pops the top literal and pushes the integer 1 if it's a missing
			 * field, 0 otherwise.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 1) {
					throw new VMException("Not enough operands on stack for operation missing$");
				}
				Object o1 = stack.pop();

				if (o1 == null) {
					stack.push(VM.TRUE);
					return;
				}

				if (!(o1 instanceof String)) {
					warn("Not a string or missing field in operation missing$");
					stack.push(VM.TRUE);
					return;
				}

				stack.push(VM.FALSE);
			}
		});

		buildInFunctions.put("newline$", new BstFunction() {
			/**
			 * Writes onto the bbl file what's accumulated in the output buffer.
			 * It writes a blank line if and only if the output buffer is empty.
			 * Since write$ does reasonable line breaking, you should use this
			 * function only when you want a blank line or an explicit line
			 * break.
			 */
			public void execute(BstEntry context) {
				VM.this.bbl.append('\n');
			}
		});

		buildInFunctions.put("num.names$", new BstFunction() {
			/**
			 * Pops the top (string) literal and pushes the number of names the
			 * string represents one plus the number of occurrences of the
			 * substring "and" (ignoring case differences) surrounded by
			 * non-null white-space at the top brace level.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 1) {
					throw new VMException("Not enough operands on stack for operation num.names$");
				}
				Object o1 = stack.pop();

				if (!(o1 instanceof String)) {
					throw new VMException("Need a string at the top of the stack for num.names$");
				}
				String s = (String) o1;

				stack.push(AuthorList.getAuthorList(s).size());
			}
		});

		buildInFunctions.put("pop$", new BstFunction() {
			/**
			 * Pops the top of the stack but doesn't print it; this gets rid of
			 * an unwanted stack literal.
			 */
			public void execute(BstEntry context) {
				stack.pop();
			}
		});

		buildInFunctions.put("preamble$", new BstFunction() {
			/**
			 * The |built_in| function {\.{preamble\$}} pushes onto the stack
			 * the concatenation of all the \.{preamble} strings read from the
			 * database files. (or the empty string if there where none)
			 * 
			 * @PREAMBLE strings read from the database files.
			 */
			public void execute(BstEntry context) {
				if (preamble != null) {
					stack.push(preamble);
				} else {
					stack.push("");
				}

			}
		});

		/**
		 * Pops the top (string) literal, removes nonalphanumeric characters
		 * except for white-space characters and hyphens and ties (these all get
		 * converted to a space), removes certain alphabetic characters
		 * contained in the control sequences associated with a \special
		 * character", and pushes the resulting string.
		 */
		buildInFunctions.put("purify$", new PurifyFunction(this));

		buildInFunctions.put("quote$", new BstFunction() {
			/**
			 * Pushes the string consisting of the double-quote character.
			 */
			public void execute(BstEntry context) {
				stack.push("\"");
			}
		});

		buildInFunctions.put("skip$", new BstFunction() {
			/**
			 * Is a no-op.
			 */
			public void execute(BstEntry context) {
				// Nothing to do! Yeah!
			}
		});

		buildInFunctions.put("stack$", new BstFunction() {
			/**
			 * Pops and prints the whole stack; it's meant to be used for style
			 * designers while debugging.
			 */
			public void execute(BstEntry context) {
				while (!stack.empty()) {
					System.out.println(stack.pop());
				}
			}
		});

		buildInFunctions.put("substring$", new BstFunction() {
			/**
			 * Pops the top three literals (they are the two integers literals
			 * len and start, and a string literal, in that order). It pushes
			 * the substring of the (at most) len consecutive characters
			 * starting at the startth character (assuming 1-based indexing) if
			 * start is positive, and ending at the start-th character
			 * (including) from the end if start is negative (where the first
			 * character from the end is the last character).
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 3) {
					throw new VMException("Not enough operands on stack for operation substring$");
				}
				Object o1 = stack.pop();
				Object o2 = stack.pop();
				Object o3 = stack.pop();

				if (!((o1 instanceof Integer) && (o2 instanceof Integer) && (o3 instanceof String))) {
					throw new VMException("Expecting two integers and a string for substring$");
				}

				Integer len = (Integer) o1;
				Integer start = (Integer) o2;

				int lenI = len;
				int startI = start;

				if (lenI > Integer.MAX_VALUE / 2)
					lenI = Integer.MAX_VALUE / 2;

				if (startI > Integer.MAX_VALUE / 2)
					startI = Integer.MAX_VALUE / 2;

				if (startI < Integer.MIN_VALUE / 2)
					startI = -Integer.MIN_VALUE / 2;

				String s = (String) o3;

				if (startI < 0) {
					startI += s.length() + 1;
					startI = Math.max(1, startI + 1 - lenI);
				}
				stack.push(s.substring(startI - 1, Math.min(startI - 1 + lenI, s.length())));
			}
		});

		buildInFunctions.put("swap$", new BstFunction() {
			/**
			 * Swaps the top two literals on the stack. text.length$ Pops the
			 * top (string) literal, and pushes the number of text char- acters
			 * it contains, where an accented character (more precisely, a
			 * \special character", defined in Section 4) counts as a single
			 * text character, even if it's missing its matching right brace,
			 * and where braces don't count as text characters.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 2) {
					throw new VMException("Not enough operands on stack for operation swap$");
				}
				Object f1 = stack.pop();
				Object f2 = stack.pop();

				stack.push(f1);
				stack.push(f2);
			}
		});

		buildInFunctions.put("text.length$", new BstFunction() {
			/**
			 * text.length$ Pops the top (string) literal, and pushes the number
			 * of text characters it contains, where an accented character (more
			 * precisely, a "special character", defined in Section 4) counts as
			 * a single text character, even if it's missing its matching right
			 * brace, and where braces don't count as text characters.
			 * 
			 * From BibTeXing: For the purposes of counting letters in labels,
			 * BibTEX considers everything contained inside the braces as a
			 * single letter.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 1) {
					throw new VMException("Not enough operands on stack for operation text.length$");
				}
				Object o1 = stack.pop();

				if (!(o1 instanceof String)) {
					throw new VMException("Can only perform operation on a string text.length$");
				}

				String s = (String) o1;
				char[] c = s.toCharArray();
				int result = 0;

				// Comments from bibtex.web:

				// sp_ptr := str_start[pop_lit1];
				int i = 0;

				// sp_end := str_start[pop_lit1+1];
				int n = s.length();

				// sp_brace_level := 0;
				int braceLevel = 0;

				// while (sp_ptr < sp_end) do begin
				while (i < n) {
					// incr(sp_ptr);
					i++;
					// if (str_pool[sp_ptr-1] = left_brace) then
					// begin
					if (c[i - 1] == '{') {
						// incr(sp_brace_level);
						braceLevel++;
						// if ((sp_brace_level = 1) and (sp_ptr < sp_end)) then
						if (braceLevel == 1 && i < n)
							// if (str_pool[sp_ptr] = backslash) then
							// begin
							if (c[i] == '\\') {
								// incr(sp_ptr); {skip over the |backslash|}
								i++; // skip over backslash
								// while ((sp_ptr < sp_end) and (sp_brace_level
								// > 0)) do begin
								while (i < n && braceLevel > 0) {
									// if (str_pool[sp_ptr] = right_brace) then
									if (c[i] == '}')
										// decr(sp_brace_level)
										braceLevel--;
									// else if (str_pool[sp_ptr] = left_brace)
									// then
									else if (c[i] == '{')

										// incr(sp_brace_level);
										braceLevel++;
									// incr(sp_ptr);
									i++;
									// end;
								}
								// incr(num_text_chars);
								result++;
								// end;
							}
						// end
					}
					// else if (str_pool[sp_ptr-1] = right_brace) then
					// begin
					else if (c[i - 1] == '}') {
						// if (sp_brace_level > 0) then
						if (braceLevel > 0)
							// decr(sp_brace_level);
							braceLevel--;
						// end
					}
					// else
					else
						// incr(num_text_chars);
						result++;
				}
				stack.push(result);
			}
		});

		/**
		 * Pops the top two literals (the integer literal len and a string
		 * literal, in that order). It pushes the substring of the (at most) len
		 * consecutive text characters starting from the beginning of the
		 * string. This function is similar to substring$, but this one
		 * considers a \special character", even if it's missing its matching
		 * right brace, to be a single text character (rather than however many
		 * ASCII characters it actually comprises), and this function doesn't
		 * consider braces to be text characters; furthermore, this function
		 * appends any needed matching right braces.
		 */
		buildInFunctions.put("text.prefix$", new TextPrefixFunction(this));

		buildInFunctions.put("top$", new BstFunction() {
			/**
			 * Pops and prints the top of the stack on the terminal and log
			 * file. It's useful for debugging.
			 */
			public void execute(BstEntry context) {
				System.out.println(stack.pop());
			}
		});

		buildInFunctions.put("type$", new BstFunction() {
			/**
			 * Pushes the current entry's type (book, article, etc.), but pushes
			 * the null string if the type is either unknown or undefined.
			 */
			public void execute(BstEntry context) {
				stack.push(context.entry.getType().getName());
			}
		});

		buildInFunctions.put("warning$", new BstFunction() {
			/**
			 * Pops the top (string) literal and prints it following a warning
			 * message. This also increments a count of the number of warning
			 * messages issued.
			 */
			int warning = 1;

			public void execute(BstEntry context) {
				out.println("Warning (#" + (warning++) + "): " + stack.pop());
			}
		});

		buildInFunctions.put("while$", new BstFunction() {
			/**
			 * Pops the top two (function) literals, and keeps executing the
			 * second as long as the (integer) literal left on the stack by
			 * executing the first is greater than 0.
			 */
			public void execute(BstEntry context) {
				if (stack.size() < 2) {
					throw new VMException("Not enough operands on stack for operation while$");
				}
				Object f2 = stack.pop();
				Object f1 = stack.pop();

				if (!(f1 instanceof Identifier || f1 instanceof Tree)
					&& (f2 instanceof Identifier || f2 instanceof Tree)) {
					throw new VMException("Expecting two functions for while$.");
				}

				do {
					VM.this.executeInContext(f1, context);

					Object i = stack.pop();
					if (!(i instanceof Integer)) {
						throw new VMException(
							"First parameter to while has to return an integer but was " + i);
					}
					if ((Integer) i <= 0) {
						break;
					}
					VM.this.executeInContext(f2, context);
				} while (true);
			}
		});

		buildInFunctions.put("width$", new WidthFunction(this));

		buildInFunctions.put("write$", new BstFunction() {
			/**
			 * Pops the top (string) literal and writes it on the output buffer
			 * (which will result in stuff being written onto the bbl file when
			 * the buffer fills up).
			 */
			public void execute(BstEntry context) {
				String s = (String) stack.pop();
				System.out.println(s);
				VM.this.bbl.append(s);
			}
		});

	}

	protected boolean assign(BstEntry context, Object o1, Object o2) {

		if (!(o1 instanceof Identifier) || !(o2 instanceof String || o2 instanceof Integer))
			throw new VMException("Invalid parameters");

		String name = ((Identifier) o1).getName();

		if (o2 instanceof String) {

			if (context != null && context.strings.containsKey(name)) {
				context.strings.put(name, (String) o2);
				return true;
			}

			if (strings.containsKey(name)) {
				strings.put(name, (String) o2);
				return true;
			}
			return false;

		}

        if (context != null && context.integers.containsKey(name)) {
            context.integers.put(name, (Integer) o2);
            return true;
        }

        if (integers.containsKey(name)) {
            integers.put(name, (Integer) o2);
            return true;
        }
        return false;
	}

	CommonTree tree;

	private StringBuffer bbl;

	String preamble;

	public String run(BibtexDatabase db) {
		preamble = db.getPreamble();
		return run(db.getEntries());
	}

	public String run(Collection<BibtexEntry> bibtex) {

		reset();

		{ // Create entries
			entries = new Vector<BstEntry>(bibtex.size());
			ListIterator<BstEntry> i = entries.listIterator();
			for (BibtexEntry entry : bibtex){
				i.add(new BstEntry(entry));
			}
		}

		// assert tree.getType() == Bst.COMMANDS;

		// Go
		for (int i = 0; i < tree.getChildCount(); i++) {
			Tree child = tree.getChild(i);
			switch (child.getType()) {
			case BstParser.STRINGS:
				strings(child);
				break;
			case BstParser.INTEGERS:
				integers(child);
				break;
			case BstParser.FUNCTION:
				function(child);
				break;
			case BstParser.EXECUTE:
				execute(child);
				break;
			case BstParser.SORT:
				sort(child);
				break;
			case BstParser.ITERATE:
				iterate(child);
				break;
			case BstParser.REVERSE:
				reverse(child);
				break;
			case BstParser.ENTRY:
				entry(child);
				break;
			case BstParser.READ:
				read();
				break;
			case BstParser.MACRO:
				macro(child);
				break;
			}
		}

		return bbl.toString();
	}

	private void reset() {
		bbl = new StringBuffer();

		entries = null;

		strings = new HashMap<String, String>();

		integers = new HashMap<String, Integer>();
		integers.put("entry.max$", Integer.MAX_VALUE);
		integers.put("global.max$", Integer.MAX_VALUE);

		functions = new HashMap<String, BstFunction>();
		functions.putAll(buildInFunctions);

		stack = new Stack<Object>();
	}

	/**
	 * Dredges up from the database file the field values for each entry in the
	 * list. It has no arguments. If a database entry doesn't have a value for a
	 * field (and probably no database entry will have a value for every field),
	 * that field variable is marked as missing for the entry.
	 * 
	 * We use null for the missing entry designator.
	 */
	private void read() {

        for(BstEntry e : entries) {

			for (Map.Entry<String, String> mEntry : e.fields.entrySet()){
				Object fieldValue = e.entry.getField(mEntry.getKey());

				mEntry.setValue((fieldValue == null ? null : fieldValue.toString()));
			}
		}

        for(BstEntry e : entries) {
			if (!e.fields.containsKey("crossref")) {
				e.fields.put("crossref", null);
			}
		}
	}

	/**
	 * Defines a string macro. It has two arguments; the first is the macro's
	 * name, which is treated like any other variable or function name, and the
	 * second is its definition, which must be double-quote-delimited. You must
	 * have one for each three-letter month abbreviation; in addition, you
	 * should have one for common journal names. The user's database may
	 * override any definition you define using this command. If you want to
	 * define a string the user can't touch, use the FUNCTION command, which has
	 * a compatible syntax.
	 * 
	 * @param child
	 */
	private void macro(Tree child) {
		String name = child.getChild(0).getText();
		String replacement = child.getChild(1).getText();
		functions.put(name, new MacroFunction(replacement));
	}

	public class MacroFunction implements BstFunction {

		String replacement;

		public MacroFunction(String replacement) {
			this.replacement = replacement;
		}

		public void execute(BstEntry context) {
			VM.this.push(replacement);
		}
	}

	/*
	 * Declares the fields and entry variables. It has three arguments, each a
	 * (possibly empty) list of variable names. The three lists are of: fields,
	 * integer entry variables, and string entry variables. There is an
	 * additional field that BibTEX automatically declares, crossref, used for
	 * cross ref- erencing. And there is an additional string entry variable
	 * automatically declared, sort.key$, used by the SORT command. Each of
	 * these variables has a value for each entry on the list.
	 */
	private void entry(Tree child) {

		{ // Fields first
			Tree t = child.getChild(0);
			// assert t.getType() == Bst.IDLIST;

			for (int i = 0; i < t.getChildCount(); i++) {
				String name = t.getChild(i).getText();

				for (BstEntry entry : entries){
					entry.fields.put(name, null);
				}
			}
		}
		{ // Integers
			Tree t = child.getChild(1);
			// assert t.getType() == Bst.IDLIST;

			for (int i = 0; i < t.getChildCount(); i++) {
				String name = t.getChild(i).getText();
				
				for (BstEntry entry : entries){
					entry.integers.put(name, 0);
				}
			}
		}
		{ // Strings
			Tree t = child.getChild(2);
			// assert t.getType() == Bst.IDLIST;

			for (int i = 0; i < t.getChildCount(); i++) {
				String name = t.getChild(i).getText();
				for (BstEntry entry : entries){
					entry.strings.put(name, null);
				}
			}
			for (BstEntry entry : entries){
				entry.strings.put("sort.key$", null);
			}
		}
	}

	private void reverse(Tree child) {

		BstFunction f = functions.get(child.getChild(0).getText());

		ListIterator<BstEntry> i = entries.listIterator(entries.size());
		while (i.hasPrevious()) {
			f.execute(i.previous());
		}
	}

	private void iterate(Tree child) {
		BstFunction f = functions.get(child.getChild(0).getText());

        for (BstEntry entry : entries) {
            f.execute(entry);
        }
	}

	/**
	 * Sorts the entry list using the values of the string entry variable
	 * sort.key$. It has no arguments.
	 * 
	 * @param child
	 */
	private void sort(Tree child) {
		Collections.sort(entries, new Comparator<BstEntry>() {
			public int compare(BstEntry o1, BstEntry o2) {
				return (o1.strings.get("sort.key$")).compareTo(o2.strings
					.get("sort.key$"));
			}
		});
	}

	public void executeInContext(Object o, BstEntry context) {
		if (o instanceof Tree) {
			Tree t = (Tree) o;
			new StackFunction(t).execute(context);
		} else if (o instanceof Identifier) {
			execute(((Identifier) o).getName(), context);
		}
	}

	public void execute(Tree child) {
		execute(child.getChild(0).getText(), null);
	}

	public class StackFunction implements BstFunction {

		Tree tree;

		public Tree getTree() {
			return tree;
		}

		public StackFunction(Tree stack) {
			// assert stack.getType() == Bst.STACK;
			tree = stack;
		}

		public void execute(BstEntry context) {

			for (int i = 0; i < tree.getChildCount(); i++) {

				Tree c = tree.getChild(i);
				try {

					switch (c.getType()) {
					case BstParser.STRING: {
						String s = c.getText();
						push(s.substring(1, s.length() - 1));
					}
						break;
					case BstParser.INTEGER:
						push(Integer.parseInt(c.getText().substring(1)));
						break;
					case BstParser.QUOTED:
						push(new Identifier(c.getText().substring(1)));
						break;
					case BstParser.STACK:
						push(c);
						break;
					default:
						VM.this.execute(c.getText(), context);
					}
				} catch (VMException e) {
					if (file != null) {
						System.err.println("ERROR " + e.getMessage() + " (" + file.getPath() + ":"
							+ c.getLine() + ")");
					} else {
						System.err.println("ERROR " + e.getMessage() + " (" + c.getLine() + ")");
					}
					throw e;
				}
			}

		}
	}

	private void push(Tree t) {
		stack.push(t);
	}

	public void execute(String name, BstEntry context) {

		if (context != null) {

			if (context.fields.containsKey(name)) {
				stack.push(context.fields.get(name));
				return;
			}
			if (context.strings.containsKey(name)) {
				stack.push(context.strings.get(name));
				return;
			}
			if (context.integers.containsKey(name)) {
				stack.push(context.integers.get(name));
				return;
			}
		}
		if (strings.containsKey(name)) {
			stack.push(strings.get(name));
			return;
		}
		if (integers.containsKey(name)) {
			stack.push(integers.get(name));
			return;
		}

		if (functions.containsKey(name)) {
			functions.get(name).execute(context);
			return;
		}

		throw new VMException("No matching identifier found: " + name);
	}

	private void function(Tree child) {
		String name = child.getChild(0).getText();
		Tree stack = child.getChild(1);
		functions.put(name, new StackFunction(stack));

	}

	/**
	 * Declares global integer variables. It has one argument, a list of
	 * variable names. There are two such automatically-declared variables,
	 * entry.max$ and global.max$, used for limiting the lengths of string vari-
	 * ables. You may have any number of these commands, but a variable's
	 * declaration must precede its use.
	 * 
	 * @param child
	 */
	private void integers(Tree child) {
		Tree t = child.getChild(0);
		// assert t.getType() == Bst.IDLIST;

		for (int i = 0; i < t.getChildCount(); i++) {
			String name = t.getChild(i).getText();
			integers.put(name, 0);
		}
	}

	/**
	 * Declares global string variables. It has one argument, a list of variable
	 * names. You may have any number of these commands, but a variable's
	 * declaration must precede its use.
	 * 
	 * @param child
	 */
	private void strings(Tree child) {
		Tree t = child.getChild(0);
		// assert t.getType() == Bst.IDLIST;

		for (int i = 0; i < t.getChildCount(); i++) {
			String name = t.getChild(i).getText();
			strings.put(name, null);
		}
	}

	public class BstEntry {

		public BstEntry(BibtexEntry e) {
			this.entry = e;
		}

		BibtexEntry entry;

		Map<String, String> strings = new HashMap<String, String>();

		Map<String, String> fields = new HashMap<String, String>();

		Map<String, Integer> integers = new HashMap<String, Integer>();

		public Map<String, String> getFields() {
			return fields;
		}

		public BibtexEntry getBibtexEntry() {
			return entry;
		}
	}

	Vector<BstEntry> entries;
	
	Map<String, String> strings = new HashMap<String, String>();
	
	Map<String, Integer> integers = new HashMap<String, Integer>();
	
	Map<String, BstFunction> functions = new HashMap<String, BstFunction>();
	
	Stack<Object> stack = new Stack<Object>();
	
	public void push(Integer integer) {
		stack.push(integer);
	}

	public void push(String string) {
		stack.push(string);
	}

	public void push(Identifier identifier) {
		stack.push(identifier);
	}

	
	  public Map<String, String> getStrings() { return strings; }
	  
	  public Map<String, Integer> getIntegers() { return integers; }
	  
	  public Vector<BstEntry> getEntries() { return entries; }
	  
	 public Map<String, BstFunction> getFunctions() { return functions; }

	public Stack<Object> getStack() {
		return stack;
	}

	public void warn(String string) {
		System.out.println(string);
	}

}
