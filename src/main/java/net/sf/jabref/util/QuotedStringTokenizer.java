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
package net.sf.jabref.util;

/**
 * A String tokenizer that works just like StringTokenizer, but considers quoted
 * characters (which do not act as delimiters).
 */
public class QuotedStringTokenizer {
    private final String m_content;
    private final int m_contentLength;
    private final String m_delimiters;
    private final char m_quoteChar;
    private int m_index = 0;
    
    /**
     * @param content
     *            The String to be tokenized.
     * @param delimiters
     *            The delimiter characters.
     * @param quoteCharacter
     *            The quoting character. Every character (including, but not
     *            limited to, delimiters) that is preceded by this character is
     *            not treated as a delimiter, but as a token component.
     */
    public QuotedStringTokenizer(String content, String delimiters, char quoteCharacter) {
        m_content = content;
        m_delimiters = delimiters;
        m_quoteChar = quoteCharacter;
        m_contentLength = m_content.length();
        // skip leading delimiters
        while (isDelimiter(m_content.charAt(m_index)) && m_index < m_contentLength)
            ++m_index;
    }
    
    public String nextToken() {
        char c;
        StringBuffer sb = new StringBuffer();
        while (m_index < m_contentLength) {
            c = m_content.charAt(m_index);
    		if (c == m_quoteChar) { // next is quoted
    		    ++m_index;
    		    if (m_index < m_contentLength) // sanity check
    		        sb.append(m_content.charAt(m_index));
    			// ignore for delimiter search!
    		} else if (isDelimiter(c)) { // unit finished
    		    // advance index until next token or end
    		    do {
    		        ++m_index;
    		    } while (m_index < m_contentLength && isDelimiter(m_content.charAt(m_index)));
    		    return sb.toString();
    		}
   			else
                sb.append(c);
   			++m_index;
    	}
        return sb.toString();
    }
    
    private boolean isDelimiter(char c) {
        return m_delimiters.indexOf(c) >= 0;
    }
    
    public boolean hasMoreTokens() {
        return m_index < m_contentLength;
    }
}
