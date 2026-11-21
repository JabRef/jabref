package org.jabref.logic.layout.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.layout.LayoutFormatter;

import org.jspecify.annotations.NullMarked;

/// Turns a field value into literal inline Markdown text.
///
/// Whitespace runs (including line breaks) are collapsed into a single space and trailing whitespace is removed,
/// so that the value cannot break a list item. Characters that start Markdown markup
/// (emphasis, links, inline HTML, code spans, tables, headings), list, quote and thematic break markers at the start,
/// and `&` starting an HTML entity are escaped with a backslash.
@NullMarked
public class MarkdownEscaper implements LayoutFormatter {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern TRAILING_WHITESPACE = Pattern.compile("\\s+$");
    private static final Pattern MARKUP = Pattern.compile("[*_\\[\\]\\\\<`|#]");
    private static final Pattern ENTITY_START = Pattern.compile("&(?=#?\\w+;)");
    private static final Pattern LEADING_BLOCK_MARKER = Pattern.compile("^(\\s*)([-+>])");
    private static final Pattern LEADING_ORDERED_LIST_MARKER = Pattern.compile("^(\\s*\\d+)([.)])");

    @Override
    public String format(String fieldText) {
        String singleLine = WHITESPACE.matcher(fieldText).replaceAll(" ");
        String trimmed = TRAILING_WHITESPACE.matcher(singleLine).replaceAll("");
        String escaped = MARKUP.matcher(trimmed).replaceAll(match -> Matcher.quoteReplacement("\\" + match.group()));
        escaped = ENTITY_START.matcher(escaped).replaceAll("\\\\&");
        // A list, quote, or thematic break marker only takes effect at the start of a line
        escaped = LEADING_BLOCK_MARKER.matcher(escaped).replaceFirst("$1\\\\$2");
        return LEADING_ORDERED_LIST_MARKER.matcher(escaped).replaceFirst("$1\\\\$2");
    }
}
