package net.sf.jabref.groups;

/**
 * A String tokenizer that works just like StringTokenizer, but considers quoted
 * characters (which do not act as delimiters).
 * 
 * JZTODO: move to some suitable location
 */
public class QuotedStringTokenizer {
    private final String m_content;
    private final int m_contentLength;
    private final String m_delimiters;
    private final char m_quoteChar;
    private int m_index = 0;
    
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
