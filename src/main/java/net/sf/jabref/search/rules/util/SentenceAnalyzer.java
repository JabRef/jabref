package net.sf.jabref.search.rules.util;

import java.util.ArrayList;
import java.util.List;

public class SentenceAnalyzer {

    public static final char ESCAPE_CHAR = '\\';
    public static final char QUOTE_CHAR = '"';

    private final String query;

    public SentenceAnalyzer(String query) {
        this.query = query;
    }

    public List<String> getWords() {
        List<String> result = new ArrayList<String>();

        StringBuffer sb = new StringBuffer();
        boolean escaped = false;
        boolean quoted = false;
        for(char c : query.toCharArray()) {
            // Check if we are entering an escape sequence:
            if (!escaped && (c == ESCAPE_CHAR)) {
                escaped = true;
            } else {
                // See if we have reached the end of a word:
                if (!escaped && !quoted && Character.isWhitespace(c)) {
                    if (sb.length() > 0) {
                        result.add(sb.toString());
                        sb = new StringBuffer();
                    }
                } else if (c == QUOTE_CHAR) {
                    // Whether it is a start or end quote, store the current
                    // word if any:
                    if (sb.length() > 0) {
                        result.add(sb.toString());
                        sb = new StringBuffer();
                    }
                    quoted = !quoted;
                } else {
                    // All other possibilities exhausted, we add the char to
                    // the current word:
                    sb.append(c);
                }
                escaped = false;
            }
        }
        // Finished with the loop. If we have a current word, add it:
        if (sb.length() > 0) {
            result.add(sb.toString());
        }

        return result;
    }
}
