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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.FieldContentSelector;
import net.sf.jabref.FieldTextField;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Util;
import net.sf.jabref.search.SearchExpressionParser;
import antlr.collections.AST;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

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
        private JRadioButton m_explicitRadioButton = new JRadioButton(Globals
                        .lang("Statically group entries by manual assignment"));
        private JRadioButton m_keywordsRadioButton = new JRadioButton(
                        Globals.lang("Dynamically group entries by searching a field for a keyword"));
        private JRadioButton m_searchRadioButton = new JRadioButton(Globals
                        .lang("Dynamically group entries by a free-form search expression"));
        private JRadioButton m_independentButton = new JRadioButton( // JZTODO lyrics
                        Globals.lang("Independent group: When selected, view only this group's entries"));
        private JRadioButton m_intersectionButton = new JRadioButton( // JZTODO lyrics
                        Globals.lang("Refine supergroup: When selected, view entries contained in both this group and its supergroup"));
        private JRadioButton m_unionButton = new JRadioButton( // JZTODO lyrics
                        Globals.lang("Include subgroups: When selected, view entries contained in this group or its subgroups"));
        // for KeywordGroup
        private JTextField m_kgSearchField = new JTextField(TEXTFIELD_LENGTH);
        private FieldTextField m_kgSearchTerm = new FieldTextField("keywords", "",
                        false);
        private JCheckBox m_kgCaseSensitive = new JCheckBox(Globals
                        .lang("Case sensitive"));
        private JCheckBox m_kgRegExp = new JCheckBox(Globals
                        .lang("Regular Expression"));
        // for SearchGroup
        private JTextField m_sgSearchExpression = new JTextField(TEXTFIELD_LENGTH);
        private JCheckBox m_sgCaseSensitive = new JCheckBox(Globals
                        .lang("Case sensitive"));
        private JCheckBox m_sgRegExp = new JCheckBox(Globals
                        .lang("Regular Expression"));
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

        private AbstractUndoableEdit m_undoAddPreviousEntires = null;

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
                ButtonGroup groupHierarchy = new ButtonGroup();
                groupHierarchy.add(m_independentButton);
                groupHierarchy.add(m_intersectionButton);
                groupHierarchy.add(m_unionButton);
                m_description.setVerticalAlignment(JLabel.TOP);
                getRootPane().setDefaultButton(m_ok);

                // build individual layout cards for each group
                m_optionsPanel.setLayout(m_optionsLayout);
                // ... for explicit group
                m_optionsPanel.add(new JPanel(), "" + INDEX_EXPLICITGROUP);
                // ... for keyword group
                FormLayout layoutKG = new FormLayout(
                                "right:pref, 4dlu, fill:1dlu:grow, 2dlu, left:pref");
                DefaultFormBuilder builderKG = new DefaultFormBuilder(layoutKG);
                builderKG.append(Globals.lang("Field"));
                builderKG.append(m_kgSearchField, 3);
                builderKG.nextLine();
                builderKG.append(Globals.lang("Keyword"));
                builderKG.append(m_kgSearchTerm);
                builderKG.append(new FieldContentSelector(m_parent, m_basePanel, this,
                                m_kgSearchTerm, m_basePanel.metaData(), null, true, ", "));
                builderKG.nextLine();
                builderKG.append(m_kgCaseSensitive, 3);
                builderKG.nextLine();
                builderKG.append(m_kgRegExp, 3);
                m_optionsPanel.add(builderKG.getPanel(), "" + INDEX_KEYWORDGROUP);
                // ... for search group
                FormLayout layoutSG = new FormLayout("right:pref, 4dlu, fill:1dlu:grow");
                DefaultFormBuilder builderSG = new DefaultFormBuilder(layoutSG);
                builderSG.append(Globals.lang("Search expression"));
                builderSG.append(m_sgSearchExpression);
                builderSG.nextLine();
                builderSG.append(m_sgCaseSensitive, 3);
                builderSG.nextLine();
                builderSG.append(m_sgRegExp, 3);
                m_optionsPanel.add(builderSG.getPanel(), "" + INDEX_SEARCHGROUP);
                // ... for buttons panel
                FormLayout layoutBP = new FormLayout("pref, 4dlu, pref", "p");
                layoutBP.setColumnGroups(new int[][] { { 1, 3 } });
                ButtonBarBuilder builderBP = new ButtonBarBuilder();
                builderBP.addGlue();
                builderBP.addGridded(m_ok);
                builderBP.addGridded(m_cancel);
                builderBP.addGlue();
                builderBP.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

                // create layout
                FormLayout layoutAll = new FormLayout(
                                "right:pref, 4dlu, fill:600px, 4dlu, fill:pref",
                                "p, 3dlu, p, 3dlu, p, 0dlu, p, 0dlu, p, 3dlu, p, 3dlu, p, "
                                                + "0dlu, p, 0dlu, p, 3dlu, p, 3dlu, "
                                                + "p, 3dlu, p, 3dlu, top:80dlu, 9dlu, p, 9dlu, p");

                DefaultFormBuilder builderAll = new DefaultFormBuilder(layoutAll);
                builderAll.setDefaultDialogBorder();
                builderAll.appendSeparator(Globals.lang("General"));
                builderAll.nextLine();
                builderAll.nextLine();
                builderAll.append(Globals.lang("Name"));
                builderAll.append(m_name);
                builderAll.nextLine();
                builderAll.nextLine();
                builderAll.append(m_explicitRadioButton, 5);
                builderAll.nextLine();
                builderAll.nextLine();
                builderAll.append(m_keywordsRadioButton, 5);
                builderAll.nextLine();
                builderAll.nextLine();
                builderAll.append(m_searchRadioButton, 5);
                builderAll.nextLine();
                builderAll.nextLine();
                builderAll.appendSeparator(Globals.lang("Hierarchical context")); // JZTODO lyrics
                builderAll.nextLine();
                builderAll.nextLine();
                builderAll.append(m_independentButton, 5);
                builderAll.nextLine();
                builderAll.nextLine();
                builderAll.append(m_intersectionButton, 5);
                builderAll.nextLine();
                builderAll.nextLine();
                builderAll.append(m_unionButton, 5);
                builderAll.nextLine();
                builderAll.nextLine();
                builderAll.appendSeparator(Globals.lang("Options"));
                builderAll.nextLine();
                builderAll.nextLine();
                builderAll.append(m_optionsPanel, 5);
                builderAll.nextLine();
                builderAll.nextLine();
                builderAll.appendSeparator(Globals.lang("Description"));
                builderAll.nextLine();
                builderAll.nextLine();
                JScrollPane sp = new JScrollPane(m_description,
                                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
                        public Dimension getPreferredSize() {
                                return getMaximumSize();
                        }
                };
                builderAll.append(sp, 5);
                builderAll.nextLine();
                builderAll.nextLine();
                builderAll.appendSeparator();
                builderAll.nextLine();
                builderAll.nextLine();
                //CellConstraints cc = new CellConstraints();
                //builderAll.add(builderBP.getPanel(), cc.xyw(builderAll.getColumn(),
                //                builderAll.getRow(), 5, "center, fill"));

                Container cp = getContentPane();
                //cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
                cp.add(builderAll.getPanel(), BorderLayout.CENTER);
                cp.add(builderBP.getPanel(), BorderLayout.SOUTH);
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
                                                m_resultingGroup.setHierarchicalContext(getContext());
                                        } else {
                                                m_resultingGroup = new ExplicitGroup(m_name.getText()
                                                                .trim(), getContext());
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
                                                                        .isSelected(), getContext());
                                        if ((m_editedGroup instanceof ExplicitGroup || m_editedGroup instanceof SearchGroup)
                                                        && m_resultingGroup.supportsAdd()) {
                                                addPreviousEntries();
                                        }
                                } else if (m_searchRadioButton.isSelected()) {
                                        try {
                                                // regex is correct, otherwise OK would have been
                                                // disabled
                                                // therefore I don't catch anything here
                                                m_resultingGroup = new SearchGroup(m_name.getText()
                                                                .trim(), m_sgSearchExpression.getText().trim(),
                                                                m_sgCaseSensitive.isSelected(), m_sgRegExp
                                                                                .isSelected(), getContext());
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
                m_sgSearchExpression.addCaretListener(caretListener);
                m_sgRegExp.addItemListener(itemListener);
                m_sgCaseSensitive.addItemListener(itemListener);

                // configure for current type
                if (editedGroup instanceof KeywordGroup) {
                        KeywordGroup group = (KeywordGroup) editedGroup;
                        m_name.setText(group.getName());
                        m_kgSearchField.setText(group.getSearchField());
                        m_kgSearchTerm.setText(group.getSearchExpression());
                        m_kgCaseSensitive.setSelected(group.isCaseSensitive());
                        m_kgRegExp.setSelected(group.isRegExp());
                        m_keywordsRadioButton.setSelected(true);
                        setContext(editedGroup.getHierarchicalContext());
                } else if (editedGroup instanceof SearchGroup) {
                        SearchGroup group = (SearchGroup) editedGroup;
                        m_name.setText(group.getName());
                        m_sgSearchExpression.setText(group.getSearchExpression());
                        m_sgCaseSensitive.setSelected(group.isCaseSensitive());
                        m_sgRegExp.setSelected(group.isRegExp());
                        m_searchRadioButton.setSelected(true);
                        setContext(editedGroup.getHierarchicalContext());
                } else if (editedGroup instanceof ExplicitGroup) {
                        m_name.setText(editedGroup.getName());
                        m_explicitRadioButton.setSelected(true);
                        setContext(editedGroup.getHierarchicalContext());
                } else { // creating new group -> defaults!
                        m_explicitRadioButton.setSelected(true);
                        setContext(AbstractGroup.INDEPENDENT);
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
                        m_optionsLayout.show(m_optionsPanel, String
                                        .valueOf(INDEX_EXPLICITGROUP));
                else if (m_keywordsRadioButton.isSelected())
                        m_optionsLayout.show(m_optionsPanel, String
                                        .valueOf(INDEX_KEYWORDGROUP));
                else if (m_searchRadioButton.isSelected())
                        m_optionsLayout.show(m_optionsPanel, String
                                        .valueOf(INDEX_SEARCHGROUP));
        }

        private void updateComponents() {
                // all groups need a name
                boolean okEnabled = m_name.getText().trim().length() > 0;
                if (!okEnabled) {
                        setDescription(Globals.lang("Please enter a name for the group."));
                        m_ok.setEnabled(false);
                        return;
                }
                String s1, s2;
                if (m_keywordsRadioButton.isSelected()) {
                        s1 = m_kgSearchField.getText().trim();
                        okEnabled = okEnabled && s1.matches("\\w+");
                        s2 = m_kgSearchTerm.getText().trim();
                        okEnabled = okEnabled && s2.length() > 0;
                        if (!okEnabled) {
                                setDescription(Globals
                                                .lang("Please enter the field to search (e.g. <b>keywords</b>) and the keyword to search it for (e.g. <b>electrical</b>)."));
                        } else {
                                if (m_kgRegExp.isSelected()) {
                                        try {
                                                Pattern.compile(s2);
                                                setDescription(KeywordGroup.getDescriptionForPreview(s1, s2,
                                                                m_kgCaseSensitive.isSelected(), m_kgRegExp
                                                                                .isSelected()));
                                        } catch (Exception e) {
                                                okEnabled = false;
                                                setDescription(formatRegExException(s2, e));
                                        }
                                } else {
                                        setDescription(KeywordGroup.getDescriptionForPreview(s1, s2,
                                                        m_kgCaseSensitive.isSelected(), m_kgRegExp
                                                                        .isSelected()));
                                }
                        }
                        setNameFontItalic(true);
                } else if (m_searchRadioButton.isSelected()) {
                        s1 = m_sgSearchExpression.getText().trim();
                        okEnabled = okEnabled & s1.length() > 0;
                        if (!okEnabled) {
                                setDescription(Globals
                                                .lang("Please enter a search term. For example, to search all fields for <b>Smith</b>, enter%c<p>"
                                                                + "<tt>smith</tt><p>"
                                                                + "To search the field <b>Author</b> for <b>Smith</b> and the field <b>Title</b> for <b>electrical</b>, enter%c<p>"
                                                                + "<tt>author%esmith and title%eelectrical</tt>"));
                        } else {
                                AST ast = SearchExpressionParser
                                                .checkSyntax(s1, m_sgCaseSensitive.isSelected(),
                                                                m_sgRegExp.isSelected());
                                setDescription(SearchGroup.getDescriptionForPreview(s1, ast,
                                                m_sgCaseSensitive.isSelected(), m_sgRegExp.isSelected()));
                                if (m_sgRegExp.isSelected()) {
                                        try {
                                                Pattern.compile(s1);
                                        } catch (Exception e) {
                                                okEnabled = false;
                                                setDescription(formatRegExException(s1, e));
                                        }
                                }
                        }
                        setNameFontItalic(true);
                } else if (m_explicitRadioButton.isSelected()) {
                        setDescription(ExplicitGroup.getDescriptionForPreview());
                        setNameFontItalic(false);
                }
                m_ok.setEnabled(okEnabled);
        }

        /**
         * This is used when a group is converted and the new group supports
         * explicit adding of entries: All entries that match the previous group are
         * added to the new group.
         */
        private void addPreviousEntries() {
                // JZTODO lyrics...
                int i = JOptionPane.showConfirmDialog(m_basePanel.frame(), Globals
                                .lang("Assign the original group's entries to this group?"),
                                Globals.lang("Change of Grouping Method"),
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (i == JOptionPane.NO_OPTION)
                        return;
                Vector<BibtexEntry> vec = new Vector<BibtexEntry>();
                for (BibtexEntry entry : m_basePanel.database().getEntries()){
                        if (m_editedGroup.contains(entry))
                        	vec.add(entry);
                }
                if (vec.size() > 0) {
                        BibtexEntry[] entries = new BibtexEntry[vec.size()];
                        vec.toArray(entries);
                        if (!Util.warnAssignmentSideEffects(new AbstractGroup[]{m_resultingGroup},
                                        entries, m_basePanel.getDatabase(), this))
                                return;
                        // the undo information for a conversion to an ExplicitGroup is
                        // contained completely in the UndoableModifyGroup object.
                        if (!(m_resultingGroup instanceof ExplicitGroup))
                                m_undoAddPreviousEntires = m_resultingGroup.add(entries);
                }
        }

        protected void setDescription(String description) {
                m_description.setText("<html>" + description + "</html>");
        }

        protected String formatRegExException(String regExp, Exception e) {
        String[] sa = e.getMessage().split("\\n");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < sa.length; ++i) {
            if (i > 0)
                sb.append("<br>");
            sb.append(Util.quoteForHTML(sa[i]));
        }
                String s = Globals.lang(
                                "The regular expression <b>%0</b> is invalid%c",
                Util.quoteForHTML(regExp))
                                + "<p><tt>"
                                + sb.toString()
                                + "</tt>";
                if (!(e instanceof PatternSyntaxException))
                        return s;
                int lastNewline = s.lastIndexOf("<br>");
                int hat = s.lastIndexOf("^");
                if (lastNewline >= 0 && hat >= 0 && hat > lastNewline)
                        return s.substring(0, lastNewline + 4)
                                        + s.substring(lastNewline + 4).replaceAll(" ", "&nbsp;");
                return s;
        }

        /**
         * Returns an undo object for adding the edited group's entries to the new
         * group, or null if this did not occur.
         */
        public AbstractUndoableEdit getUndoForAddPreviousEntries() {
                return m_undoAddPreviousEntires;
        }

        /** Sets the font of the name entry field. */
        protected void setNameFontItalic(boolean italic) {
                Font f = m_name.getFont();
                if (f.isItalic() != italic) {
                        f = f.deriveFont(italic ? Font.ITALIC : Font.PLAIN);
                        m_name.setFont(f);
                }
        }

        /**
         * Returns the int representing the selected hierarchical group context.
         */
        protected int getContext() {
                if (m_independentButton.isSelected())
                        return AbstractGroup.INDEPENDENT;
                if (m_intersectionButton.isSelected())
                        return AbstractGroup.REFINING;
                if (m_unionButton.isSelected())
                        return AbstractGroup.INCLUDING;
                return AbstractGroup.INDEPENDENT; // default
        }

        protected void setContext(int context) {
                switch (context) {
                case AbstractGroup.REFINING:
                        m_intersectionButton.setSelected(true);
                        return;
                case AbstractGroup.INCLUDING:
                        m_unionButton.setSelected(true);
                        return;
                case AbstractGroup.INDEPENDENT:
                default:
                        m_independentButton.setSelected(true);
                        return;
                }
        }

}
