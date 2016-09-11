package net.sf.jabref.model.entry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import net.sf.jabref.model.strings.StringUtil;

/**
 * Represents a list of keyword chains.
 * For example, "Type > A, Type > B, Something else".
 */
public class KeywordList extends ArrayList<Keyword> {

    public KeywordList() {
    }

    public KeywordList(Collection<Keyword> keywords) {
        super(keywords);
    }

    public KeywordList(List<String> keywords) {
        this(keywords.stream().map(Keyword::new).collect(Collectors.toList()));
    }

    /**
     * @param keywordString a String of keywords
     * @return an parsed list containing the keywords
     */
    public static KeywordList parse(String keywordString, Character delimiter) {
        if (StringUtil.isBlank(keywordString)) {
            return new KeywordList();
        }

        List<String> keywords = new ArrayList<>();

        StringTokenizer tok = new StringTokenizer(keywordString, delimiter.toString());
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken().trim();
            keywords.add(word);
        }
        return new KeywordList(keywords);
    }

    public void replaceKeywords(KeywordList keywordsToReplace, Optional<Keyword> newValue) {
        // Remove keywords which should be replaced
        int foundPos = -1; // remember position of the last found keyword
        for (Keyword specialFieldKeyword : keywordsToReplace) {
            int pos = indexOf(specialFieldKeyword);
            if (pos >= 0) {
                foundPos = pos;
                remove(pos);
            }
        }

        // Add new keyword at right position
        int finalFoundPos = foundPos;
        newValue.ifPresent(value -> {
            if (finalFoundPos == -1) {
                add(value);
            } else {
                add(finalFoundPos, value);
            }
        });
    }

    @Override
    public boolean add(Keyword keyword) {
        if (contains(keyword)) {
            return false; // Don't add duplicate keywords
        }
        return super.add(keyword);
    }

    /**
     * Keywords are separated by the given delimiter and an additional space, i.e. "one, two".
     */
    public String getAsString(Character delimiter) {
        return this.stream().map(Keyword::toString).collect(Collectors.joining(delimiter + " "));
    }

    public void add(String keywordsString) {
        add(new Keyword(keywordsString));
    }
}
