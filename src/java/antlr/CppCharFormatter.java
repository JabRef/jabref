package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

// C++ code generator by Pete Wells: pete@yamuna.demon.co.uk

class CppCharFormatter implements CharFormatter {

	/** Given a character value, return a string representing the character
	 * that can be embedded inside a string literal or character literal
	 * This works for Java/C/C++ code-generation and languages with compatible
	 * special-character-escapment.
	 *
	 * Used internally in CppCharFormatter and in
	 * CppCodeGenerator.converJavaToCppString.
	 *
	 * @param c   The character of interest.
	 * @param forCharLiteral  true to escape for char literal, false for string literal
	 */
	public String escapeChar(int c, boolean forCharLiteral) {
		// System.out.println("CppCharFormatter.escapeChar("+c+")");
		switch (c) {
		case '\n' : return "\\n";
		case '\t' : return "\\t";
		case '\r' : return "\\r";
		case '\\' : return "\\\\";
		case '\'' : return forCharLiteral ? "\\'" : "'";
		case '"' :  return forCharLiteral ? "\"" : "\\\"";
		default :
			if ( c < ' ' || c > 126 )
			{
				if (c > 255)
				{
					String s = Integer.toString(c,16);
					// put leading zeroes in front of the thing..
					while( s.length() < 4 )
						s = '0' + s;
					return "\\u" + s;
				}
				else {
					return "\\" + Integer.toString(c,8);
				}
			}
			else {
				return String.valueOf((char)c);
			}
		}
	}

	/** Converts a String into a representation that can be use as a literal
	 * when surrounded by double-quotes.
	 *
	 * Used for escaping semantic predicate strings for exceptions.
	 *
	 * @param s The String to be changed into a literal
	 */
	public String escapeString(String s)
	{
		String retval = new String();
		for (int i = 0; i < s.length(); i++)
			retval += escapeChar(s.charAt(i), false);

		return retval;
	}

	/** Given a character value, return a string representing the character
	 * literal that can be recognized by the target language compiler.
	 * This works for languages that use single-quotes for character literals.
	 * @param c The character of interest.
	 */
	public String literalChar(int c) {
		String ret = "0x"+Integer.toString(c,16);
		if( c >= 0 && c <= 126 )
			ret += " /* '"+escapeChar(c,true)+"' */ ";
		return ret;
	}

	/** Converts a String into a string literal
	 * This works for languages that use double-quotes for string literals.
	 * Code-generators for languages should override this method.
	 *
	 * Used for the generation of the tables with token names
	 *
	 * @param s The String to be changed into a literal
	 */
	public String literalString(String s)
	{
		return "\"" + escapeString(s) + "\"";
	}

}
