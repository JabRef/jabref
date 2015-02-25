/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.groups;

import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;
import net.sf.jabref.util.QuotedStringTokenizer;

/**
 * @author jzieren
 */
public class KeywordGroup extends AbstractGroup implements SearchRule {
	public static final String ID = "KeywordGroup:";
	private final String m_searchField;
	private final String m_searchExpression;
	private final boolean m_caseSensitive;
	private final boolean m_regExp;
	private Pattern m_pattern = null;

	/**
	 * Creates a KeywordGroup with the specified properties.
	 */
	public KeywordGroup(String name, String searchField,
			String searchExpression, boolean caseSensitive, boolean regExp,
			int context) throws IllegalArgumentException {
		super(name, context);
		m_searchField = searchField;
		m_searchExpression = searchExpression;
		m_caseSensitive = caseSensitive;
		m_regExp = regExp;
		if (m_regExp)
			compilePattern();
	}

	protected void compilePattern() throws IllegalArgumentException {
		m_pattern = m_caseSensitive ? Pattern.compile("\\b"+m_searchExpression+"\\b")
				: Pattern.compile("\\b"+m_searchExpression+"\\b", Pattern.CASE_INSENSITIVE);
	}

	/**
	 * Parses s and recreates the KeywordGroup from it.
	 * 
	 * @param s
	 *            The String representation obtained from
	 *            KeywordGroup.toString()
	 */
	public static AbstractGroup fromString(String s, BibtexDatabase db,
			int version) throws Exception {
        if (!s.startsWith(ID))
			throw new Exception(
					"Internal error: KeywordGroup cannot be created from \""
							+ s
							+ "\". "
							+ "Please report this on www.sf.net/projects/jabref");
		QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(ID
				.length()), SEPARATOR, QUOTE_CHAR);
		switch (version) {
		case 0: {
			String name = tok.nextToken();
			String field = tok.nextToken();
			String expression = tok.nextToken();
			// assume caseSensitive=false and regExp=true for old groups
			return new KeywordGroup(Util.unquote(name, QUOTE_CHAR), Util
					.unquote(field, QUOTE_CHAR), Util.unquote(expression,
					QUOTE_CHAR), false, true, AbstractGroup.INDEPENDENT);
		}
		case 1:
		case 2: {
			String name = tok.nextToken();
			String field = tok.nextToken();
			String expression = tok.nextToken();
			boolean caseSensitive = Integer.parseInt(tok.nextToken()) == 1;
			boolean regExp = Integer.parseInt(tok.nextToken()) == 1;
			return new KeywordGroup(Util.unquote(name, QUOTE_CHAR), Util
					.unquote(field, QUOTE_CHAR), Util.unquote(expression,
					QUOTE_CHAR), caseSensitive, regExp,
					AbstractGroup.INDEPENDENT);
		}
		case 3: {
            String name = tok.nextToken();
            int context = Integer.parseInt(tok.nextToken());
            String field = tok.nextToken();
            String expression = tok.nextToken();
            boolean caseSensitive = Integer.parseInt(tok.nextToken()) == 1;
            boolean regExp = Integer.parseInt(tok.nextToken()) == 1;
            return new KeywordGroup(Util.unquote(name, QUOTE_CHAR), Util
                    .unquote(field, QUOTE_CHAR), Util.unquote(expression,
                    QUOTE_CHAR), caseSensitive, regExp, context);
		}
		default:
			throw new UnsupportedVersionException("KeywordGroup", version);
		}
	}

	/**
	 * @see net.sf.jabref.groups.AbstractGroup#getSearchRule()
	 */
	public SearchRule getSearchRule() {
		return this;
	}

	/**
	 * Returns a String representation of this object that can be used to
	 * reconstruct it.
	 */
	public String toString() {
		return ID + Util.quote(m_name, SEPARATOR, QUOTE_CHAR) + SEPARATOR
				+ m_context + SEPARATOR
				+ Util.quote(m_searchField, SEPARATOR, QUOTE_CHAR) + SEPARATOR
				+ Util.quote(m_searchExpression, SEPARATOR, QUOTE_CHAR)
				+ SEPARATOR + (m_caseSensitive ? "1" : "0") + SEPARATOR
				+ (m_regExp ? "1" : "0") + SEPARATOR;
	}

	public boolean supportsAdd() {
		return !m_regExp;
	}

	public boolean supportsRemove() {
		return !m_regExp;
	}

	public AbstractUndoableEdit add(BibtexEntry[] entries) {
		if (!supportsAdd())
			return null;
		if ((entries != null) && (entries.length > 0)) {
			NamedCompound ce = new NamedCompound(
                                        Globals.lang("add entries to group"));
			boolean modified = false;
            for (BibtexEntry entry : entries) {
                if (applyRule(null, entry) == 0) {
                    String oldContent = entry
                            .getField(m_searchField),
                            pre = Globals.prefs.get("groupKeywordSeparator");
                    String newContent = (oldContent == null ? "" : oldContent
                            + pre)
                            + m_searchExpression;
                    entry.setField(m_searchField, newContent);

                    // Store undo information.
                    ce.addEdit(new UndoableFieldChange(entry,
                            m_searchField, oldContent, newContent));
                    modified = true;
                }
            }
			if (modified)
				ce.end();

			return modified ? ce : null;
		}

		return null;
	}

	public AbstractUndoableEdit remove(BibtexEntry[] entries) {
		if (!supportsRemove())
			return null;

		if ((entries != null) && (entries.length > 0)) {
			NamedCompound ce = new NamedCompound(Globals.lang("remove from group"));
			boolean modified = false;
            for (BibtexEntry entry : entries) {
                if (applyRule(null, entry) > 0) {
                    String oldContent = entry
                            .getField(m_searchField);
                    removeMatches(entry);
                    // Store undo information.
                    ce.addEdit(new UndoableFieldChange(entry,
                            m_searchField, oldContent, entry
                            .getField(m_searchField)
                    ));
                    modified = true;
                }
            }
			if (modified)
				ce.end();

			return modified ? ce : null;
		}

		return null;
	}

	public boolean equals(Object o) {
		if (!(o instanceof KeywordGroup))
			return false;
		KeywordGroup other = (KeywordGroup) o;
		return m_name.equals(other.m_name)
				&& m_searchField.equals(other.m_searchField)
				&& m_searchExpression.equals(other.m_searchExpression)
				&& m_caseSensitive == other.m_caseSensitive
				&& m_regExp == other.m_regExp
                && getHierarchicalContext() == other.getHierarchicalContext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.jabref.groups.AbstractGroup#contains(java.util.Map,
	 *      net.sf.jabref.BibtexEntry)
	 */
	public boolean contains(Map<String, String> searchOptions, BibtexEntry entry) {
		return contains(entry);
	}

	public boolean contains(BibtexEntry entry) {
		String content = entry.getField(m_searchField);
		if (content == null)
			return false;
		if (m_regExp)
			return m_pattern.matcher(content).find();
		if (m_caseSensitive)
			return containsWord(m_searchExpression, content);
		return containsWord(m_searchExpression.toLowerCase(), content.toLowerCase());
	}

    /**
     * Look for the given non-regexp string in another string, but check whether a
     * match concerns a complete word, not part of a word.
     * @param word The word to look for.
     * @param text The string to look in.
     * @return true if the word was found, false otherwise.
     */
    private static boolean containsWord(String word, String text) {
        int piv = 0;
        while (piv < text.length()) {
            int ind = text.indexOf(word, piv);
            if (ind < 0)
                return false;
            // Found a match. See if it is a complete word:
            if (((ind == 0) || !Character.isLetterOrDigit(text.charAt(ind-1))) &&
                ((ind+word.length() == text.length()) || !Character.isLetterOrDigit(text.charAt(ind+word.length())))) {
                return true;
            }
            else piv = ind+1;
        }
        return false;
    }

	/**
	 * Removes matches of searchString in the entry's field. This is only
	 * possible if the search expression is not a regExp.
	 */
	private void removeMatches(BibtexEntry entry) {
		String content = entry.getField(m_searchField);
		if (content == null)
			return; // nothing to modify
		StringBuffer sbOrig = new StringBuffer(content);
		StringBuffer sbLower = new StringBuffer(content.toLowerCase());
		StringBuffer haystack = m_caseSensitive ? sbOrig : sbLower;
		String needle = m_caseSensitive ? m_searchExpression
				: m_searchExpression.toLowerCase();
		int i, j, k;
		final String separator = Globals.prefs.get("groupKeywordSeparator");
		while ((i = haystack.indexOf(needle)) >= 0) {
			sbOrig.replace(i, i + needle.length(), "");
			sbLower.replace(i, i + needle.length(), "");
			// reduce spaces at i to 1
			j = i;
			k = i;
			while (j - 1 >= 0 && separator.indexOf(haystack.charAt(j - 1)) >= 0)
				--j;
			while (k < haystack.length() && separator.indexOf(haystack.charAt(k)) >= 0)
				++k;
			sbOrig.replace(j, k, j >= 0 && k < sbOrig.length() ? separator : "");
			sbLower.replace(j, k, j >= 0 && k < sbOrig.length() ? separator : "");
		}

		String result = sbOrig.toString().trim();
		entry.setField(m_searchField, (result.length() > 0 ? result : null));
	}

	public int applyRule(Map<String, String> searchOptions, BibtexEntry entry) {
		return contains(searchOptions, entry) ? 1 : 0;
	}

    public boolean validateSearchStrings(Map<String, String> searchStrings) {
        return true;
    }

    public AbstractGroup deepCopy() {
		try {
			return new KeywordGroup(m_name, m_searchField, m_searchExpression,
					m_caseSensitive, m_regExp, m_context);
		} catch (Throwable t) {
			// this should never happen, because the constructor obviously
			// succeeded in creating _this_ instance!
			System.err.println("Internal error: Exception " + t
					+ " in KeywordGroup.deepCopy(). "
					+ "Please report this on www.sf.net/projects/jabref");
			return null;
		}
	}

	public boolean isCaseSensitive() {
		return m_caseSensitive;
	}

	public boolean isRegExp() {
		return m_regExp;
	}

	public String getSearchExpression() {
		return m_searchExpression;
	}

	public String getSearchField() {
		return m_searchField;
	}

	public boolean isDynamic() {
		return true;
	}
	
	public String getDescription() {
		return getDescriptionForPreview(m_searchField, m_searchExpression, m_caseSensitive,
				m_regExp); 
	}
	
	public static String getDescriptionForPreview(String field, String expr,
            boolean caseSensitive, boolean regExp) {
        return (regExp ? Globals.lang(
                "This group contains entries whose <b>%0</b> field contains the regular expression <b>%1</b>",
                field, Util.quoteForHTML(expr))
                : Globals.lang(
                "This group contains entries whose <b>%0</b> field contains the keyword <b>%1</b>",
                field, Util.quoteForHTML(expr))) + " (" + (caseSensitive ? Globals.lang("case sensitive")
                : Globals.lang("case insensitive")) + "). " + (regExp ? Globals.lang(
                "Entries cannot be manually assigned to or removed from this group.")
                : Globals.lang(
                "Additionally, entries whose <b>%0</b> field does not contain "
                        + "<b>%1</b> can be assigned manually to this group by selecting them "
                        + "then using either drag and drop or the context menu. "
                        + "This process adds the term <b>%1</b> to "
                        + "each entry's <b>%0</b> field. "
                        + "Entries can be removed manually from this group by selecting them "
                        + "then using the context menu. "
                        + "This process removes the term <b>%1</b> from "
                        + "each entry's <b>%0</b> field.",
                field, Util.quoteForHTML(expr)));
    }

	public String getShortDescription() {
		StringBuffer sb = new StringBuffer();
		sb.append("<b>");
		if (Globals.prefs.getBoolean("groupShowDynamic"))
            sb.append("<i>").append(Util.quoteForHTML(getName())).append("</i>");
		else
			sb.append(Util.quoteForHTML(getName()));
			sb.append("</b> - ");
            sb.append(Globals.lang("dynamic group"));
            sb.append("<b>");
            sb.append(m_searchField);
            sb.append("</b>");
            sb.append(Globals.lang("contains"));
            sb.append(" <b>");
            sb.append(Util.quoteForHTML(m_searchExpression));
            sb.append("</b>)");
		switch (getHierarchicalContext()) {
		case AbstractGroup.INCLUDING:
            sb.append(", ").append(Globals.lang("includes subgroups"));
			break;
		case AbstractGroup.REFINING:
        	sb.append(", ").append(Globals.lang("refines supergroup"));
			break;
		default:
			break;
		}
		return sb.toString();
	}

    public String getTypeId() {
        return ID;
    }
}
