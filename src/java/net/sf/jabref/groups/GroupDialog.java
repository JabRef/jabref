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

import java.awt.Container;
import java.awt.event.*;
import java.io.StringReader;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.*;

import antlr.*;

import net.sf.jabref.*;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.components.*;
import net.sf.jabref.search.*;

/**
 * Dialog for creating or modifying groups. Operates directly on the Vector
 * containing group information.
 */
class GroupDialog extends JDialog {
    private static final int INDEX_KEYWORDGROUP = 0;
    private static final int INDEX_SEARCHGROUP = 1;
    private static final int INDEX_EXPLICITGROUP = 2;
    private static final int TEXTFIELD_LENGTH = 30;
    // for all types
    private JTextField m_name = new JTextField(TEXTFIELD_LENGTH);
    private JLabel m_nameLabel = new JLabel(Globals.lang("Group name") + ":");
    // for KeywordGroup
    private JTextField m_kgSearchExpression = new JTextField(TEXTFIELD_LENGTH);
    private JTextField m_searchField = new JTextField(TEXTFIELD_LENGTH);
    private JLabel m_keywordLabel = new JLabel(Globals.lang("Search term")
            + ":");
    private JLabel m_searchFieldLabel = new JLabel(Globals
            .lang("Field to search")
            + ":");
    private JPanel m_keywordGroupPanel;
    // for SearchGroup
    private JTextField m_sgSearchExpression = new JTextField(TEXTFIELD_LENGTH);
    private JCheckBox m_caseSensitive = new JCheckBox("Case sensitive");
    private JCheckBox m_isRegExp = new JCheckBox("Regular Expression");
    private JLabel m_searchExpressionLabel = new JLabel("Search expression:");
    private JPanel m_searchGroupPanel;
    private JLabel m_searchType = new JLabel("Plaintext Search");
    private JCheckBox m_searchAllFields = new JCheckBox("Search All Fields");
    private JCheckBox m_searchRequiredFields = new JCheckBox("Search Required Fields");
    private JCheckBox m_searchOptionalFields = new JCheckBox("Search Optional Fields");
    private JCheckBox m_searchGeneralFields = new JCheckBox("Search General Fields");
    private SearchExpressionParser m_parser;
    // JZTODO: translations...

    // for all types
    private DefaultComboBoxModel m_types = new DefaultComboBoxModel();
    private JLabel m_typeLabel = new JLabel("Assign entries based on:");
    private JComboBox m_typeSelector = new JComboBox();
    private JButton m_ok = new JButton(Globals.lang("Ok"));
    private JButton m_cancel = new JButton(Globals.lang("Cancel"));
    private JPanel m_mainPanel;

    private boolean m_okPressed = false;
    private final JabRefFrame m_parent;
    private final BasePanel m_basePanel;
    private AbstractGroup m_resultingGroup;
    private final AbstractGroup m_editedGroup;

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
    public GroupDialog(JabRefFrame jabrefFrame, BasePanel basePanel, AbstractGroup editedGroup) {
        super(jabrefFrame, Globals.lang("Edit group"), true);
        m_basePanel = basePanel;
        m_parent = jabrefFrame;
        m_editedGroup = editedGroup;

        // set default values (overwritten if editedGroup != null)
        m_searchField.setText(jabrefFrame.prefs().get("groupsDefaultField"));

        // configure elements
        m_types.addElement("Keywords");
        m_types.addElement("Search Expression");
        m_types.addElement("Explicit");
        m_typeSelector.setModel(m_types);

        // create layout
        m_mainPanel = new JPanelYBoxPreferredWidth();
        JPanel namePanel = new JPanelXBoxPreferredHeight();
        namePanel.add(m_nameLabel);
        namePanel.add(Box.createHorizontalGlue());
        namePanel.add(new JPanelXBoxPreferredSize(m_name));
        JPanel typePanel = new JPanelXBoxPreferredHeight();
        typePanel.add(m_typeLabel);
        typePanel.add(Box.createHorizontalGlue());
        typePanel.add(new JPanelXBoxPreferredSize(m_typeSelector));

        // ...for keyword group
        m_keywordGroupPanel = new JPanelYBox();
        JPanel kgField = new JPanelXBoxPreferredHeight();
        kgField.add(m_searchFieldLabel);
        kgField.add(Box.createHorizontalGlue());
        kgField.add(new JPanelXBoxPreferredSize(m_searchField));
        JPanel kgExpression = new JPanelXBoxPreferredHeight();
        kgExpression.add(m_keywordLabel);
        kgExpression.add(Box.createHorizontalGlue());
        kgExpression.add(new JPanelXBoxPreferredSize(m_kgSearchExpression));
        m_keywordGroupPanel.add(kgField);
        m_keywordGroupPanel.add(kgExpression);
        m_keywordGroupPanel.add(Box.createVerticalGlue());

        // ...for search group
        m_searchGroupPanel = new JPanelYBox();
        JPanel sgExpression = new JPanelXBoxPreferredHeight();
        sgExpression.add(m_searchExpressionLabel);
        sgExpression.add(Box.createHorizontalGlue());
        sgExpression.add(new JPanelXBoxPreferredSize(m_sgSearchExpression));
        JPanel sgSearchType = new JPanelXBoxPreferredHeight(m_searchType);
        sgSearchType.add(Box.createHorizontalGlue());
        JPanel sgCaseSensitive = new JPanelXBoxPreferredHeight(m_caseSensitive);
        JPanel sgRegExp = new JPanelXBoxPreferredHeight(m_isRegExp);
        JPanel sgAll = new JPanelXBoxPreferredHeight(m_searchAllFields);
        JPanel sgReq = new JPanelXBoxPreferredHeight(m_searchRequiredFields);
        JPanel sgOpt = new JPanelXBoxPreferredHeight(m_searchOptionalFields);
        JPanel sgGen = new JPanelXBoxPreferredHeight(m_searchGeneralFields);
        sgCaseSensitive.add(Box.createHorizontalGlue());
        sgRegExp.add(Box.createHorizontalGlue());
        sgAll.add(Box.createHorizontalGlue());
        sgReq.add(Box.createHorizontalGlue());
        sgOpt.add(Box.createHorizontalGlue());
        sgGen.add(Box.createHorizontalGlue());
        m_searchGroupPanel.add(sgExpression);
        m_searchGroupPanel.add(sgSearchType);
        m_searchGroupPanel.add(sgCaseSensitive);
        m_searchGroupPanel.add(sgRegExp);
        m_searchGroupPanel.add(sgAll);
        m_searchGroupPanel.add(sgReq);
        m_searchGroupPanel.add(sgOpt);
        m_searchGroupPanel.add(sgGen);
        m_searchGroupPanel.add(Box.createVerticalGlue());

        m_mainPanel.add(namePanel);
        m_mainPanel.add(typePanel);

        JPanel buttons = new JPanelXBoxPreferredHeight();
        buttons.add(m_ok);
        buttons.add(Box.createHorizontalStrut(5));
        buttons.add(m_cancel);

        Container cp = getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));

        cp.add(m_mainPanel);
        cp.add(Box.createVerticalGlue());
        cp.add(buttons);

        // add listeners
        m_typeSelector.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                setLayoutForGroup(m_typeSelector.getSelectedIndex());
                updateComponents();
            }
        });

        m_cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        m_ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                m_okPressed = true;
                switch (m_typeSelector.getSelectedIndex()) {
                case INDEX_EXPLICITGROUP:
                    if (m_editedGroup instanceof ExplicitGroup) {
                        // keep assignments from possible previous ExplicitGroup
                        m_resultingGroup = m_editedGroup.deepCopy();
                        m_resultingGroup.setName(m_name.getText().trim());
                    } else {
                        m_resultingGroup = new ExplicitGroup(m_name.getText()
                                .trim(),m_basePanel.database());
                    }
                    break;
                case INDEX_KEYWORDGROUP:
                    // regex is correct, otherwise OK would have been disabled
                    // therefore I don't catch anything here
                    m_resultingGroup = new KeywordGroup(
                            m_name.getText().trim(), m_searchField.getText()
                                    .trim(), m_kgSearchExpression.getText()
                                    .trim());
                    break;
                case INDEX_SEARCHGROUP:
                    try {
                        // regex is correct, otherwise OK would have been
                        // disabled
                        // therefore I don't catch anything here
                        m_resultingGroup = new SearchGroup(m_name.getText()
                                .trim(), m_sgSearchExpression.getText().trim(),
                                m_caseSensitive.isSelected(), m_isRegExp
                                        .isSelected(), m_searchAllFields
                                        .isSelected(), m_searchRequiredFields
                                        .isSelected(), m_searchOptionalFields
                                        .isSelected(), m_searchGeneralFields
                                        .isSelected());
                    } catch (Exception e1) {
                        // should never happen
                    }
                    break;
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
        m_searchField.addCaretListener(caretListener);
        m_kgSearchExpression.addCaretListener(caretListener);
        m_sgSearchExpression.addCaretListener(caretListener);
        m_isRegExp.addItemListener(itemListener);
        m_caseSensitive.addItemListener(itemListener);
        m_searchAllFields.addItemListener(itemListener);
        m_searchRequiredFields.addItemListener(itemListener);
        m_searchOptionalFields.addItemListener(itemListener);
        m_searchGeneralFields.addItemListener(itemListener);

        // configure for current type
        if (editedGroup instanceof KeywordGroup) {
            KeywordGroup group = (KeywordGroup) editedGroup;
            m_name.setText(group.getName());
            m_searchField.setText(group.getSearchField());
            m_kgSearchExpression.setText(group.getSearchExpression());
            m_typeSelector.setSelectedIndex(INDEX_KEYWORDGROUP);
        } else if (editedGroup instanceof SearchGroup) {
            SearchGroup group = (SearchGroup) editedGroup;
            m_name.setText(group.getName());
            m_sgSearchExpression.setText(group.getSearchExpression());
            m_caseSensitive.setSelected(group.isCaseSensitive());
            m_isRegExp.setSelected(group.isRegExp());
            m_searchAllFields.setSelected(group.searchAllFields());
            m_searchRequiredFields.setSelected(group.searchRequiredFields());
            m_searchOptionalFields.setSelected(group.searchOptionalFields());
            m_searchGeneralFields.setSelected(group.searchGeneralFields());
            m_typeSelector.setSelectedIndex(INDEX_SEARCHGROUP);
        } else if (editedGroup instanceof ExplicitGroup) {
            m_name.setText(editedGroup.getName());
            m_typeSelector.setSelectedIndex(INDEX_EXPLICITGROUP);
        }

        pack();
        setSize(350, 300);
        setResizable(false);

        updateComponents();
        setLayoutForGroup(m_typeSelector.getSelectedIndex());

        Util.placeDialog(this, m_parent);
    }

    public boolean okPressed() {
        return m_okPressed;
    }

    public AbstractGroup getResultingGroup() {
        return m_resultingGroup;
    }

    private void setLayoutForGroup(int index) {
        switch (index) {
        case INDEX_KEYWORDGROUP:
            m_mainPanel.remove(m_searchGroupPanel);
            m_mainPanel.add(m_keywordGroupPanel);
            validate();
            repaint();
            break;
        case INDEX_SEARCHGROUP:
            m_mainPanel.remove(m_keywordGroupPanel);
            m_mainPanel.add(m_searchGroupPanel);
            validate();
            repaint();
            break;
        case INDEX_EXPLICITGROUP:
            m_mainPanel.remove(m_searchGroupPanel);
            m_mainPanel.remove(m_keywordGroupPanel);
            validate();
            repaint();
            break;
        }
    }

    private void updateComponents() {
        // all groups need a name
        boolean okEnabled = m_name.getText().trim().length() > 0;
        String s;
        switch (m_typeSelector.getSelectedIndex()) {
        case INDEX_KEYWORDGROUP:
            s = m_searchField.getText().trim();
            okEnabled = okEnabled && s.length() > 0 && s.indexOf(' ') < 0;
            s = m_kgSearchExpression.getText().trim();
            okEnabled = okEnabled && s.length() > 0;
            try {
                Pattern.compile(s);
            } catch (Exception e) {
                okEnabled = false;
            }
            break;
        case INDEX_SEARCHGROUP:
            s = m_sgSearchExpression.getText().trim();
            okEnabled = okEnabled & s.length() > 0;
            m_parser = new SearchExpressionParser(new SearchExpressionLexer(
                    new StringReader(s)));
            m_parser.caseSensitive = m_caseSensitive.isSelected();
            m_parser.regex = m_isRegExp.isSelected();
            boolean advancedSearch = false;
            try {
                m_parser.searchExpression();
                advancedSearch = true;
            } catch (Exception e) {
                // advancedSearch remains false;
            }
            m_searchType.setText(advancedSearch ? "Advanced Search":"Plaintext Search");
            m_searchAllFields.setEnabled(!advancedSearch);
            m_searchRequiredFields.setEnabled(!advancedSearch && !m_searchAllFields.isSelected());
            m_searchOptionalFields.setEnabled(!advancedSearch && !m_searchAllFields.isSelected());
            m_searchGeneralFields.setEnabled(!advancedSearch && !m_searchAllFields.isSelected());
            validate();
            break;
        case INDEX_EXPLICITGROUP:
            break;
        }
        m_ok.setEnabled(okEnabled);
    }
}
