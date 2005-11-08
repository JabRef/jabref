package gnu.dtools.ritopt;

/**
 * Utility.java
 *
 * Version:
 *    $Id$
 */

/**
 * This class provides static utility members for some basic string operations.
 *
 * <hr>
 *
 * <pre>
 * Copyright (C) Damian Ryan Eads, 2001. All Rights Reserved.
 *
 * ritopt is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * ritopt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ritopt; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * </pre>
 *
 * @author Damian Eads
 */

public class Utility {

    /**
     * Returns true if any of the characters in the list appear in the
     * check string passed.
     *
     * @param check The string to check.
     * @param list  The list of valid characters.
     *
     * @return true if the criteria of this method is satisfied.
     */

    public static boolean contains( String check, String list ) {
	for ( int n = 0; n < list.length(); n++ ) {
	    if ( check.indexOf( list.substring( n, n + 1 ) ) != -1 )
		return true;
	}
	return false;
    }

    /**
     * Returns the number of occurances the character specification
     * appears in the check string passed.
     *
     * @param check The string to check.
     * @param spec  The character specification.
     *
     * @return the number of occurances of the character specification.
     */

    public static int count( String check, char spec ) {
        int sum = 0;
	for ( int n = 0; n < check.length(); n++ ) {
	    if ( check.charAt( 0 ) == spec ) sum++;
	}
	return sum;
    }

    /**
     * Returns true if any of the characters in the list are equal to
     * the check character.
     *
     * @param check The character to check.
     * @param list  The list of valid characters.
     *
     * @return true if any of the characters in the list equal the check
     *              character. 
     */

    public static boolean contains( char check, String list ) {
	return contains( "" + check, list );
    }

    /**
     * Returns true if the string only contains letters in the phonetic
     * alphabet.
     *
     * @param check The string to check.
     *
     * @return If this method's criteria is matched.
     */

    public static boolean isAlpha( String check ) {
	boolean retval = false;
	for ( int n = 0; n < check.length(); n++ ) {
	    retval = isAlphaLower( check.charAt( n ) ) ||
		isAlphaUpper( check.charAt( n ) );
	}
	return retval;
    }

    /**
     * Returns true if the string only contains lower case letters in the
     * phonetic alphabet.
     *
     * @param check The string to check.
     *
     * @return If this method's criteria is matched.
     */

    public static boolean isAlphaLower( String check ) {
	boolean retval = false;
	for ( int n = 0; n < check.length(); n++ ) {
	    retval = isAlphaLower( check.charAt( n ) );
	}
	return retval;
    }

    /**
     * Returns true if the string only contains upper case letters in the
     * phonetic alphabet.
     *
     * @param check The string to check.
     *
     * @return If this method's criteria is matched.
     */

    public static boolean isAlphaUpper( String check ) {
	boolean retval = false;
	for ( int n = 0; n < check.length(); n++ ) {
	    retval = isAlphaUpper( check.charAt( n ) );
	}
	return retval;
    }

    /**
     * Returns true if the character is a letter in the phonetic alphabetic.
     *
     * @param check The character to check.
     *
     * @return true if this method's criteria is matched.
     */

    public static boolean isAlpha( char check ) {
	return isAlphaLower( check ) || isAlphaUpper( check );
    }

    /**
     * Returns true if the character is a lower case letter in the
     * phonetic alphabetic.
     *
     * @param check The character to check.
     *
     * @return true if this method's criteria is matched.
     */

    public static boolean isAlphaLower( char check ) {
	return check >= 'a' && check <= 'z';
    }

    /**
     * Returns true if the character is a upper case letter in the
     * phonetic alphabetic.
     *
     * @param check The character to check.
     *
     * @return true if this method's criteria is matched.
     */

    public static boolean isAlphaUpper( char check ) {
	return check >= 'A' && check <= 'Z';
    }

    /**
     * Returns true if the character is a letter in the phonetic alphabetic
     * or is a decimal number.
     *
     * @param check The character to check.
     *
     * @return true if this method's criteria is matched.
     */

    public static boolean isAlphaNumeric( char check ) {
	return isAlpha( check ) || isNumeric( check );
    }

    /**
     * Returns true if the character is a decimal number.
     *
     * @param check The character to check.
     *
     * @return true if this method's criteria is matched.
     */

    public static boolean isNumeric( char check ) {
	return check >= '0' && check <= '9'; 
    }

    /**
     * Returns a string with no more and no less than <em>n</em> characters
     * where n is the length. If the string passed is less than this length,
     * an appropriate number of spaces is concatenated. If the string is
     * longer than the length passed, a substring of the length passed
     * is returned.
     *
     * @param s       The string to expand.
     * @param length  The required length.
     *
     * @return The expanded string.
     */

    public static String expandString( String s, int length ) {
	if ( s.length() > length ) s = s.substring( 0, length );
	return s + getSpaces( length - s.length() );
    }

    /**
     * Returns a string containing the number of spaces passed as an
     * argument.
     *
     * @param count The number of spaces in the string returned.
     *
     * @return a string containing the number of spaces passed.
     */

    public static String getSpaces( int count ) {
	return repeat( ' ', count );
    }

    /**
     * Returns a string with a character repeated based on a count passed.
     *
     * @param c     The character to repeat.
     * @param count The number of times to repeat the character.
     */

    public static String repeat( char c, int count ) {
	StringBuffer retval = new StringBuffer( count );
	for ( int n = 0; n < count; n++ ) {
	    retval.append( c );
	}
	return retval.toString();
    }

    /**
     * Trim spaces off the left side of this string.
     *
     * @param s The string to trim.
     *
     * @return  The trimmed string.
     */

    public static String ltrim( String s ) {
	StringBuffer buf = new StringBuffer( s );
	for ( int n = 0; n < buf.length() && buf.charAt( n ) == ' '; ) {
	    buf.delete( 0, 1 );
	}
	return buf.toString();
    }

    /**
     * Trim spaces off the right side of this string.
     *
     * @param s The string to trim.
     *
     * @return  The trimmed string.
     */

    public static String rtrim( String s ) {
	StringBuffer buf = new StringBuffer( s );
	for ( int k = buf.length() - 1; k >= 0 && buf.charAt( k ) == ' ';
	      k = buf.length() - 1 ) {
	    buf.delete( buf.length() - 1, buf.length() );
	}
	return buf.toString();
    }

    /**
     * Trim spaces off both sides of this string.
     *
     * @param s The string to trim.
     *
     * @return  The trimmed string.
     */

    public static String trim( String s ) {
	return ltrim( rtrim( s ) );
    }

    /**
     * Takes a line (presumably from a file) and removes a comment if
     * one exists. If the comment character is enclosed within a literal
     * string defined by the delimiter passed, then the character is
     * ignored.
     *
     * @param s         The string to strip comments off.
     * @param delim     The string delimiter.
     * @param comment   The comment character.
     *
     * @return A string stripped of comments.
     */

    public static String stripComments( String s, char delim, char comment ) {
	String retval = s;
	boolean q = false;
	for ( int n = 0; n < s.length(); n++ ) {
	    if ( s.charAt( n ) == delim ) {
		q = !q;
	    }
	    else if ( !q && s.charAt( n ) == comment ) {
		retval = s.substring( 0, n );
	    }
	}
	return retval;
    }

    /**
     * Returns a string with the passed string repeated based on the 
     * integer count passed.
     *
     * @param s     The string to repeat.
     * @param count The number of times to repeat the string.
     *
     * @return      The repeated string.
     */

    public static String repeat( String s, int count ) {
	StringBuffer retval = new StringBuffer( s.length() * count );
	for ( int n = 0; n < count; n++ ) {
	    retval.append( s );
	}
	return retval.toString();
    }
}
