/*
 Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref.groups;

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.regex.*;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.*;

import net.sf.jabref.*;
import net.sf.jabref.search.*;
import antlr.collections.AST;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.*;

/**
 * Dialog for creating or modifying groups. Operates directly on the Vector
 * containing group information.
 */
class GroupDialog extends JDialog {
    private static final int INDEX_EXPLICITGROUP = 0;
    private static final int INDEX_KEYWORDGROUP = 1;
    private static final int INDEX_SEARCHGROUP = 2;
    private static final int TEXTFIELD_LENGTH = 30;
    // for all types
    private JTextField m_name = new JTextField(TEXTFIELD_LENGTH);
    // JZTODO better descriptions
    private JRadioButton m_explicitRadioButton = new JRadioButton(
            "Assign entries to this group explicitly");
    private JRadioButton m_keywordsRadioButton = new JRadioButton(
            "Group entries by keywords");
    private JRadioButton m_searchRadioButton = new JRadioButton(
            "Group entries by search expression");

    // for KeywordGroup
    private JTextField m_kgSearchField = new JTextField(TEXTFIELD_LENGTH);
    private FieldTextField m_kgSearchTerm = new FieldTextField("keywords", "", false);
    private JCheckBox m_kgCaseSensitive = new JCheckBox("Case sensitive");
    private JCheckBox m_kgRegExp = new JCheckBox("Regular Expression");
    private JCheckBox m_kgIgnoreBraces = new JCheckBox("Ignore curly braces {} in field content");
    // for SearchGroup
    // JZTODO translation
    private JTextField m_sgSearchExpression = new JTextField(TEXTFIELD_LENGTH);
    private JCheckBox m_sgCaseSensitive = new JCheckBox("Case sensitive");
    private JCheckBox m_sgRegExp = new JCheckBox("Regular Expression");
    private JCheckBox m_sgIgnoreBraces = new JCheckBox("Ignore curly braces {} in field content");

    // for all types
    private JButton m_ok = new JButton(Globals.lang("Ok"));
    private JButton m_cancel = new JButton(Globals.lang("Cancel"));
    private JPanel m_optionsPanel = new JPanel();
    private JLabel m_description = new JLabel() {
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            // width must be smaller than width of enclosing JScrollPane
            // to prevent a horizontal scroll bar
            d.width = 1; 
            return d;
        }
    };

    private boolean m_okPressed = false;
    private final JabRefFrame m_parent;
    private final BasePanel m_basePanel;
    private AbstractGroup m_resultingGroup;
    private final AbstractGroup m_editedGroup;
    private CardLayout m_optionsLayout = new CardLayout();

    /**
     * Shows a group add/edit dialog.
     * 
     * @param jabrefFrame
     *            The parent frame.
     * @param defaultField
     *            The default grouping field.
     * @param editedGroup
     *            The group being edited, or null if a new group is to be
     *            created.
     */
    public GroupDialog(JabRefFrame jabrefFrame, BasePanel basePanel,
            AbstractGroup editedGroup) {
        super(jabrefFrame, Globals.lang("Edit group"), true);
        m_basePanel = basePanel;
        m_parent = jabrefFrame;
        m_editedGroup = editedGroup;

        // set default values (overwritten if editedGroup != null)
        m_kgSearchField.setText(jabrefFrame.prefs().get("groupsDefaultField"));

        // configure elements
        ButtonGroup groupType = new ButtonGroup();
        groupType.add(m_explicitRadioButton);
        groupType.add(m_keywordsRadioButton);
        groupType.add(m_searchRadioButton);
        m_description.setVerticalAlignment(JLabel.TOP);
        getRootPane().setDefaultButton(m_ok);
        
        // build individual layout cards for each group
        m_optionsPanel.setLayout(m_optionsLayout);
        // ... for explicit group
        m_optionsPanel.add(new JPanel(),""+INDEX_EXPLICITGROUP);
        // ... for keyword group
        FormLayout layoutKG = new FormLayout(
                "right:pref, 4dlu, fill:1dlu:grow, 2dlu, left:pref");
        DefaultFormBuilder builderKG = new DefaultFormBuilder(layoutKG);
        builderKG.append("Field");
        builderKG.append(m_kgSearchField,3);
        builderKG.nextLine();
        builderKG.append("Term");
        builderKG.append(m_kgSearchTerm);
        builderKG.append(new FieldContentSelector(m_parent, m_basePanel, this,
                m_kgSearchTerm, m_basePanel.metaData(), null));
        builderKG.nextLine();
        builderKG.append(m_kgCaseSensitive,3);
        builderKG.nextLine();
        builderKG.append(m_kgRegExp,3);
        builderKG.nextLine();
        builderKG.append(m_kgIgnoreBraces,3);
        m_optionsPanel.add(builderKG.getPanel(),""+INDEX_KEYWORDGROUP);
        // ... for search group
        FormLayout layoutSG = new FormLayout(
            "right:pref, 4dlu, fill:1dlu:grow");
        DefaultFormBuilder builderSG = new DefaultFormBuilder(layoutSG);
        builderSG.append("Expression");
        builderSG.append(m_sgSearchExpression);
        builderSG.nextLine();
        builderSG.append(m_sgCaseSensitive,3);
        builderSG.nextLine();
        builderSG.append(m_sgRegExp,3);
        builderSG.nextLine();
        builderSG.append(m_sgIgnoreBraces,3);
        m_optionsPanel.add(builderSG.getPanel(),""+INDEX_SEARCHGROUP);
        // ... for buttons panel
        FormLayout layoutBP = new FormLayout(
                "pref, 4dlu, pref", "p");
        layoutBP.setColumnGroups(new int[][]{{1, 3}});
        DefaultFormBuilder builderBP = new DefaultFormBuilder(layoutBP);
        builderBP.append(m_ok);
        builderBP.add(m_cancel);

        // create layout
        FormLayout layoutAll = new FormLayout(
                "right:pref, 4dlu, fill:500px, 4dlu, fill:pref",
                "p, 3dlu, p, 3dlu, p, 0dlu, p, 0dlu, p, 3dlu, p, 3dlu, " +
                "p, 3dlu, p, 3dlu, top:80dlu, 9dlu, p, , 9dlu, p");

        // ...for keyword group
        DefaultFormBuilder builderAll = new DefaultFormBuilder(layoutAll);
        builderAll.setDefaultDialogBorder();
        builderAll.appendSeparator("General");
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append("Name");
        builderAll.append(m_name);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(m_explicitRadioButton,5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(m_keywordsRadioButton,5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(m_searchRadioButton,5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.appendSeparator("Options");
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(m_optionsPanel,5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.appendSeparator("Description");
        builderAll.nextLine();
        builderAll.nextLine();
        JScrollPane sp = new JScrollPane(m_description, 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            public Dimension getPreferredSize() {
                return getMaximumSize();
            }
        };
        builderAll.append(sp,5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.appendSeparator();
        builderAll.nextLine();
        builderAll.nextLine();
        CellConstraints cc = new CellConstraints();
        builderAll.add(builderBP.getPanel(), 
                cc.xyw(builderAll.getColumn(), builderAll.getRow(), 5, "center, fill"));
        
        Container cp = getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.add(builderAll.getPanel());
        pack();
        setResizable(false);
        updateComponents();
        setLayoutForSelectedGroup();
        Util.placeDialog(this, m_parent);

        // add listeners
        ItemListener radioButtonItemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                setLayoutForSelectedGroup();
                updateComponents();
            }
        };
        m_explicitRadioButton.addItemListener(radioButtonItemListener);
        m_keywordsRadioButton.addItemListener(radioButtonItemListener);
        m_searchRadioButton.addItemListener(radioButtonItemListener);

        m_cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        m_ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                m_okPressed = true;
                if (m_explicitRadioButton.isSelected()) {
                    if (m_editedGroup instanceof ExplicitGroup) {
                        // keep assignments from possible previous ExplicitGroup
                        m_resultingGroup = m_editedGroup.deepCopy();
                        m_resultingGroup.setName(m_name.getText().trim());
                    } else {
                        m_resultingGroup = new ExplicitGroup(m_name.getText()
                                .trim(), m_basePanel.database());
                        if (m_editedGroup != null)
                            addPreviousEntries();
                    }
                } else if (m_keywordsRadioButton.isSelected()) {
                    // regex is correct, otherwise OK would have been disabled
                    // therefore I don't catch anything here
                    m_resultingGroup = new KeywordGroup(
                            m_name.getText().trim(), m_kgSearchField.getText()
                                    .trim(), m_kgSearchTerm.getText().trim(),
                            m_kgCaseSensitive.isSelected(), m_kgRegExp
                                    .isSelected());
                } else if (m_searchRadioButton.isSelected()) {
                    try {
                        // regex is correct, otherwise OK would have been
                        // disabled
                        // therefore I don't catch anything here
                        m_resultingGroup = new SearchGroup(m_name.getText()
                                .trim(), m_sgSearchExpression.getText().trim(),
                                m_sgCaseSensitive.isSelected(), m_sgRegExp
                                        .isSelected());
                    } catch (Exception e1) {
                        // should never happen
                    }
                }
                dispose();
            }
        });

        CaretListener caretListener = new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                updateComponents();
            }
        };

        ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateComponents();
            }
        };

        m_name.addCaretListener(caretListener);
        m_kgSearchField.addCaretListener(caretListener);
        m_kgSearchTerm.addCaretListener(caretListener);
        m_kgCaseSensitive.addItemListener(itemListener);
        m_kgRegExp.addItemListener(itemListener);
        m_kgIgnoreBraces.addItemListener(itemListener);
        m_sgSearchExpression.addCaretListener(caretListener);
        m_sgRegExp.addItemListener(itemListener);
        m_sgCaseSensitive.addItemListener(itemListener);
        m_sgIgnoreBraces.addItemListener(itemListener);

        // configure for current type
        if (editedGroup instanceof KeywordGroup) {
            KeywordGroup group = (KeywordGroup) editedGroup;
            m_name.setText(group.getName());
            m_kgSearchField.setText(group.getSearchField());
            m_kgSearchTerm.setText(group.getSearchExpression());
            m_kgCaseSensitive.setSelected(group.isCaseSensitive());
            m_kgRegExp.setSelected(group.isRegExp());
            // JZTODO curly braces...
            m_keywordsRadioButton.setSelected(true);
        } else if (editedGroup instanceof SearchGroup) {
            SearchGroup group = (SearchGroup) editedGroup;
            m_name.setText(group.getName());
            m_sgSearchExpression.setText(group.getSearchExpression());
            m_sgCaseSensitive.setSelected(group.isCaseSensitive());
            m_sgRegExp.setSelected(group.isRegExp());
            m_searchRadioButton.setSelected(true);
            // JZTODO curly braces...
        } else if (editedGroup instanceof ExplicitGroup) {
            m_name.setText(editedGroup.getName());
            m_explicitRadioButton.setSelected(true);
        }
    }

    public boolean okPressed() {
        return m_okPressed;
    }

    public AbstractGroup getResultingGroup() {
        return m_resultingGroup;
    }

    private void setLayoutForSelectedGroup() {
        if (m_explicitRadioButton.isSelected())
            m_optionsLayout.show(m_optionsPanel, String.valueOf(INDEX_EXPLICITGROUP));
        else if (m_keywordsRadioButton.isSelected())
            m_optionsLayout.show(m_optionsPanel, String.valueOf(INDEX_KEYWORDGROUP));
        else if (m_searchRadioButton.isSelected())
            m_optionsLayout.show(m_optionsPanel, String.valueOf(INDEX_SEARCHGROUP));
    }

    private void updateComponents() {
        // all groups need a name
        boolean okEnabled = m_name.getText().trim().length() > 0;
        String s1, s2;
        if (m_keywordsRadioButton.isSelected()) {
            s1 = m_kgSearchField.getText().trim();
            okEnabled = okEnabled && s1.length() > 0 && s1.indexOf(' ') < 0;
            s2 = m_kgSearchTerm.getText().trim();
            okEnabled = okEnabled && s2.length() > 0;
            if (!okEnabled) {
                setDescription("Please enter the field to search (e.g. <b>keywords</b>) and the term to search it for (e.g. <b>electrical</b>).");
            } else {
                if (m_kgRegExp.isSelected()) {
                    try {
                        Pattern.compile(s2);
                        setDescription(getDescriptionForKeywordGroup(s1, s2,
                                m_kgCaseSensitive.isSelected(), m_kgRegExp.isSelected()));
                    } catch (Exception e) {
                        okEnabled = false;
                        setDescription(formatRegExException(s2,e));
                    }
                }
            }
        } else if (m_searchRadioButton.isSelected()) {
            s1 = m_sgSearchExpression.getText().trim();
            okEnabled = okEnabled & s1.length() > 0;
            if (!okEnabled) {
                setDescription("Please enter a search term. For example, to search all fields for <b>Smith</b>, enter:<p>" +
                        "<tt>smith</tt><p>" +
                        "To search the field <b>Author</b> for <b>Smith</b> and the field <b>Title</b> for <b>electrical</b>, enter:<p>" +
                        "<tt>author=smith and title=electrical</tt>");
            } else {
                AST ast = SearchExpressionParser.checkSyntax(s1, m_sgCaseSensitive
                        .isSelected(), m_sgRegExp.isSelected());
                setDescription(getDescriptionForSearchGroup(s1, ast,
                        m_sgCaseSensitive.isSelected(), m_sgRegExp.isSelected()));
                if (m_sgRegExp.isSelected()) {
                    try {
                        Pattern.compile(s1);
                    } catch (Exception e) {
                        okEnabled = false;
                        setDescription(formatRegExException(s1,e));
                    }
                }
            }
        } else if (m_explicitRadioButton.isSelected()) {
            setDescription(getDescriptionForExplicitGroup());
        }
        m_ok.setEnabled(okEnabled);
    }

    /**
     * This is used when a group is converted and the new group supports
     * explicit adding of entries: All entries that match the previous group are
     * added to the new group.
     */
    private void addPreviousEntries() {
        // JZTODO in general, this should create undo information
        // because it might affect the entries. currently, it is only
        // used for ExplicitGroups; the undo information for this case is
        // contained completely in the UndoableModifyGroup object.
        // JZTODO lyrics...
        int i = JOptionPane
                .showConfirmDialog(
                        m_basePanel.frame(),
                        "Assign all entries that matched the previous group to this group?",
                        "Conversion to an Explicit Group",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (i == JOptionPane.NO_OPTION)
            return;
        BibtexEntry entry;
        for (Iterator it = m_basePanel.database().getEntries().iterator(); it
                .hasNext();) {
            entry = (BibtexEntry) it.next();
            if (m_editedGroup.contains(entry))
                ((ExplicitGroup) m_resultingGroup).addEntry(entry);
        }
    }

    private String getDescriptionForExplicitGroup() {
        return "This group contains entries based on explicit assignment. "
                + "Entries can be assigned to this group by selecting them "
                + "then using either drag and drop or the context menu. "
                + "Entries can be removed from this group by selecting them "
                + "then using the context menu. Every entry assigned to this group " 
                + "must have a unique key.";
    }

    private String getDescriptionForKeywordGroup(String field, String expr,
            boolean caseSensitive, boolean regExp) {
        StringBuffer sb = new StringBuffer();
        sb.append("This group contains entries whose <b>" + field + "</b> field ");
        sb.append(regExp ? "matches the regular expression "
                : "contains the term ");
        sb.append("<b>" + expr + "</b>");
        sb.append(caseSensitive ? " (case sensitive). "
                : " (case insensitive). ");
        sb
                .append(regExp ? "Entries cannot be explicitly assigned to or removed from this group."
                        : "Additionally, entries whose <b>"
                                + field
                                + "</b> field does not contain "
                                + "<b>"
                                + expr
                                + "</b> can be assigned to this group by selecting them "
                                + "then using either drag and drop or the context menu. "
                                + "This process adds the term <b>"
                                + expr
                                + "</b> to "
                                + "each entry's <b>"
                                + field
                                + "</b> field. "
                                + "Entries can be removed from this group by selecting them "
                                + "then using the context menu. "
                                + "This process removes the term <b>" + expr
                                + "</b> from " + "each  entry's <b>" + field
                                + "</b> field. ");
        return sb.toString();
    }

    private String getDescriptionForSearchGroup(String expr, AST ast,
            boolean caseSensitive, boolean regExp) {
        StringBuffer sb = new StringBuffer();
        if (ast == null) {
            sb.append("This group contains entries in which any field ");
            sb.append(regExp ? "matches the regular expression "
                    : "contains the term ");
            sb.append("<b>"
                    + expr
                    + "</b> "
                    + (caseSensitive ? "(case sensitive). "
                            : "(case insensitive). "));
            sb
                    .append("Entries cannot be explicitly assigned to or removed from this group.");
            sb
                    .append("<p><br>Hint: To search specific fields only, enter for example:"
                            + "<p><tt>author=smith and title=electrical</tt>");
            return sb.toString();
        }
        // describe advanced search expression
        sb.append("This group contains entries in which ");
        sb.append(describeSearchGroupNode(ast, regExp, false, false, false));
        sb
                .append(". The search is "
                        + (caseSensitive ? "case sensitive"
                                : "case insensitive") + ".");
        return sb.toString();
    }

    private String describeSearchGroupNode(AST node, boolean regExp,
            boolean not, boolean and, boolean or) {
        StringBuffer sb = new StringBuffer();
        switch (node.getType()) {
        case SearchExpressionTreeParserTokenTypes.And:
            if (not)
                sb.append("not ");
            // if there was an "or" in this subtree so far, braces may be needed
            if (or || not)
                sb.append("(");
            sb.append(describeSearchGroupNode(node.getFirstChild(), regExp,
                    false, true, false)
                    + " and "
                    + describeSearchGroupNode(node.getFirstChild()
                            .getNextSibling(), regExp, false, true, false));
            if (or || not)
                sb.append(")");
            return sb.toString();
        case SearchExpressionTreeParserTokenTypes.Or:
            if (not)
                sb.append("not ");
            // if there was an "and" in this subtree so far, braces may be
            // needed
            if (and || not)
                sb.append("(");
            sb.append(describeSearchGroupNode(node.getFirstChild(), regExp,
                    false, false, true)
                    + " or "
                    + describeSearchGroupNode(node.getFirstChild()
                            .getNextSibling(), regExp, false, false, true));
            if (and || not)
                sb.append(")");
            return sb.toString();
        case SearchExpressionTreeParserTokenTypes.Not:
            return describeSearchGroupNode(node.getFirstChild(), regExp, true,
                    and, or);
        default:
            node = node.getFirstChild();
            String field = node.getText();
            node = node.getNextSibling();
            String matchType;
            switch (node.getType()) {
            case SearchExpressionTreeParserTokenTypes.LITERAL_contains:
            case SearchExpressionTreeParserTokenTypes.EQUAL:
                matchType = not ? "doesn't contain" : "contains";
                break;
            case SearchExpressionTreeParserTokenTypes.LITERAL_matches:
            case SearchExpressionTreeParserTokenTypes.EEQUAL:
                matchType = not ? "doesn't match" : "matches";
                break;
            case SearchExpressionTreeParserTokenTypes.NEQUAL:
                matchType = "doesn't contain";
                break;
            default:
                matchType = "?"; // this should never happen
            }
            node = node.getNextSibling();
            String term = node.getText();
            boolean regExpFieldSpec = !Pattern.matches("\\w+", field);
            sb
                    .append(regExpFieldSpec ? "any field that matches the regular expression"
                            : "the field");
            sb.append(" <b>" + field + "</b> " + matchType);
            sb.append((regExp ? " the regular expression" : " the term")
                    + " <b>" + term + "</b>");
            return sb.toString();
        }
    }
    
    protected void setDescription(String description) {
        m_description.setText("<html>" + description + "</html>");
    }
    
    protected String formatRegExException(String regExp, Exception e) {
        String s = "The regular expression <b>" + regExp
                + "</b> is invalid:<p><tt>" 
                + e.getMessage().replaceAll("\\n","<br>");
        if (!(e instanceof PatternSyntaxException))
            return s;
        int lastNewline = s.lastIndexOf("<br>");
        int hat = s.lastIndexOf("^");
        if (lastNewline >=0  && hat >= 0 && hat > lastNewline)
            return s.substring(0,lastNewline+4) 
            + s.substring(lastNewline+4).replaceAll(" ", "&nbsp;");
        return s;
    }
}
