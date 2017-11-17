package org.jabref.gui.search.rules.describer;

import java.util.List;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.util.TextUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.search.rules.SentenceAnalyzer;

public class ContainsAndRegexBasedSearchRuleDescriber implements SearchDescriber {

    private final boolean regExp;
    private final boolean caseSensitive;
    private final String query;
    private final double textSize = 13;

    public ContainsAndRegexBasedSearchRuleDescriber(boolean caseSensitive, boolean regExp, String query) {
        this.caseSensitive = caseSensitive;
        this.regExp = regExp;
        this.query = query;
    }

    @Override
    public TextFlow getDescription() {
        List<String> words = new SentenceAnalyzer(query).getWords();
        String firstWord = words.isEmpty() ? "" : words.get(0);

        TextFlow searchDescription = new TextFlow();
        searchDescription.getChildren().add(TextUtil.createText(regExp ? Localization.lang(
                "This search contains entries in which any field contains the regular expression ")
                : Localization.lang("This search contains entries in which any field contains the term "), textSize));
        searchDescription.getChildren().add(TextUtil.createTextBold(firstWord, textSize));

        if (words.size() > 1) {
            List<String> unprocessedWords = words.subList(1, words.size());
            for (String word : unprocessedWords) {
                searchDescription.getChildren().add(TextUtil.createText(String.format(" %s ", Localization.lang("and")), textSize));
                searchDescription.getChildren().add(TextUtil.createTextBold(word, textSize));
            }
        }

        Text genericDescription = TextUtil.createText("\n\n" + Localization.lang("Hint: To search specific fields only, enter for example:\n"), textSize);
        Text genericDescription2 = TextUtil.createTextMonospaced("author=smith and title=electrical", textSize);
        searchDescription.getChildren().add(getCaseSensitiveDescription());
        searchDescription.getChildren().add(genericDescription);
        searchDescription.getChildren().add(genericDescription2);
        return searchDescription;
    }

    private Text getCaseSensitiveDescription() {
        if (caseSensitive) {
            return TextUtil.createText(String.format(" (%s). ", Localization.lang("case sensitive")), textSize);
        } else {
            return TextUtil.createText(String.format(" (%s). ", Localization.lang("case insensitive")), textSize);
        }
    }
}
