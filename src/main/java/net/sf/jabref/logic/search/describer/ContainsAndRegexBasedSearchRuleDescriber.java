/*  Copyright (C) 2003-2015 JabRef contributors.
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

        String searchDescription = regExp ? Localization.lang(
                "This group contains entries in which any field contains the regular expression <b>%0</b>",
                StringUtil.quoteForHTML(firstWord))
                : Localization.lang("This group contains entries in which any field contains the term <b>%0</b>",
                StringUtil.quoteForHTML(firstWord));

        if(words.size() > 1) {
            List<String> unprocessedWords = words.subList(1, words.size());
            List<String> unprocessedWordsInHtmlFormat = new LinkedList<>();
            for(String word : unprocessedWords) {
                unprocessedWordsInHtmlFormat.add(String.format("<b>%s</b>", StringUtil.quoteForHTML(word)));
            }
            String andSeparator = String.format(" %s ", Localization.lang("and"));
            String[] unprocessedWordsInHtmlFormatArray = unprocessedWordsInHtmlFormat.toArray(new String[unprocessedWordsInHtmlFormat.size()]);
            searchDescription += StringUtil.join(unprocessedWordsInHtmlFormatArray, andSeparator);
        }

        String caseSensitiveDescription = getCaseSensitiveDescription();
        String genericDescription = Localization.lang(
                "Entries cannot be manually assigned to or removed from this group.") + "<p><br>" + Localization.lang(
                "Hint%c To search specific fields only, enter for example%c<p><tt>author%esmith and title%eelectrical</tt>");
        return String.format("%s (%s). %s", searchDescription, caseSensitiveDescription, genericDescription);
    }

    private String getCaseSensitiveDescription() {
        return caseSensitive ? Localization.lang("case sensitive") : Localization.lang("case insensitive");
    }
}
