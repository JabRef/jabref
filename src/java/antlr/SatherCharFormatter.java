package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

import antlr.collections.impl.BitSet;

class SatherCharFormatter implements CharFormatter 
{

    /** Given a character value, return a string representing the character
     * that can be embedded inside a string literal or character literal
     * This works for Java/C/C++ code-generation and languages with compatible
     * special-character-escapment.
     * Code-generators for languages should override this method.
     * @param c   The character of interest.
     * @param forCharLiteral  true to escape for char literal, false for string literal
     */
    public String escapeChar(int c, boolean forCharLiteral) 
    {
	switch (c) {
	    //		case GrammarAnalyzer.EPSILON_TYPE : return "<end-of-token>";
	case '\n' : return "\\n";
	case '\t' : return "\\t";
	case '\r' : return "\\r";
	case '\\' : return "\\\\";
	case '\'' : return forCharLiteral ? "\\'" : "'";
	case '"' :  return forCharLiteral ? "\"" : "\\\"";
	default :
	    if ( c<' '||c>126 ) {
		if (c > 255) {
		    System.out.println("warning: Sather does not support 16-bit characters (..yet).");
		    return "\\u" + Integer.toString(c,16);
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
     * @param s The String to be changed into a literal
     */
    public String escapeString(String s)
    {
	String retval = new String();
	for (int i = 0; i < s.length(); i++)
	    {
		retval += escapeChar(s.charAt(i), false);
	    }
	return retval;
    }

    /** Given a character value, return a string representing the character
     * literal that can be recognized by the target language compiler.
     * This works for languages that use single-quotes for character literals.
     * Code-generators for languages should override this method.
     * @param c   The character of interest.
     */
    public String literalChar(int c) {
	return "'"  + escapeChar(c, true) + "'";
    }

    /** Converts a String into a string literal
     * This works for languages that use double-quotes for string literals.
     * Code-generators for languages should override this method.
     * @param s The String to be changed into a literal
     */
    public String literalString(String s)
    {
	return "\"" + escapeString(s) + "\"";
    }

    public String BitSet2BoolList( BitSet bs, String separator )
    {
	String result = new String();
	int bs_size = bs.size();
	for ( int i = 0 ; i < bs_size ; i++ ) {
	    if ( bs.member(i) )
		result += "true";
	    else
		result += "false";

	    if ( i < bs_size - 1 )
		result += separator;
	}

	return result;
    }

    public String BitSet2IntList( BitSet bs, String separator )
    {
	String result = new String();
	boolean first = true;
	for ( int i = 0 ; i < bs.size() ; i++ )
	    if ( bs.member(i) ) {

		if ( !first )
		    result += separator;
		else
		    first = false;

		result += i;
	    }

	return result;
    }
}
