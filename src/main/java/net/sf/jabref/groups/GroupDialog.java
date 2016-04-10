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
package net.sf.jabref.groups;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.undo.AbstractUndoableEdit;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.groups.structure.*;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FieldContentSelector;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.fieldeditors.TextField;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.Util;

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
    private final JTextField m_name = new JTextField(GroupDialog.TEXTFIELD_LENGTH);
    private final JRadioButton m_explicitRadioButton = new JRadioButton(
            Localization.lang("Statically group entries by manual assignment"));
    private final JRadioButton m_keywordsRadioButton = new JRadioButton(
            Localization.lang("Dynamically group entries by searching a field for a keyword"));
    private final JRadioButton m_searchRadioButton = new JRadioButton(
            Localization.lang("Dynamically group entries by a free-form search expression"));
    private final JRadioButton m_independentButton = new JRadioButton(
            Localization.lang("Independent group: When selected, view only this group's entries"));
    private final JRadioButton m_intersectionButton = new JRadioButton(
            Localization.lang("Refine supergroup: When selected, view entries contained in both this group and its supergroup"));
    private final JRadioButton m_unionButton = new JRadioButton(
            Localization.lang("Include subgroups: When selected, view entries contained in this group or its subgroups"));
    // for KeywordGroup
    private final JTextField m_kgSearchField = new JTextField(GroupDialog.TEXTFIELD_LENGTH);
    private final TextField m_kgSearchTerm = new TextField("keywords", "", false);
    private final JCheckBox m_kgCaseSensitive = new JCheckBox(Localization.lang("Case sensitive"));
    private final JCheckBox m_kgRegExp = new JCheckBox(Localization.lang("regular expression"));
    // for SearchGroup
    private final JTextField m_sgSearchExpression = new JTextField(GroupDialog.TEXTFIELD_LENGTH);
    private final JCheckBox m_sgCaseSensitive = new JCheckBox(Localization.lang("Case sensitive"));
    private final JCheckBox m_sgRegExp = new JCheckBox(Localization.lang("regular expression"));
    // for all types
    private final JButton m_ok = new JButton(Localization.lang("OK"));
    private final JPanel m_optionsPanel = new JPanel();
    private final JLabel m_description = new JLabel() {

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            // width must be smaller than width of enclosing JScrollPane
            // to prevent a horizontal scroll bar
            d.width = 1;
            return d;
        }
    };

    private boolean mOkPressed;

    private final BasePanel m_basePanel;

    private AbstractGroup mResultingGroup;

    private AbstractUndoableEdit mUndoAddPreviousEntires;

    private final AbstractGroup m_editedGroup;

    private final CardLayout m_optionsLayout = new CardLayout();

    /**
     * Shows a group add/edit dialog.
     *
     * @param jabrefFrame The parent frame.
     * @param basePanel   The default grouping field.
     * @param editedGroup The group being edited, or null if a new group is to be
     *                    created.
     */
    public GroupDialog(JabRefFrame jabrefFrame, BasePanel basePanel,
            AbstractGroup editedGroup) {
        super(jabrefFrame, Localization.lang("Edit group"), true);
        m_basePanel = basePanel;
        m_editedGroup = editedGroup;

        // set default values (overwritten if editedGroup != null)
        m_kgSearchField.setText(jabrefFrame.prefs().get(JabRefPreferences.GROUPS_DEFAULT_FIELD));

        // configure elements
        ButtonGroup groupType = new ButtonGroup();
        groupType.add(m_explicitRadioButton);
        groupType.add(m_keywordsRadioButton);
        groupType.add(m_searchRadioButton);
        ButtonGroup groupHierarchy = new ButtonGroup();
        groupHierarchy.add(m_independentButton);
        groupHierarchy.add(m_intersectionButton);
        groupHierarchy.add(m_unionButton);
        m_description.setVerticalAlignment(SwingConstants.TOP);
        getRootPane().setDefaultButton(m_ok);

        // build individual layout cards for each group
        m_optionsPanel.setLayout(m_optionsLayout);
        // ... for explicit group
        m_optionsPanel.add(new JPanel(), String.valueOf(GroupDialog.INDEX_EXPLICITGROUP));
        // ... for keyword group
        FormLayout layoutKG = new FormLayout(
                "right:pref, 4dlu, fill:1dlu:grow, 2dlu, left:pref");
        DefaultFormBuilder builderKG = new DefaultFormBuilder(layoutKG);
        builderKG.append(Localization.lang("Field"));
        builderKG.append(m_kgSearchField, 3);
        builderKG.nextLine();
        builderKG.append(Localization.lang("Keyword"));
        builderKG.append(m_kgSearchTerm);
        builderKG.append(new FieldContentSelector(jabrefFrame, m_basePanel, this,
                m_kgSearchTerm, m_basePanel.getBibDatabaseContext().getMetaData(), null, true, ", "));
        builderKG.nextLine();
        builderKG.append(m_kgCaseSensitive, 3);
        builderKG.nextLine();
        builderKG.append(m_kgRegExp, 3);
        m_optionsPanel.add(builderKG.getPanel(), String.valueOf(GroupDialog.INDEX_KEYWORDGROUP));
        // ... for search group
        FormLayout layoutSG = new FormLayout("right:pref, 4dlu, fill:1dlu:grow");
        DefaultFormBuilder builderSG = new DefaultFormBuilder(layoutSG);
        builderSG.append(Localization.lang("Search expression"));
        builderSG.append(m_sgSearchExpression);
        builderSG.nextLine();
        builderSG.append(m_sgCaseSensitive, 3);
        builderSG.nextLine();
        builderSG.append(m_sgRegExp, 3);
        m_optionsPanel.add(builderSG.getPanel(), String.valueOf(GroupDialog.INDEX_SEARCHGROUP));
        // ... for buttons panel
        FormLayout layoutBP = new FormLayout("pref, 4dlu, pref", "p");
        layoutBP.setColumnGroups(new int[][] {{1, 3}});
        ButtonBarBuilder builderBP = new ButtonBarBuilder();
        builderBP.addGlue();
        builderBP.addButton(m_ok);
        JButton mCancel = new JButton(Localization.lang("Cancel"));
        builderBP.addButton(mCancel);
        builderBP.addGlue();
        builderBP.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // create layout
        FormLayout layoutAll = new FormLayout(
                "right:pref, 4dlu, fill:600px, 4dlu, fill:pref",
                "p, 3dlu, p, 3dlu, p, 0dlu, p, 0dlu, p, 3dlu, p, 3dlu, p, "
                        + "0dlu, p, 0dlu, p, 3dlu, p, 3dlu, "
                        + "p, 3dlu, p, 3dlu, top:80dlu, 9dlu, p, 9dlu, p");

        DefaultFormBuilder builderAll = new DefaultFormBuilder(layoutAll);
        builderAll.appendSeparator(Localization.lang("General"));
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(Localization.lang("Name"));
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
        builderAll.appendSeparator(Localization.lang("Hierarchical context"));
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
        builderAll.appendSeparator(Localization.lang("Options"));
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(m_optionsPanel, 5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.appendSeparator(Localization.lang("Description"));
        builderAll.nextLine();
        builderAll.nextLine();
        JScrollPane sp = new JScrollPane(m_description,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {

            @Override
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
        setLocationRelativeTo(jabrefFrame);

        // add listeners
        ItemListener radioButtonItemListener = new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                setLayoutForSelectedGroup();
                updateComponents();
            }
        };
        m_explicitRadioButton.addItemListener(radioButtonItemListener);
        m_keywordsRadioButton.addItemListener(radioButtonItemListener);
        m_searchRadioButton.addItemListener(radioButtonItemListener);

        Action cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        mCancel.addActionListener(cancelAction);
        builderAll.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        builderAll.getPanel().getActionMap().put("close", cancelAction);

        m_ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                mOkPressed = true;
                if (m_explicitRadioButton.isSelected()) {
                    if (m_editedGroup instanceof ExplicitGroup) {
                        // keep assignments from possible previous ExplicitGroup
                        mResultingGroup = m_editedGroup.deepCopy();
                        mResultingGroup.setName(m_name.getText().trim());
                        mResultingGroup.setHierarchicalContext(getContext());
                    } else {
                        mResultingGroup = new ExplicitGroup(m_name.getText()
                                .trim(), getContext());
                        if (m_editedGroup != null) {
                            addPreviousEntries();
                        }
                    }
                } else if (m_keywordsRadioButton.isSelected()) {
                    // regex is correct, otherwise OK would have been disabled
                    // therefore I don't catch anything here
                    mResultingGroup = new KeywordGroup(
                            m_name.getText().trim(), m_kgSearchField.getText()
                            .trim(), m_kgSearchTerm.getText().trim(),
                            m_kgCaseSensitive.isSelected(), m_kgRegExp
                            .isSelected(), getContext());
                    if (((m_editedGroup instanceof ExplicitGroup) || (m_editedGroup instanceof SearchGroup))
                            && mResultingGroup.supportsAdd()) {
                        addPreviousEntries();
                    }
                } else if (m_searchRadioButton.isSelected()) {
                    try {
                        // regex is correct, otherwise OK would have been
                        // disabled
                        // therefore I don't catch anything here
                        mResultingGroup = new SearchGroup(m_name.getText()
                                .trim(), m_sgSearchExpression.getText().trim(),
                                isCaseSensitive(), isRegex(), getContext());
                    } catch (Exception e1) {
                        // should never happen
                    }
                }
                dispose();
            }
        });

        CaretListener caretListener = new CaretListener() {

            @Override
            public void caretUpdate(CaretEvent e) {
                updateComponents();
            }
        };

        ItemListener itemListener = new ItemListener() {

            @Override
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
            setContext(GroupHierarchyType.INDEPENDENT);
        }
    }

    public boolean okPressed() {
        return mOkPressed;
    }

    public AbstractGroup getResultingGroup() {
        return mResultingGroup;
    }

    private void setLayoutForSelectedGroup() {
        if (m_explicitRadioButton.isSelected()) {
            m_optionsLayout.show(m_optionsPanel, String
                    .valueOf(GroupDialog.INDEX_EXPLICITGROUP));
        } else if (m_keywordsRadioButton.isSelected()) {
            m_optionsLayout.show(m_optionsPanel, String
                    .valueOf(GroupDialog.INDEX_KEYWORDGROUP));
        } else if (m_searchRadioButton.isSelected()) {
            m_optionsLayout.show(m_optionsPanel, String
                    .valueOf(GroupDialog.INDEX_SEARCHGROUP));
        }
    }

    private void updateComponents() {
        // all groups need a name
        boolean okEnabled = !m_name.getText().trim().isEmpty();
        if (!okEnabled) {
            setDescription(Localization.lang("Please enter a name for the group."));
            m_ok.setEnabled(false);
            return;
        }
        String s1;
        String s2;
        if (m_keywordsRadioButton.isSelected()) {
            s1 = m_kgSearchField.getText().trim();
            okEnabled = okEnabled && s1.matches("\\w+");
            s2 = m_kgSearchTerm.getText().trim();
            okEnabled = okEnabled && !s2.isEmpty();
            if (okEnabled) {
                if (m_kgRegExp.isSelected()) {
                    try {
                        Pattern.compile(s2);
                        setDescription(KeywordGroup.getDescriptionForPreview(s1, s2, m_kgCaseSensitive.isSelected(),
                                m_kgRegExp.isSelected()));
                    } catch (PatternSyntaxException e) {
                        okEnabled = false;
                        setDescription(formatRegExException(s2, e));
                    }
                } else {
                    setDescription(KeywordGroup.getDescriptionForPreview(s1, s2, m_kgCaseSensitive.isSelected(),
                            m_kgRegExp.isSelected()));
                }
            } else {
                setDescription(Localization.lang(
                        "Please enter the field to search (e.g. <b>keywords</b>) and the keyword to search it for (e.g. <b>electrical</b>)."));
            }
            setNameFontItalic(true);
        } else if (m_searchRadioButton.isSelected()) {
            s1 = m_sgSearchExpression.getText().trim();
            okEnabled = okEnabled & !s1.isEmpty();
            if (okEnabled) {
                setDescription(new SearchQuery(s1, isCaseSensitive(), isRegex()).getDescription());

                if (isRegex()) {
                    try {
                        Pattern.compile(s1);
                    } catch (PatternSyntaxException e) {
                        okEnabled = false;
                        setDescription(formatRegExException(s1, e));
                    }
                }
            } else {
                setDescription(Localization
                        .lang("Please enter a search term. For example, to search all fields for <b>Smith</b>, enter:<p>"
                                + "<tt>smith</tt><p>"
                                + "To search the field <b>Author</b> for <b>Smith</b> and the field <b>Title</b> for <b>electrical</b>, enter:<p>"
                                + "<tt>author=smith and title=electrical</tt>"));
            }
            setNameFontItalic(true);
        } else if (m_explicitRadioButton.isSelected()) {
            setDescription(ExplicitGroup.getDescriptionForPreview());
            setNameFontItalic(false);
        }
        m_ok.setEnabled(okEnabled);
    }

    private boolean isRegex() {
        return m_sgRegExp.isSelected();
    }

    private boolean isCaseSensitive() {
        return m_sgCaseSensitive.isSelected();
    }

    /**
     * This is used when a group is converted and the new group supports
     * explicit adding of entries: All entries that match the previous group are
     * added to the new group.
     */
    private void addPreviousEntries() {
        int i = JOptionPane.showConfirmDialog(m_basePanel.frame(),
                Localization.lang("Assign the original group's entries to this group?"),
                Localization.lang("Change of Grouping Method"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (i == JOptionPane.NO_OPTION) {
            return;
        }
        List<BibEntry> list = new ArrayList<>();
        for (BibEntry entry : m_basePanel.getDatabase().getEntries()) {
            if (m_editedGroup.contains(entry)) {
                list.add(entry);
            }
        }
        if (!list.isEmpty()) {
            if (!Util.warnAssignmentSideEffects(Arrays.asList(mResultingGroup), this)) {
                return;
            }
            // the undo information for a conversion to an ExplicitGroup is
            // contained completely in the UndoableModifyGroup object.
            if (!(mResultingGroup instanceof ExplicitGroup)) {
                mUndoAddPreviousEntires = mResultingGroup.add(list);
            }
        }
    }

    private void setDescription(String description) {
        m_description.setText("<html>" + description + "</html>");
    }

    private static String formatRegExException(String regExp, Exception e) {
        String[] sa = e.getMessage().split("\\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sa.length; ++i) {
            if (i > 0) {
                sb.append("<br>");
            }
            sb.append(StringUtil.quoteForHTML(sa[i]));
        }
        String s = Localization.lang(
                "The regular expression <b>%0</b> is invalid:",
                StringUtil.quoteForHTML(regExp))
                + "<p><tt>"
                + sb
                + "</tt>";
        if (!(e instanceof PatternSyntaxException)) {
            return s;
        }
        int lastNewline = s.lastIndexOf("<br>");
        int hat = s.lastIndexOf('^');
        if ((lastNewline >= 0) && (hat >= 0) && (hat > lastNewline)) {
            return s.substring(0, lastNewline + 4) + s.substring(lastNewline + 4).replace(" ", "&nbsp;");
        }
        return s;
    }

    /**
     * Returns an undo object for adding the edited group's entries to the new
     * group, or null if this did not occur.
     */
    public AbstractUndoableEdit getUndoForAddPreviousEntries() {
        return mUndoAddPreviousEntires;
    }

    /**
     * Sets the font of the name entry field.
     */
    private void setNameFontItalic(boolean italic) {
        Font f = m_name.getFont();
        if (f.isItalic() != italic) {
            f = f.deriveFont(italic ? Font.ITALIC : Font.PLAIN);
            m_name.setFont(f);
        }
    }

    /**
     * Returns the int representing the selected hierarchical group context.
     */
    private GroupHierarchyType getContext() {
        if (m_independentButton.isSelected()) {
            return GroupHierarchyType.INDEPENDENT;
        }
        if (m_intersectionButton.isSelected()) {
            return GroupHierarchyType.REFINING;
        }
        if (m_unionButton.isSelected()) {
            return GroupHierarchyType.INCLUDING;
        }
        return GroupHierarchyType.INDEPENDENT; // default
    }

    private void setContext(GroupHierarchyType context) {
        if (context == GroupHierarchyType.REFINING) {
            m_intersectionButton.setSelected(true);
        } else if (context == GroupHierarchyType.INCLUDING) {
            m_unionButton.setSelected(true);
        } else {
            m_independentButton.setSelected(true);
        }
    }

}
