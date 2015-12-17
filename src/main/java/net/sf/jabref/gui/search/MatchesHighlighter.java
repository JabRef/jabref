package net.sf.jabref.gui.search;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchesHighlighter {

    // used at highlighting in preview area.
    // Color chosen similar to JTextComponent.getSelectionColor(), which is
    // used at highlighting words at the editor
    public static final String HIGHLIGHT_COLOR = "#3399FF";

    /**
     * Will return the text that was called by the method with HTML tags to highlight each word the user has searched
     * for and will skip the highlight process if the first Char isn't a letter or a digit.
     * <p>
     * This check is a quick hack to avoid highlighting of HTML tags It does not always work, but it does its job mostly
     *
     * @param text             This is a String in which we search for different words
     * @param wordsToHighlight List of all words which must be highlighted
     * @return String that was called by the method, with HTML Tags if a word was found
     */
    public static String highlightWordsWithHTML(String text, List<String> wordsToHighlight) {
        Objects.requireNonNull(wordsToHighlight);
        Objects.requireNonNull(text);

        if (text.isEmpty() || wordsToHighlight.isEmpty()) {
            return text;
        }

        Optional<Pattern> patternForWords = getPatternForWords(wordsToHighlight, Globals.prefs.getBoolean(JabRefPreferences.SEARCH_REG_EXP),
                Globals.prefs.getBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE));
        if (!patternForWords.isPresent()) {
            return text;
        }

        Matcher matcher = patternForWords.get().matcher(text);

        StringBuffer sb = new StringBuffer();
        boolean foundSomething = false;

        while (matcher.find()) {
            String found = matcher.group();
            // color the search keyword
            // put first String Part and then html + word + html to a StringBuffer
            matcher.appendReplacement(sb, "<span style=\"background-color:" + HIGHLIGHT_COLOR + ";\">" + found + "</span>");
            foundSomething = true;
        }

        if (foundSomething) {
            matcher.appendTail(sb);
            text = sb.toString();
        }

        return text;
    }

    // Returns a regular expression pattern in the form (w1)|(w2)| ... wi are escaped if no regular expression search is enabled
    public static Optional<Pattern> getPatternForWords(List<String> words, boolean useRegex, boolean isCaseSensitive) {
        if ((words == null) || words.isEmpty() || words.get(0).isEmpty()) {
            return Optional.empty();
        }

        // compile the words to a regular expression in the form (w1)|(w2)|(w3)
        StringJoiner joiner = new StringJoiner(")|(", "(", ")");
        for (String word : words) {
            joiner.add(useRegex ? word : Pattern.quote(word));
        }
        String searchPattern = joiner.toString();

        if (isCaseSensitive) {
            return Optional.of(Pattern.compile(searchPattern));
        } else {
            return Optional.of(Pattern.compile(searchPattern, Pattern.CASE_INSENSITIVE));
        }
    }
}
