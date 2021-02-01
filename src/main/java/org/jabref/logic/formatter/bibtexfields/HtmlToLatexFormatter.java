package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.util.strings.HTMLUnicodeConversionMaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlToLatexFormatter extends Formatter implements LayoutFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlToLatexFormatter.class);

    private static final int MAX_TAG_LENGTH = 100;

    private static final Pattern ESCAPED_PATTERN = Pattern.compile("&#([x]*)([0]*)(\\p{XDigit}+);");
    private static final Pattern ESCAPED_PATTERN2 = Pattern.compile("(.)&#([x]*)([0]*)(\\p{XDigit}+);");
    private static final Pattern ESCAPED_PATTERN3 = Pattern.compile("&#([x]*)([0]*)(\\p{XDigit}+);");
    private static final Pattern ESCAPED_PATTERN4 = Pattern.compile("&(\\w+);");

    @Override
    public String format(String text) {
        String result = Objects.requireNonNull(text);

        if (result.isEmpty()) {
            return result;
        }

        StringBuilder sb = new StringBuilder();
        // Deal with the form <sup>k</sup>and <sub>k</sub>
        result = result.replaceAll("<[ ]?sup>([^<]+)</sup>", "\\\\textsuperscript\\{$1\\}");
        result = result.replaceAll("<[ ]?sub>([^<]+)</sub>", "\\\\textsubscript\\{$1\\}");

        // TODO: maybe rewrite this based on regular expressions instead
        // Note that (at least) the IEEE Xplore fetcher must be fixed as it relies on the current way to
        // remove tags for its image alt-tag to equation converter
        for (int i = 0; i < result.length(); i++) {

            int c = result.charAt(i);

            if (c == '<') {
                i = readTag(result, i);
            } else {
                sb.append((char) c);
            }
        }
        result = sb.toString();

        // Handle text based HTML entities
        Set<String> patterns = HTMLUnicodeConversionMaps.HTML_LATEX_CONVERSION_MAP.keySet();
        for (String pattern : patterns) {
            result = result.replace(pattern, HTMLUnicodeConversionMaps.HTML_LATEX_CONVERSION_MAP.get(pattern));
        }

        // Handle numerical HTML entities
        Matcher m = ESCAPED_PATTERN.matcher(result);
        while (m.find()) {
            int num = Integer.decode(m.group(1).replace("x", "#") + m.group(3));
            if (HTMLUnicodeConversionMaps.NUMERICAL_LATEX_CONVERSION_MAP.containsKey(num)) {
                result = result.replace("&#" + m.group(1) + m.group(2) + m.group(3) + ";",
                        HTMLUnicodeConversionMaps.NUMERICAL_LATEX_CONVERSION_MAP.get(num));
            }
        }

        // Combining accents
        m = ESCAPED_PATTERN2.matcher(result);
        while (m.find()) {
            int num = Integer.decode(m.group(2).replace("x", "#") + m.group(4));
            if (HTMLUnicodeConversionMaps.ESCAPED_ACCENTS.containsKey(num)) {
                if ("i".equals(m.group(1))) {
                    result = result.replace(m.group(1) + "&#" + m.group(2) + m.group(3) + m.group(4) + ";",
                            "{\\" + HTMLUnicodeConversionMaps.ESCAPED_ACCENTS.get(num) + "{\\i}}");
                } else if ("j".equals(m.group(1))) {
                    result = result.replace(m.group(1) + "&#" + m.group(2) + m.group(3) + m.group(4) + ";",
                            "{\\" + HTMLUnicodeConversionMaps.ESCAPED_ACCENTS.get(num) + "{\\j}}");
                } else {
                    result = result.replace(m.group(1) + "&#" + m.group(2) + m.group(3) + m.group(4) + ";",
                            "{\\" + HTMLUnicodeConversionMaps.ESCAPED_ACCENTS.get(num) + "{" + m.group(1) + "}}");
                }
            }
        }

        // Find non-converted numerical characters
        m = ESCAPED_PATTERN3.matcher(result);
        while (m.find()) {
            int num = Integer.decode(m.group(1).replace("x", "#") + m.group(3));
            LOGGER.warn("HTML escaped char not converted: {}{}{} = {}", m.group(1), m.group(2), m.group(3), " = ", num);
        }

        // Remove $$ in case of two adjacent conversions
        result = result.replace("$$", "");

        // Find non-covered special characters with alphabetic codes
        m = ESCAPED_PATTERN4.matcher(result);
        while (m.find()) {
            LOGGER.warn("HTML escaped char not converted: " + m.group(1));
        }

        return result.trim();
    }

    @Override
    public String getDescription() {
        return Localization.lang("Converts HTML code to LaTeX code.");
    }

    @Override
    public String getExampleInput() {
        return "<strong>JabRef</strong>";
    }

    private int readTag(String text, int position) {
        // Have just read the < character that starts the tag.
        int index = text.indexOf('>', position);
        if ((index > position) && ((index - position) < MAX_TAG_LENGTH)) {
            return index; // Just skip the tag.
        } else {
            return position; // Don't do anything.
        }
    }

    @Override
    public String getName() {
        return Localization.lang("HTML to LaTeX");
    }

    @Override
    public String getKey() {
        return "html_to_latex";
    }
}
