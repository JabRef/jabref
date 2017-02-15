package net.sf.jabref.logic.search.rules.describer;

import java.util.LinkedList;
import java.util.List;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.search.rules.SentenceAnalyzer;
import net.sf.jabref.model.strings.StringUtil;

public class ContainsAndRegexBasedSearchRuleDescriber implements SearchDescriber {

    private final boolean regExp;
    private final boolean caseSensitive;
    private final String query;

    public ContainsAndRegexBasedSearchRuleDescriber(boolean caseSensitive, boolean regExp, String query) {
        this.caseSensitive = caseSensitive;
        this.regExp = regExp;
        this.query = query;
    }

    @Override
    public String getDescription() {
        List<String> words = new SentenceAnalyzer(query).getWords();
        String firstWord = words.isEmpty() ? "" : words.get(0);

        StringBuilder searchDescription = new StringBuilder(regExp ? Localization.lang(
                "This search contains entries in which any field contains the regular expression <b>%0</b>",
                StringUtil.quoteForHTML(firstWord))
                : Localization.lang("This search contains entries in which any field contains the term <b>%0</b>",
                        StringUtil.quoteForHTML(firstWord)));

        if(words.size() > 1) {
            List<String> unprocessedWords = words.subList(1, words.size());
            List<String> unprocessedWordsInHtmlFormat = new LinkedList<>();
            for(String word : unprocessedWords) {
                unprocessedWordsInHtmlFormat.add(String.format("<b>%s</b>", StringUtil.quoteForHTML(word)));
            }
            String andSeparator = String.format(" %s ", Localization.lang("and"));
            searchDescription.append(String.join(andSeparator, unprocessedWordsInHtmlFormat));
        }

        String caseSensitiveDescription = getCaseSensitiveDescription();
        String genericDescription = "<p><br>" + Localization.lang("Hint: To search specific fields only, enter for example:<p><tt>author=smith and title=electrical</tt>");
        return String.format("%s (%s). %s", searchDescription.toString(), caseSensitiveDescription, genericDescription);
    }

    private String getCaseSensitiveDescription() {
        if(caseSensitive) {
            return Localization.lang("case sensitive");
        } else {
            return Localization.lang("case insensitive");
        }
    }
}
