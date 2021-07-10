package org.jabref.gui.search.rules.describer;

import java.util.EnumSet;
import java.util.List;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.search.rules.SearchRules.SearchFlags;
import org.jabref.model.search.rules.SentenceAnalyzer;

public class ContainsAndRegexBasedSearchRuleDescriber implements SearchDescriber {

    private final EnumSet<SearchFlags> searchFlags;
    private final String query;

    public ContainsAndRegexBasedSearchRuleDescriber(EnumSet<SearchFlags> searchFlags, String query) {
        this.searchFlags = searchFlags;
        this.query = query;
    }

    @Override
    public TextFlow getDescription() {
        List<String> words = new SentenceAnalyzer(query).getWords();
        String firstWord = words.isEmpty() ? "" : words.get(0);

        String temp = searchFlags.contains(SearchRules.SearchFlags.REGULAR_EXPRESSION) ? Localization.lang(
                "This search contains entries in which any field contains the regular expression <b>%0</b>")
                : Localization.lang("This search contains entries in which any field contains the term <b>%0</b>");
        List<Text> textList = TooltipTextUtil.formatToTexts(temp, new TooltipTextUtil.TextReplacement("<b>%0</b>", firstWord, TooltipTextUtil.TextType.BOLD));

        if (words.size() > 1) {
            List<String> unprocessedWords = words.subList(1, words.size());
            for (String word : unprocessedWords) {
                textList.add(TooltipTextUtil.createText(String.format(" %s ", Localization.lang("and")), TooltipTextUtil.TextType.NORMAL));
                textList.add(TooltipTextUtil.createText(word, TooltipTextUtil.TextType.BOLD));
            }
        }

        textList.add(getCaseSensitiveDescription());

        TextFlow searchDescription = new TextFlow();
        searchDescription.getChildren().setAll(textList);
        return searchDescription;
    }

    private Text getCaseSensitiveDescription() {
        if (searchFlags.contains(SearchRules.SearchFlags.CASE_SENSITIVE)) {
            return TooltipTextUtil.createText(String.format(" (%s). ", Localization.lang("case sensitive")), TooltipTextUtil.TextType.NORMAL);
        } else {
            return TooltipTextUtil.createText(String.format(" (%s). ", Localization.lang("case insensitive")), TooltipTextUtil.TextType.NORMAL);
        }
    }
}
