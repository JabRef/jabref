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

import java.io.StringReader;
import java.util.*;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.*;
import net.sf.jabref.search.*;
import net.sf.jabref.util.QuotedStringTokenizer;
import antlr.RecognitionException;
import antlr.collections.AST;

/**
 * @author jzieren
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class SearchGroup extends AbstractGroup implements SearchRule {
    public static final String ID = "SearchGroup:";
    private final String m_searchExpression;
    private final boolean m_caseSensitive;
    private final boolean m_regExp;
    private final AST m_ast;
    private static final SearchExpressionTreeParser m_treeParser = new SearchExpressionTreeParser();
    /**
     * If m_searchExpression is in valid syntax for advanced search, <b>this
     * </b> will do the search; otherwise, either <b>RegExpRule </b> or
     * <b>SimpleSearchRule </b> will be used.
     */
    private final SearchRule m_searchRule;
    // Options for non-advanced search
    private final boolean m_searchAllFields;
    private final boolean m_searchRequiredFields;
    private final boolean m_searchOptionalFields;
    private final boolean m_searchGeneralFields;

    /**
     * Creates a SearchGroup with the specified properties.
     */
    public SearchGroup(String name, String searchExpression,
            boolean caseSensitive, boolean regExp, boolean searchAllFields,
            boolean searchRequiredFields, boolean searchOptionalFields,
            boolean searchGeneralFields) {
        super(name);
        m_searchExpression = searchExpression;
        m_caseSensitive = caseSensitive;
        m_regExp = regExp;
        m_searchAllFields = searchAllFields;
        m_searchRequiredFields = searchRequiredFields;
        m_searchOptionalFields = searchOptionalFields;
        m_searchGeneralFields = searchGeneralFields;

        // create AST
        AST ast = null;
        try {
            SearchExpressionParser parser = new SearchExpressionParser(
                    new SearchExpressionLexer(new StringReader(
                            m_searchExpression)));
            parser.caseSensitive = m_caseSensitive;
            parser.regex = m_regExp;
            parser.searchExpression();
            ast = parser.getAST();
        } catch (Exception e) {
            ast = null;
            // nothing to do; set m_ast to null -> regular plaintext search
        }
        m_ast = ast;

        if (m_ast != null) { // do advanced search
            m_searchRule = this;
        } else { // do plaintext search
            if (m_regExp)
                m_searchRule = new RegExpRule(m_caseSensitive,m_searchAllFields,m_searchRequiredFields,m_searchOptionalFields,m_searchGeneralFields);
            else
                m_searchRule = new SimpleSearchRule(m_caseSensitive,m_searchAllFields,m_searchRequiredFields,m_searchOptionalFields,m_searchGeneralFields);
        }

    }

    /**
     * Parses s and recreates the SearchGroup from it.
     * 
     * @param s
     *            The String representation obtained from
     *            SearchGroup.toString(), or null if incompatible
     */
    public static AbstractGroup fromString(String s, BibtexDatabase db, int version) throws Exception {
        if (!s.startsWith(ID))
            throw new Exception(
                    "Internal error: SearchGroup cannot be created from \"" + s
                            + "\"");
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(ID
                .length()), SEPARATOR, QUOTE_CHAR);
        switch (version) {
        case 0:
        case 1:
            String name = tok.nextToken();
            String expression = tok.nextToken();
            boolean caseSensitive = Integer.parseInt(tok.nextToken()) == 1;
            boolean regExp = Integer.parseInt(tok.nextToken()) == 1;
            boolean searchAllFields = Integer.parseInt(tok.nextToken()) == 1;
            boolean searchRequiredFields = Integer.parseInt(tok.nextToken()) == 1;
            boolean searchOptionalFields = Integer.parseInt(tok.nextToken()) == 1;
            boolean searchGeneralFields = Integer.parseInt(tok.nextToken()) == 1;
            return new SearchGroup(Util.unquote(name, QUOTE_CHAR), Util.unquote(
                    expression, QUOTE_CHAR), caseSensitive, regExp,
                    searchAllFields, searchRequiredFields, searchOptionalFields,
                    searchGeneralFields);
        default:
            throw new UnsupportedVersionException("SearchGroup",version);
        }
    }

    /**
     * @see net.sf.jabref.groups.AbstractGroup#getSearchRule()
     */
    public SearchRule getSearchRule() {
        return m_searchRule;
    }

    /**
     * Returns a String representation of this object that can be used to
     * reconstruct it.
     */
    public String toString() {
        return ID + Util.quote(m_name, SEPARATOR, QUOTE_CHAR) + SEPARATOR
                + Util.quote(m_searchExpression, SEPARATOR, QUOTE_CHAR) + SEPARATOR
                + (m_caseSensitive ? "1" : "0") + SEPARATOR
                + (m_regExp ? "1" : "0") + SEPARATOR
                + (m_searchAllFields ? "1" : "0") + SEPARATOR
                + (m_searchRequiredFields ? "1" : "0") + SEPARATOR
                + (m_searchOptionalFields ? "1" : "0") + SEPARATOR
                + (m_searchGeneralFields ? "1" : "0") + SEPARATOR;
    }

    public String getSearchExpression() {
        return m_searchExpression;
    }

    public boolean supportsAdd() {
        return false;
    }

    public boolean supportsRemove() {
        return false;
    }

    public AbstractUndoableEdit addSelection(BasePanel basePanel) {
        // nothing to do, add is not supported
        return null;
    }

    public AbstractUndoableEdit addSelection(BibtexEntry[] entries) {
        // nothing to do, add is not supported
        return null;
    }

    public AbstractUndoableEdit removeSelection(BasePanel basePanel) {
        // nothing to do, remove is not supported
        return null;
    }

    public AbstractUndoableEdit removeSelection(BibtexEntry[] entries) {
        // nothing to do, remove is not supported
        return null;
    }

    public boolean equals(Object o) {
        if (!(o instanceof SearchGroup))
            return false;
        SearchGroup other = (SearchGroup) o;
        return m_name.equals(other.m_name)
                && m_searchExpression.equals(other.m_searchExpression)
                && m_caseSensitive == other.m_caseSensitive
                && m_regExp == other.m_regExp
                && m_searchAllFields == other.m_searchAllFields
                && m_searchRequiredFields == other.m_searchRequiredFields
                && m_searchOptionalFields == other.m_searchOptionalFields
                && m_searchGeneralFields == other.m_searchGeneralFields;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.jabref.groups.AbstractGroup#contains(java.util.Map,
     *      net.sf.jabref.BibtexEntry)
     */
    public boolean contains(Map searchOptions, BibtexEntry entry) {
        return m_searchRule.applyRule(searchOptions,entry) == 0 ? false : true;
    }

    public boolean contains(BibtexEntry entry) {
        // use dummy map
        return contains(new HashMap(), entry);
    }
    
    public int applyRule(Map searchOptions, BibtexEntry entry) {
        if (m_ast == null)
            return 0; // this instance cannot be used as a SearchRule; should never happen
        try {
            return m_treeParser.apply(m_ast, entry);
        } catch (RecognitionException e) {
            return 0; // this should never occur
        }
    }

    public AbstractGroup deepCopy() {
        try {
            return new SearchGroup(m_name, m_searchExpression, m_caseSensitive,
                    m_regExp, m_searchAllFields, m_searchRequiredFields,
                    m_searchOptionalFields, m_searchGeneralFields);
        } catch (Throwable t) {
            // this should never happen, because the constructor obviously
            // succeeded in creating _this_ instance!
            System.err.println("Internal error: Exception " + t
                    + " in SearchGroup.deepCopy()");
            return null;
        }
    }

    public boolean isCaseSensitive() {
        return m_caseSensitive;
    }

    public boolean isRegExp() {
        return m_regExp;
    }
    public boolean searchAllFields() {
        return m_searchAllFields;
    }
    public boolean searchGeneralFields() {
        return m_searchGeneralFields;
    }
    public boolean searchOptionalFields() {
        return m_searchOptionalFields;
    }
    public boolean searchRequiredFields() {
        return m_searchRequiredFields;
    }
}
