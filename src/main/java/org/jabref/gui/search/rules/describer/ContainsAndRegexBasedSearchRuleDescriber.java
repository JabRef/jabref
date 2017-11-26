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

        String temp = regExp ? Localization.lang(
                "This search contains entries in which any field contains the regular expression <b>%0</b>")
                : Localization.lang("This search contains entries in which any field contains the term <b>%0</b>");
        List<Text> textList = TextUtil.formatToTexts(temp, textSize, new TextUtil.TextReplacement("<b>%0</b>", firstWord, TextUtil.TextType.BOLD));

        if (words.size() > 1) {
            List<String> unprocessedWords = words.subList(1, words.size());
            for (String word : unprocessedWords) {
                textList.add(TextUtil.createText(String.format(" %s ", Localization.lang("and")), textSize, TextUtil.TextType.NORMAL));
                textList.add(TextUtil.createText(word, textSize, TextUtil.TextType.BOLD));
            }
        }

        String genericDescription = "\n\n" + Localization.lang("Hint: To search specific fields only, enter for example:<p><tt>author=smith and title=electrical</tt>");
        genericDescription = genericDescription.replace("<p>", "\n");
        List<Text> genericDescriptionTexts = TextUtil.formatToTexts(genericDescription, textSize, new TextUtil.TextReplacement("<tt>author=smith and title=electrical</tt>", "author=smith and title=electrical", TextUtil.TextType.MONOSPACED));
        textList.add(getCaseSensitiveDescription());
        textList.addAll(genericDescriptionTexts);

        TextFlow searchDescription = new TextFlow();
        searchDescription.getChildren().setAll(textList);
        return searchDescription;
    }

    private Text getCaseSensitiveDescription() {
        if (caseSensitive) {
            return TextUtil.createText(String.format(" (%s). ", Localization.lang("case sensitive")), textSize, TextUtil.TextType.NORMAL);
        } else {
            return TextUtil.createText(String.format(" (%s). ", Localization.lang("case insensitive")), textSize, TextUtil.TextType.NORMAL);
        }
    }
}
