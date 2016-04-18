/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.formatter.bibtexfields;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.l10n.Localization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.logic.util.strings.HTMLUnicodeConversionMaps;

public class HtmlToLatexFormatter implements LayoutFormatter, Formatter {

    private static final Log LOGGER = LogFactory.getLog(HtmlToLatexFormatter.class);

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
        // If the result is in text or equation form can be controlled
        // From the "Advanced settings" tab
        if (Globals.prefs.getBoolean(JabRefPreferences.USE_CONVERT_TO_EQUATION)) {
            result = result.replaceAll("<[ ]?sup>([^<]+)</sup>", "\\$\\^\\{$1\\}\\$");
            result = result.replaceAll("<[ ]?sub>([^<]+)</sub>", "\\$_\\{$1\\}\\$");
        } else {
            result = result.replaceAll("<[ ]?sup>([^<]+)</sup>", "\\\\textsuperscript\\{$1\\}");
            result = result.replaceAll("<[ ]?sub>([^<]+)</sub>", "\\\\textsubscript\\{$1\\}");
        }

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
            result = result.replaceAll(pattern, HTMLUnicodeConversionMaps.HTML_LATEX_CONVERSION_MAP.get(pattern));
        }

        // Handle numerical HTML entities
        Matcher m = ESCAPED_PATTERN.matcher(result);
        while (m.find()) {
            int num = Integer.decode(m.group(1).replace("x", "#") + m.group(3));
            if (HTMLUnicodeConversionMaps.NUMERICAL_LATEX_CONVERSION_MAP.containsKey(num)) {
                result = result.replaceAll("&#" + m.group(1) + m.group(2) + m.group(3) + ";",
                        HTMLUnicodeConversionMaps.NUMERICAL_LATEX_CONVERSION_MAP.get(num));
            }
        }

        // Combining accents
        m = ESCAPED_PATTERN2.matcher(result);
        while (m.find()) {
            int num = Integer.decode(m.group(2).replace("x", "#") + m.group(4));
            if (HTMLUnicodeConversionMaps.ESCAPED_ACCENTS.containsKey(num)) {
                if ("i".equals(m.group(1))) {
                    result = result.replaceAll(m.group(1) + "&#" + m.group(2) + m.group(3) + m.group(4) + ";",
                            "\\{\\\\" + HTMLUnicodeConversionMaps.ESCAPED_ACCENTS.get(num) + "\\{\\\\i\\}\\}");
                } else if ("j".equals(m.group(1))) {
                    result = result.replaceAll(m.group(1) + "&#" + m.group(2) + m.group(3) + m.group(4) + ";",
                            "\\{\\\\" + HTMLUnicodeConversionMaps.ESCAPED_ACCENTS.get(num) + "\\{\\\\j\\}\\}");
                } else {
                    result = result.replaceAll(m.group(1) + "&#" + m.group(2) + m.group(3) + m.group(4) + ";", "\\{\\\\"
                            + HTMLUnicodeConversionMaps.ESCAPED_ACCENTS.get(num) + "\\{" + m.group(1) + "\\}\\}");
                }
            }
        }

        // Find non-converted numerical characters
        m = ESCAPED_PATTERN3.matcher(result);
        while (m.find()) {
            int num = Integer.decode(m.group(1).replace("x", "#") + m.group(3));
            LOGGER.warn("HTML escaped char not converted: " + m.group(1) + m.group(2) + m.group(3) + " = " + Integer.toString(num));
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
