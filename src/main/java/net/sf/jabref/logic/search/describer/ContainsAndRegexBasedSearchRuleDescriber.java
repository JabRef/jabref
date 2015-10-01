package net.sf.jabref.logic.search.describer;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.rules.util.SentenceAnalyzer;
import net.sf.jabref.logic.util.strings.StringUtil;

import java.util.LinkedList;
import java.util.List;

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
        String firstWord = !words.isEmpty() ? words.get(0) : "";

        System.out.println("words = " + words);

        String searchDescription = regExp ? Localization.lang(
                "This group contains entries in which any field contains the regular expression <b>%0</b>",
                StringUtil.quoteForHTML(firstWord))
                : Localization.lang("This group contains entries in which any field contains the term <b>%0</b>",
                StringUtil.quoteForHTML(firstWord));

        if(words.size() > 1) {
            List<String> unprocessedWords = words.subList(1, words.size());
            List<String> unprocessedWordsInHtmlFormat = new LinkedList<String>();
            for(String word : unprocessedWords) {
                unprocessedWordsInHtmlFormat.add(String.format("<b>%s</b>", StringUtil.quoteForHTML(word)));
            }
            String andSeparator = String.format(" %s ", Localization.lang("and"));
            String[] unprocessedWordsInHtmlFormatArray = unprocessedWordsInHtmlFormat.toArray(new String[unprocessedWordsInHtmlFormat.size()]);
            searchDescription += StringUtil.join(unprocessedWordsInHtmlFormatArray, andSeparator);
        }

        String caseSensitiveDescription = caseSensitive ? Localization.lang("case sensitive") : Localization.lang("case insensitive");
        String genericDescription = Localization.lang(
                "Entries cannot be manually assigned to or removed from this group.") + "<p><br>" + Localization.lang(
                "Hint%c To search specific fields only, enter for example%c<p><tt>author%esmith and title%eelectrical</tt>");
        return String.format("%s (%s). %s", searchDescription, caseSensitiveDescription, genericDescription);
    }
}
