/*
 All programs in this directory and subdirectories are published under the 
 GNU General Public License as described below.

 This program is free software; you can redistribute it and/or modify it 
 under the terms of the GNU General Public License as published by the Free 
 Software Foundation; either version 2 of the License, or (at your option) 
 any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT 
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 more details.

 You should have received a copy of the GNU General Public License along 
 with this program; if not, write to the Free Software Foundation, Inc., 59 
 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html
 */

package net.sf.jabref.groups;

import java.util.Map;
import java.util.regex.*;

import javax.swing.JOptionPane;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.*;
import net.sf.jabref.undo.*;
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
            String searchExpression, boolean caseSensitive, boolean regExp)
            throws IllegalArgumentException, PatternSyntaxException {
        super(name);
        m_searchField = searchField;
        m_searchExpression = searchExpression;
        m_caseSensitive = caseSensitive;
        m_regExp = regExp;
        if (m_regExp)
            compilePattern();
    }

    protected void compilePattern() throws IllegalArgumentException,
            PatternSyntaxException {
        m_pattern = m_caseSensitive ? Pattern.compile(m_searchExpression)
                : Pattern.compile(m_searchExpression, Pattern.CASE_INSENSITIVE);
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
                            + s + "\"");
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
                    QUOTE_CHAR), false, true);
        }
        case 1: {
            String name = tok.nextToken();
            String field = tok.nextToken();
            String expression = tok.nextToken();
            boolean caseSensitive = Integer.parseInt(tok.nextToken()) == 1;
            boolean regExp = Integer.parseInt(tok.nextToken()) == 1;
            return new KeywordGroup(Util.unquote(name, QUOTE_CHAR), Util
                    .unquote(field, QUOTE_CHAR), Util.unquote(expression,
                    QUOTE_CHAR), caseSensitive, regExp);
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

    public AbstractUndoableEdit addSelection(BasePanel basePanel) {
        if (!supportsAdd()) {
            // this should never happen
            basePanel.output("The group \"" + getName()
                    + "\" does not support the adding of entries.");
            return null;
        }
        for (int i = 0; i < GUIGlobals.ALL_FIELDS.length; ++i) {
            if (m_searchField.equals(GUIGlobals.ALL_FIELDS[i])
                    && !m_searchField.equals("keywords")) {
                if (!showWarningDialog(basePanel))
                    return null;
            }
        }

        BibtexEntry[] entries = basePanel.getSelectedEntries();
        AbstractUndoableEdit undo = addSelection(entries);

        if (entries.length > 0) // JZTODO: translation
            basePanel.output("Appended '" + m_searchExpression + "' to the '"
                    + m_searchField + "' field of " + entries.length + " entr"
                    + (entries.length > 1 ? "ies." : "y."));

        return undo;
    }

    public AbstractUndoableEdit addSelection(BibtexEntry[] entries) {
        if ((entries != null) && (entries.length > 0)) {
            NamedCompound ce = new NamedCompound("add to group");
            boolean modified = false;
            for (int i = 0; i < entries.length; i++) {
                if (applyRule(null, entries[i]) == 0) {
                    String oldContent = (String) entries[i]
                            .getField(m_searchField), pre = " ", post = "";
                    String newContent = (oldContent == null ? "" : oldContent
                            + pre)
                            + m_searchExpression + post;
                    entries[i].setField(m_searchField, newContent);

                    // Store undo information.
                    ce.addEdit(new UndoableFieldChange(entries[i],
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

    /**
     * Displays a warning message about changes to the entries due to adding
     * to/removal from a group.
     * 
     * @return true if the user chose to proceed, false otherwise.
     */
    private boolean showWarningDialog(BasePanel basePanel) {
        String message = "This action will modify the \""
                + m_searchField
                + "\" field "
                + "of your entries.\nThis could cause undesired changes to "
                + "your entries, so it\nis recommended that you change the field "
                + "in your group\ndefinition to \"keywords\" or a non-standard name."
                + "\n\nDo you still want to continue?";
        int choice = JOptionPane.showConfirmDialog(basePanel, message,
                "Warning", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return choice != JOptionPane.NO_OPTION;
    }

    public AbstractUndoableEdit removeSelection(BasePanel basePanel) {
        if (!supportsRemove()) {
            // this should never happen
            basePanel.output("The group \"" + getName()
                    + "\" does not support the removal of entries.");
            return null;
        }
        for (int i = 0; i < GUIGlobals.ALL_FIELDS.length; i++) {
            if (m_searchField.equals(GUIGlobals.ALL_FIELDS[i])
                    && !m_searchField.equals("keywords")) {
                if (!showWarningDialog(basePanel))
                    return null;
            }
        }

        BibtexEntry[] entries = basePanel.getSelectedEntries();
        AbstractUndoableEdit undo = removeSelection(entries);
        if (entries.length > 0)
            basePanel.output("Removed '" + m_searchExpression + "' from the '"
                    + m_searchField + "' field of " + entries.length + " entr"
                    + (entries.length > 1 ? "ies." : "y."));
        return undo;
    }

    public AbstractUndoableEdit removeSelection(BibtexEntry[] entries) {
        if ((entries != null) && (entries.length > 0)) {
            NamedCompound ce = new NamedCompound("remove from group");
            boolean modified = false;
            for (int i = 0; i < entries.length; ++i) {
                if (applyRule(null, entries[i]) > 0) {
                    String oldContent = (String) entries[i]
                            .getField(m_searchField);
                    removeMatches(entries[i]);
                    // Store undo information.
                    ce.addEdit(new UndoableFieldChange(entries[i],
                            m_searchField, oldContent, entries[i]
                                    .getField(m_searchField)));
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
                && m_regExp == other.m_regExp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.jabref.groups.AbstractGroup#contains(java.util.Map,
     *      net.sf.jabref.BibtexEntry)
     */
    public boolean contains(Map searchOptions, BibtexEntry entry) {
        return contains(entry);
    }

    public boolean contains(BibtexEntry entry) {
        String content = (String) entry.getField(m_searchField);
        if (content == null)
            return false;
        if (m_regExp)
            return m_pattern.matcher(content).find();
        if (m_caseSensitive)
            return content.indexOf(m_searchExpression) >= 0;
        content = content.toLowerCase();
        return content.indexOf(m_searchExpression.toLowerCase()) >= 0;
    }

    /**
     * Removes matches of searchString in the entry's field.
     */
    private void removeMatches(BibtexEntry entry) {
        String content = (String) entry.getField(m_searchField);
        StringBuffer sb = new StringBuffer();
        if (content != null) {
            String[] split = m_pattern.split(content);
            for (int i = 0; i < split.length; ++i)
                sb.append(split[i]);
        }
        entry.setField(m_searchField, (sb.length() > 0 ? sb.toString() : null));
    }

    public int applyRule(Map searchOptions, BibtexEntry entry) {
        return contains(searchOptions, entry) ? 1 : 0;
    }

    public AbstractGroup deepCopy() {
        try {
            return new KeywordGroup(m_name, m_searchField, m_searchExpression,
                    m_caseSensitive, m_regExp);
        } catch (Throwable t) {
            // this should never happen, because the constructor obviously
            // succeeded in creating _this_ instance!
            System.err.println("Internal error: Exception " + t
                    + " in KeywordGroup.deepCopy()");
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
}
