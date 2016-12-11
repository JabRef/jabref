package net.sf.jabref.gui.groups;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.CaretListener;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.fieldeditors.TextField;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.groups.GroupDescriptions;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.groups.AbstractGroup;
import net.sf.jabref.model.groups.ExplicitGroup;
import net.sf.jabref.model.groups.GroupHierarchyType;
import net.sf.jabref.model.groups.KeywordGroup;
import net.sf.jabref.model.groups.SearchGroup;
import net.sf.jabref.model.strings.StringUtil;
import net.sf.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog for creating or modifying groups. Operates directly on the Vector
 * containing group information.
 */
class GroupDialog extends JDialog {

    private static final int INDEX_EXPLICIT_GROUP = 0;
    private static final int INDEX_KEYWORD_GROUP = 1;
    private static final int INDEX_SEARCH_GROUP = 2;
    private static final int TEXTFIELD_LENGTH = 30;
    // for all types
    private final JTextField nameField = new JTextField(GroupDialog.TEXTFIELD_LENGTH);
    private final JRadioButton explicitRadioButton = new JRadioButton(
            Localization.lang("Statically group entries by manual assignment"));
    private final JRadioButton keywordsRadioButton = new JRadioButton(
            Localization.lang("Dynamically group entries by searching a field for a keyword"));
    private final JRadioButton searchRadioButton = new JRadioButton(
            Localization.lang("Dynamically group entries by a free-form search expression"));
    private final JRadioButton independentButton = new JRadioButton(
            Localization.lang("Independent group: When selected, view only this group's entries"));
    private final JRadioButton intersectionButton = new JRadioButton(
            Localization.lang("Refine supergroup: When selected, view entries contained in both this group and its supergroup"));
    private final JRadioButton unionButton = new JRadioButton(
            Localization.lang("Include subgroups: When selected, view entries contained in this group or its subgroups"));
    // for KeywordGroup
    private final JTextField keywordGroupSearchField = new JTextField(GroupDialog.TEXTFIELD_LENGTH);
    private final TextField keywordGroupSearchTerm = new TextField(FieldName.KEYWORDS, "", false);
    private final JCheckBox keywordGroupCaseSensitive = new JCheckBox(Localization.lang("Case sensitive"));
    private final JCheckBox keywordGroupRegExp = new JCheckBox(Localization.lang("regular expression"));
    // for SearchGroup
    private final JTextField searchGroupSearchExpression = new JTextField(GroupDialog.TEXTFIELD_LENGTH);
    private final JCheckBox searchGroupCaseSensitive = new JCheckBox(Localization.lang("Case sensitive"));
    private final JCheckBox searchGroupRegExp = new JCheckBox(Localization.lang("regular expression"));
    // for all types
    private final JButton okButton = new JButton(Localization.lang("OK"));
    private final JPanel optionsPanel = new JPanel();
    private final JLabel descriptionLabel = new JLabel() {

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            // width must be smaller than width of enclosing JScrollPane
            // to prevent a horizontal scroll bar
            d.width = 1;
            return d;
        }
    };

    private boolean isOkPressed;

    private AbstractGroup resultingGroup;

    private final CardLayout optionsLayout = new CardLayout();

    /**
     * Shows a group add/edit dialog.
     *
     * @param jabrefFrame The parent frame.
     * @param basePanel   The default grouping field.
     * @param editedGroup The group being edited, or null if a new group is to be
     *                    created.
     */
    public GroupDialog(JabRefFrame jabrefFrame,
            AbstractGroup editedGroup) {
        super(jabrefFrame, Localization.lang("Edit group"), true);

        // set default values (overwritten if editedGroup != null)
        keywordGroupSearchField.setText(jabrefFrame.prefs().get(JabRefPreferences.GROUPS_DEFAULT_FIELD));

        // configure elements
        ButtonGroup groupType = new ButtonGroup();
        groupType.add(explicitRadioButton);
        groupType.add(keywordsRadioButton);
        groupType.add(searchRadioButton);
        ButtonGroup groupHierarchy = new ButtonGroup();
        groupHierarchy.add(independentButton);
        groupHierarchy.add(intersectionButton);
        groupHierarchy.add(unionButton);
        descriptionLabel.setVerticalAlignment(SwingConstants.TOP);
        getRootPane().setDefaultButton(okButton);

        // build individual layout cards for each group
        optionsPanel.setLayout(optionsLayout);
        // ... for explicit group
        optionsPanel.add(new JPanel(), String.valueOf(GroupDialog.INDEX_EXPLICIT_GROUP));
        // ... for keyword group
        FormLayout layoutKG = new FormLayout(
                "right:pref, 4dlu, fill:1dlu:grow, 2dlu, left:pref");
        DefaultFormBuilder builderKG = new DefaultFormBuilder(layoutKG);
        builderKG.append(Localization.lang("Field"));
        builderKG.append(keywordGroupSearchField, 3);
        builderKG.nextLine();
        builderKG.append(Localization.lang("Keyword"));
        builderKG.append(keywordGroupSearchTerm);
        builderKG.nextLine();
        builderKG.append(keywordGroupCaseSensitive, 3);
        builderKG.nextLine();
        builderKG.append(keywordGroupRegExp, 3);
        optionsPanel.add(builderKG.getPanel(), String.valueOf(GroupDialog.INDEX_KEYWORD_GROUP));
        // ... for search group
        FormLayout layoutSG = new FormLayout("right:pref, 4dlu, fill:1dlu:grow");
        DefaultFormBuilder builderSG = new DefaultFormBuilder(layoutSG);
        builderSG.append(Localization.lang("Search expression"));
        builderSG.append(searchGroupSearchExpression);
        builderSG.nextLine();
        builderSG.append(searchGroupCaseSensitive, 3);
        builderSG.nextLine();
        builderSG.append(searchGroupRegExp, 3);
        optionsPanel.add(builderSG.getPanel(), String.valueOf(GroupDialog.INDEX_SEARCH_GROUP));
        // ... for buttons panel
        FormLayout layoutBP = new FormLayout("pref, 4dlu, pref", "p");
        layoutBP.setColumnGroups(new int[][] {{1, 3}});
        ButtonBarBuilder builderBP = new ButtonBarBuilder();
        builderBP.addGlue();
        builderBP.addButton(okButton);
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
        builderAll.append(nameField);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(explicitRadioButton, 5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(keywordsRadioButton, 5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(searchRadioButton, 5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.appendSeparator(Localization.lang("Hierarchical context"));
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(independentButton, 5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(intersectionButton, 5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(unionButton, 5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.appendSeparator(Localization.lang("Options"));
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(optionsPanel, 5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.appendSeparator(Localization.lang("Description"));
        builderAll.nextLine();
        builderAll.nextLine();
        JScrollPane sp = new JScrollPane(descriptionLabel,
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

        Container cp = getContentPane();
        cp.add(builderAll.getPanel(), BorderLayout.CENTER);
        cp.add(builderBP.getPanel(), BorderLayout.SOUTH);
        pack();
        setResizable(false);
        updateComponents();
        setLayoutForSelectedGroup();
        setLocationRelativeTo(jabrefFrame);

        // add listeners
        ItemListener radioButtonItemListener = e -> {
            setLayoutForSelectedGroup();
            updateComponents();
        };
        explicitRadioButton.addItemListener(radioButtonItemListener);
        keywordsRadioButton.addItemListener(radioButtonItemListener);
        searchRadioButton.addItemListener(radioButtonItemListener);

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

        okButton.addActionListener(e -> {
                isOkPressed = true;
            try {
                if (explicitRadioButton.isSelected()) {
                    resultingGroup = new ExplicitGroup(nameField.getText().trim(), getContext(),
                            Globals.prefs.getKeywordDelimiter());
                } else if (keywordsRadioButton.isSelected()) {
                    // regex is correct, otherwise OK would have been disabled
                    // therefore I don't catch anything here
                    resultingGroup = new KeywordGroup(nameField.getText().trim(),
                            keywordGroupSearchField.getText().trim(), keywordGroupSearchTerm.getText().trim(),
                            keywordGroupCaseSensitive.isSelected(), keywordGroupRegExp.isSelected(), getContext(),
                            Globals.prefs.getKeywordDelimiter());
                } else if (searchRadioButton.isSelected()) {
                    try {
                        // regex is correct, otherwise OK would have been
                        // disabled
                        // therefore I don't catch anything here
                        resultingGroup = new SearchGroup(nameField.getText().trim(), searchGroupSearchExpression.getText().trim(),
                                isCaseSensitive(), isRegex(), getContext());
                    } catch (Exception e1) {
                        // should never happen
                    }
                }
                dispose();
            } catch (IllegalArgumentException exception) {
                jabrefFrame.showMessage(exception.getLocalizedMessage());
            }
        });

        CaretListener caretListener = e -> updateComponents();
        ItemListener itemListener = e -> updateComponents();

        nameField.addCaretListener(caretListener);
        keywordGroupSearchField.addCaretListener(caretListener);
        keywordGroupSearchTerm.addCaretListener(caretListener);
        keywordGroupCaseSensitive.addItemListener(itemListener);
        keywordGroupRegExp.addItemListener(itemListener);
        searchGroupSearchExpression.addCaretListener(caretListener);
        searchGroupRegExp.addItemListener(itemListener);
        searchGroupCaseSensitive.addItemListener(itemListener);

        // configure for current type
        if ((editedGroup != null) && (editedGroup.getClass() == KeywordGroup.class)) {
            KeywordGroup group = (KeywordGroup) editedGroup;
            nameField.setText(group.getName());
            keywordGroupSearchField.setText(group.getSearchField());
            keywordGroupSearchTerm.setText(group.getSearchExpression());
            keywordGroupCaseSensitive.setSelected(group.isCaseSensitive());
            keywordGroupRegExp.setSelected(group.isRegExp());
            keywordsRadioButton.setSelected(true);
            setContext(editedGroup.getHierarchicalContext());
        } else if ((editedGroup != null) && (editedGroup.getClass() == SearchGroup.class)) {
            SearchGroup group = (SearchGroup) editedGroup;
            nameField.setText(group.getName());
            searchGroupSearchExpression.setText(group.getSearchExpression());
            searchGroupCaseSensitive.setSelected(group.isCaseSensitive());
            searchGroupRegExp.setSelected(group.isRegExp());
            searchRadioButton.setSelected(true);
            setContext(editedGroup.getHierarchicalContext());
        } else if ((editedGroup != null) && (editedGroup.getClass() == ExplicitGroup.class)) {
            nameField.setText(editedGroup.getName());
            explicitRadioButton.setSelected(true);
            setContext(editedGroup.getHierarchicalContext());
        } else { // creating new group -> defaults!
            explicitRadioButton.setSelected(true);
            setContext(GroupHierarchyType.INDEPENDENT);
        }
    }

    public boolean okPressed() {
        return isOkPressed;
    }

    public AbstractGroup getResultingGroup() {
        return resultingGroup;
    }

    private void setLayoutForSelectedGroup() {
        if (explicitRadioButton.isSelected()) {
            optionsLayout.show(optionsPanel, String.valueOf(GroupDialog.INDEX_EXPLICIT_GROUP));
        } else if (keywordsRadioButton.isSelected()) {
            optionsLayout.show(optionsPanel, String.valueOf(GroupDialog.INDEX_KEYWORD_GROUP));
        } else if (searchRadioButton.isSelected()) {
            optionsLayout.show(optionsPanel, String.valueOf(GroupDialog.INDEX_SEARCH_GROUP));
        }
    }

    private void updateComponents() {
        // all groups need a name
        boolean okEnabled = !nameField.getText().trim().isEmpty();
        if (!okEnabled) {
            setDescription(Localization.lang("Please enter a name for the group."));
            okButton.setEnabled(false);
            return;
        }
        String s1;
        String s2;
        if (keywordsRadioButton.isSelected()) {
            s1 = keywordGroupSearchField.getText().trim();
            okEnabled = okEnabled && s1.matches("\\w+");
            s2 = keywordGroupSearchTerm.getText().trim();
            okEnabled = okEnabled && !s2.isEmpty();
            if (okEnabled) {
                if (keywordGroupRegExp.isSelected()) {
                    try {
                        Pattern.compile(s2);
                        setDescription(GroupDescriptions.getDescriptionForPreview(s1, s2, keywordGroupCaseSensitive.isSelected(),
                                keywordGroupRegExp.isSelected()));
                    } catch (PatternSyntaxException e) {
                        okEnabled = false;
                        setDescription(formatRegExException(s2, e));
                    }
                } else {
                    setDescription(GroupDescriptions.getDescriptionForPreview(s1, s2, keywordGroupCaseSensitive.isSelected(),
                            keywordGroupRegExp.isSelected()));
                }
            } else {
                setDescription(Localization.lang(
                        "Please enter the field to search (e.g. <b>keywords</b>) and the keyword to search it for (e.g. <b>electrical</b>)."));
            }
            setNameFontItalic(true);
        } else if (searchRadioButton.isSelected()) {
            s1 = searchGroupSearchExpression.getText().trim();
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
        } else if (explicitRadioButton.isSelected()) {
            setDescription(GroupDescriptions.getDescriptionForPreview());
            setNameFontItalic(false);
        }
        okButton.setEnabled(okEnabled);
    }

    private boolean isRegex() {
        return searchGroupRegExp.isSelected();
    }

    private boolean isCaseSensitive() {
        return searchGroupCaseSensitive.isSelected();
    }

    private void setDescription(String description) {
        descriptionLabel.setText("<html>" + description + "</html>");
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
     * Sets the font of the name entry field.
     */
    private void setNameFontItalic(boolean italic) {
        Font f = nameField.getFont();
        if (f.isItalic() != italic) {
            f = f.deriveFont(italic ? Font.ITALIC : Font.PLAIN);
            nameField.setFont(f);
        }
    }

    /**
     * Returns the int representing the selected hierarchical group context.
     */
    private GroupHierarchyType getContext() {
        if (independentButton.isSelected()) {
            return GroupHierarchyType.INDEPENDENT;
        }
        if (intersectionButton.isSelected()) {
            return GroupHierarchyType.REFINING;
        }
        if (unionButton.isSelected()) {
            return GroupHierarchyType.INCLUDING;
        }
        return GroupHierarchyType.INDEPENDENT; // default
    }

    private void setContext(GroupHierarchyType context) {
        if (context == GroupHierarchyType.REFINING) {
            intersectionButton.setSelected(true);
        } else if (context == GroupHierarchyType.INCLUDING) {
            unionButton.setSelected(true);
        } else {
            independentButton.setSelected(true);
        }
    }
}
