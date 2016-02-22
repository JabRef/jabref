package net.sf.jabref.logic.search;

import java.util.Objects;
import java.util.Optional;
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
    public static String highlightWordsWithHTML(String text, Optional<Pattern> highlightPattern) {
        Objects.requireNonNull(highlightPattern);
        Objects.requireNonNull(text);

        if (text.isEmpty() || !highlightPattern.isPresent()) {
            return text;
        }

        Matcher matcher = highlightPattern.get().matcher(text);

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

}
