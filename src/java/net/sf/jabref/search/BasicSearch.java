package net.sf.jabref.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.SearchRule;
import net.sf.jabref.export.layout.format.RemoveLatexCommands;

/**
 * Search rule for simple search.
 */
public class BasicSearch implements SearchRule {
    private boolean caseSensitive;
    private boolean regExp;
    Pattern[] pattern;
    //static RemoveBrackets removeLatexCommands = new RemoveBrackets();
    static RemoveLatexCommands removeBrackets = new RemoveLatexCommands();

    public BasicSearch(boolean caseSensitive, boolean regExp) {

        this.caseSensitive = caseSensitive;
        this.regExp = regExp;
    }

    public int applyRule(String query, BibtexEntry bibtexEntry) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("1", query);
        return applyRule(map, bibtexEntry);
    }

    public int applyRule(Map<String, String> searchStrings, BibtexEntry bibtexEntry) {

        int flags = 0;
        String searchString = searchStrings.values().iterator().next();
        if (!caseSensitive) {
            searchString = searchString.toLowerCase();
            flags = Pattern.CASE_INSENSITIVE;
        }

        ArrayList<String> words = parseQuery(searchString);

        if (regExp) {
            pattern = new Pattern[words.size()];
            for (int i = 0; i < pattern.length; i++) {
                pattern[i] = Pattern.compile(words.get(i), flags);
            }
        }

        //print(words);
        // We need match for all words:
        boolean[] matchFound = new boolean[words.size()];

        Object fieldContentAsObject;
        String fieldContent;
        
        for (String field : bibtexEntry.getAllFields()){
            fieldContentAsObject = bibtexEntry.getField(field);
            if (fieldContentAsObject != null) {
                fieldContent = removeBrackets.format(fieldContentAsObject.toString());
                if (!caseSensitive)
                    fieldContent = fieldContent.toLowerCase();
                int index = 0;
                // Check if we have a match for each of the query words, ignoring
                // those words for which we already have a match:
                for (int j=0; j<words.size(); j++) {
                    if (!regExp) {
                        String s = words.get(j);
                        matchFound[index] = matchFound[index]
                            || (fieldContent.indexOf(s) >= 0);
                    } else {
                        if (fieldContent != null) {
                            Matcher m = pattern[j].matcher
                                    (removeBrackets.format(fieldContent));
                            matchFound[index] = matchFound[index]
                                || m.find();
                        }
                    }

                    index++;
                }
            }

        }
        for (int i = 0; i < matchFound.length; i++) {
            if (!matchFound[i])
                return 0; // Didn't match all words.
        }
        return 1; // Matched all words.
    }

    private ArrayList<String> parseQuery(String query) {
        StringBuffer sb = new StringBuffer();
        ArrayList<String> result = new ArrayList<String>();
        int c;
        boolean escaped = false, quoted = false;
        for (int i=0; i<query.length(); i++) {
            c = query.charAt(i);
            // Check if we are entering an escape sequence:
            if (!escaped && (c == '\\'))
                escaped = true;
            else {
                // See if we have reached the end of a word:
                if (!escaped && !quoted && Character.isWhitespace((char)c)) {
                    if (sb.length() > 0) {
                        result.add(sb.toString());
                        sb = new StringBuffer();
                    }
                }
                else if (c == '"') {
                    // Whether it is a start or end quote, store the current
                    // word if any:
                    if (sb.length() > 0) {
                        result.add(sb.toString());
                        sb = new StringBuffer();
                    }
                    quoted = !quoted;
                }
                else {
                    // All other possibilities exhausted, we add the char to
                    // the current word:
                    sb.append((char)c);
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
