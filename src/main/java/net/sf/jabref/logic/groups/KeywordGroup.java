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
package net.sf.jabref.logic.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.fileformat.ParseException;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.QuotedStringTokenizer;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author jzieren
 */
public class KeywordGroup extends AbstractGroup {

    public static final String ID = "KeywordGroup:";

    private final String searchField;
    private final String searchExpression;
    private final boolean caseSensitive;
    private final boolean regExp;
    private Pattern pattern;

    private static final Log LOGGER = LogFactory.getLog(KeywordGroup.class);


    /**
     * Creates a KeywordGroup with the specified properties.
     */
    public KeywordGroup(String name, String searchField,
                        String searchExpression, boolean caseSensitive, boolean regExp,
                        GroupHierarchyType context) throws ParseException {
        super(name, context);
        this.searchField = searchField;
        this.searchExpression = searchExpression;
        this.caseSensitive = caseSensitive;
        this.regExp = regExp;
        if (this.regExp) {
            compilePattern();
        }
    }

    private void compilePattern() throws ParseException {
        try {
            pattern = caseSensitive ? Pattern.compile("\\b" + searchExpression + "\\b") : Pattern.compile(
                    "\\b" + searchExpression + "\\b", Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException exception) {
            throw new ParseException(Localization.lang("Syntax error in regular-expression pattern", searchExpression));
        }
    }

    /**
     * Parses s and recreates the KeywordGroup from it.
     *
     * @param s The String representation obtained from
     *          KeywordGroup.toString()
     */
    public static AbstractGroup fromString(String s) throws ParseException {
        if (!s.startsWith(KeywordGroup.ID)) {
            throw new IllegalArgumentException("KeywordGroup cannot be created from \"" + s + "\".");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(KeywordGroup.ID
                .length()), AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR);

        String name = tok.nextToken();
        int context = Integer.parseInt(tok.nextToken());
        String field = tok.nextToken();
        String expression = tok.nextToken();
        boolean caseSensitive = Integer.parseInt(tok.nextToken()) == 1;
        boolean regExp = Integer.parseInt(tok.nextToken()) == 1;
        return new KeywordGroup(StringUtil.unquote(name, AbstractGroup.QUOTE_CHAR),
                StringUtil.unquote(field, AbstractGroup.QUOTE_CHAR),
                StringUtil.unquote(expression, AbstractGroup.QUOTE_CHAR), caseSensitive, regExp,
                GroupHierarchyType.getByNumber(context));
    }

    /**
     * Returns a String representation of this object that can be used to
     * reconstruct it.
     */
    @Override
    public String toString() {
        return KeywordGroup.ID + StringUtil.quote(getName(), AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR) +
                AbstractGroup.SEPARATOR
                + getContext().ordinal() + AbstractGroup.SEPARATOR
                + StringUtil.quote(searchField, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR) + AbstractGroup.SEPARATOR
                + StringUtil.quote(searchExpression, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR)
                + AbstractGroup.SEPARATOR + StringUtil.booleanToBinaryString(caseSensitive) + AbstractGroup.SEPARATOR
                + StringUtil.booleanToBinaryString(regExp) + AbstractGroup.SEPARATOR;
    }

    @Override
    public boolean supportsAdd() {
        return !regExp;
    }

    @Override
    public boolean supportsRemove() {
        return !regExp;
    }

    @Override
    public Optional<EntriesGroupChange> add(List<BibEntry> entriesToAdd) {
        if (!supportsAdd()) {
            return Optional.empty();
        }
        if ((entriesToAdd != null) && !(entriesToAdd.isEmpty())) {
            List<FieldChange> changes = new ArrayList<>();
            boolean modified = false;
            for (BibEntry entry : entriesToAdd) {
                if (!contains(entry)) {
                    String oldContent = entry
                            .getField(searchField);
                    String pre = Globals.prefs.get(JabRefPreferences.GROUP_KEYWORD_SEPARATOR);
                    String newContent = (oldContent == null ? "" : oldContent
                            + pre)
                            + searchExpression;
                    entry.setField(searchField, newContent);

                    // Store change information.
                    changes.add(new FieldChange(entry, searchField, oldContent, newContent));
                    modified = true;
                }
            }

            return modified ? Optional.of(new EntriesGroupChange(changes)) : Optional.empty();
        }

        return Optional.empty();
    }

    @Override
    public Optional<EntriesGroupChange> remove(List<BibEntry> entriesToRemove) {
        if (!supportsRemove()) {
            return Optional.empty();
        }

        if ((entriesToRemove != null) && (entriesToRemove.size() > 0)) {
            List<FieldChange> changes = new ArrayList<>();
            boolean modified = false;
            for (BibEntry entry : entriesToRemove) {
                if (contains(entry)) {
                    String oldContent = entry
                            .getField(searchField);
                    removeMatches(entry);

                    // Store change information.
                    changes.add(new FieldChange(entry, searchField, oldContent, entry.getField(searchField)));
                    modified = true;
                }
            }

            return modified ? Optional.of(new EntriesGroupChange(changes)) : Optional.empty();
        }

        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof KeywordGroup)) {
            return false;
        }
        KeywordGroup other = (KeywordGroup) o;
        return getName().equals(other.getName())
                && searchField.equals(other.searchField)
                && searchExpression.equals(other.searchExpression)
                && (caseSensitive == other.caseSensitive)
                && (regExp == other.regExp)
                && (getHierarchicalContext() == other.getHierarchicalContext());
    }

    @Override
    public boolean contains(BibEntry entry) {
        if (!entry.hasField(searchField)) {
            return false;
        }
        String content = entry.getField(searchField);
        if (regExp) {
            return pattern.matcher(content).find();
        }
        if (caseSensitive) {
            return KeywordGroup.containsWord(searchExpression, content);
        }
        return KeywordGroup.containsWord(searchExpression.toLowerCase(), content.toLowerCase());
    }

    /**
     * Look for the given non-regexp string in another string, but check whether a
     * match concerns a complete word, not part of a word.
     *
     * @param word The word to look for.
     * @param text The string to look in.
     * @return true if the word was found, false otherwise.
     */
    private static boolean containsWord(String word, String text) {
        int piv = 0;
        while (piv < text.length()) {
            int index = text.indexOf(word, piv);
            if (index < 0) {
                return false;
            }
            // Found a match. See if it is a complete word:
            if (((index == 0) || !Character.isLetterOrDigit(text.charAt(index - 1))) &&
                    (((index + word.length()) == text.length())
                            || !Character.isLetterOrDigit(text.charAt(index + word.length())))) {
                return true;
            } else {
                piv = index + 1;
            }
        }
        return false;
    }

    /**
     * Removes matches of searchString in the entry's field. This is only
     * possible if the search expression is not a regExp.
     */
    private void removeMatches(BibEntry entry) {
        if (!entry.hasField(searchField)) {
            return; // nothing to modify
        }
        String content = entry.getField(searchField);
        StringBuffer sbOrig = new StringBuffer(content);
        StringBuffer sbLower = new StringBuffer(content.toLowerCase());
        StringBuffer haystack = caseSensitive ? sbOrig : sbLower;
        String needle = caseSensitive ? searchExpression
                : searchExpression.toLowerCase();
        int i;
        int j;
        int k;
        final String separator = Globals.prefs.get(JabRefPreferences.GROUP_KEYWORD_SEPARATOR);
        while ((i = haystack.indexOf(needle)) >= 0) {
            sbOrig.replace(i, i + needle.length(), "");
            sbLower.replace(i, i + needle.length(), "");
            // reduce spaces at i to 1
            j = i;
            k = i;
            while (((j - 1) >= 0) && (separator.indexOf(haystack.charAt(j - 1)) >= 0)) {
                --j;
            }
            while ((k < haystack.length()) && (separator.indexOf(haystack.charAt(k)) >= 0)) {
                ++k;
            }
            sbOrig.replace(j, k, (j >= 0) && (k < sbOrig.length()) ? separator : "");
            sbLower.replace(j, k, (j >= 0) && (k < sbOrig.length()) ? separator : "");
        }

        String result = sbOrig.toString().trim();
        if (result.isEmpty()) {
            entry.clearField(searchField);
        } else {
            entry.setField(searchField, result);
        }
    }

    @Override
    public AbstractGroup deepCopy() {
        try {
            return new KeywordGroup(getName(), searchField, searchExpression,
                    caseSensitive, regExp, getContext());
        } catch (ParseException exception) {
            // this should never happen, because the constructor obviously succeeded in creating _this_ instance!
            LOGGER.error("Internal error in KeywordGroup.deepCopy(). "
                    + "Please report this on https://github.com/JabRef/jabref/issues", exception);
            return null;
        }
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isRegExp() {
        return regExp;
    }

    public String getSearchExpression() {
        return searchExpression;
    }

    public String getSearchField() {
        return searchField;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public String getDescription() {
        return KeywordGroup.getDescriptionForPreview(searchField, searchExpression, caseSensitive, regExp);
    }

    public static String getDescriptionForPreview(String field, String expr, boolean caseSensitive, boolean regExp) {
        String header = regExp ? Localization.lang("This group contains entries whose <b>%0</b> field contains the regular expression <b>%1</b>",
                field, StringUtil.quoteForHTML(expr))
                : Localization.lang("This group contains entries whose <b>%0</b> field contains the keyword <b>%1</b>",
                field, StringUtil.quoteForHTML(expr));
        String caseSensitiveText = caseSensitive ? Localization.lang("case sensitive") :
            Localization.lang("case insensitive");
        String footer = regExp ?
                Localization.lang("Entries cannot be manually assigned to or removed from this group.")
                : Localization.lang(
                "Additionally, entries whose <b>%0</b> field does not contain "
                        + "<b>%1</b> can be assigned manually to this group by selecting them "
                        + "then using either drag and drop or the context menu. "
                        + "This process adds the term <b>%1</b> to "
                        + "each entry's <b>%0</b> field. "
                        + "Entries can be removed manually from this group by selecting them "
                        + "then using the context menu. "
                        + "This process removes the term <b>%1</b> from "
                        + "each entry's <b>%0</b> field.",
                field, StringUtil.quoteForHTML(expr));
        return String.format("%s (%s). %s", header, caseSensitiveText, footer);
    }

    @Override
    public String getShortDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>");
        if (Globals.prefs.getBoolean(JabRefPreferences.GROUP_SHOW_DYNAMIC)) {
            sb.append("<i>").append(StringUtil.quoteForHTML(getName())).append("</i>");
        } else {
            sb.append(StringUtil.quoteForHTML(getName()));
        }
        sb.append("</b> - ");
        sb.append(Localization.lang("dynamic group"));
        sb.append("<b>");
        sb.append(searchField);
        sb.append("</b>");
        sb.append(Localization.lang("contains"));
        sb.append(" <b>");
        sb.append(StringUtil.quoteForHTML(searchExpression));
        sb.append("</b>)");
        switch (getHierarchicalContext()) {
            case INCLUDING:
                sb.append(", ").append(Localization.lang("includes subgroups"));
                break;
            case REFINING:
                sb.append(", ").append(Localization.lang("refines supergroup"));
                break;
            default:
                break;
        }
        return sb.toString();
    }

    @Override
    public String getTypeId() {
        return KeywordGroup.ID;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }
}
