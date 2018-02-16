package org.jabref.gui.groups;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.CaretListener;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.Dialog;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.fieldeditors.TextField;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.search.rules.describer.SearchDescribers;
import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.Keyword;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog for creating or modifying groups. Operates directly on the Vector
 * containing group information.
 */
class GroupDialog extends JabRefDialog implements Dialog<AbstractGroup> {

    private static final int INDEX_EXPLICIT_GROUP = 0;
    private static final int INDEX_KEYWORD_GROUP = 1;
    private static final int INDEX_SEARCH_GROUP = 2;
    private static final int INDEX_AUTO_GROUP = 3;
    private static final int INDEX_TEX_GROUP = 4;
    private static final int TEXTFIELD_LENGTH = 30;
    // for all types
    private final JTextField nameField = new JTextField(GroupDialog.TEXTFIELD_LENGTH);
    private final JRadioButton explicitRadioButton = new JRadioButton(
            Localization.lang("Statically group entries by manual assignment"));
    private final JRadioButton keywordsRadioButton = new JRadioButton(
            Localization.lang("Dynamically group entries by searching a field for a keyword"));
    private final JRadioButton searchRadioButton = new JRadioButton(
            Localization.lang("Dynamically group entries by a free-form search expression"));
    private final JRadioButton autoRadioButton = new JRadioButton(
            Localization.lang("Automatically create groups"));
    private final JRadioButton texRadioButton = new JRadioButton(
            Localization.lang("Group containing entries cited in a given TeX file"));
    private final JRadioButton independentButton = new JRadioButton(
            Localization.lang("Independent group: When selected, view only this group's entries"));
    private final JRadioButton intersectionButton = new JRadioButton(
            Localization.lang("Refine supergroup: When selected, view entries contained in both this group and its supergroup"));
    private final JRadioButton unionButton = new JRadioButton(
            Localization.lang("Include subgroups: When selected, view entries contained in this group or its subgroups"));
    private final JTextField colorField = new JTextField(GroupDialog.TEXTFIELD_LENGTH);
    private final JTextField descriptionField = new JTextField(GroupDialog.TEXTFIELD_LENGTH);
    private final JTextField iconField = new JTextField(GroupDialog.TEXTFIELD_LENGTH);

    // for KeywordGroup
    private final JTextField keywordGroupSearchField = new JTextField(GroupDialog.TEXTFIELD_LENGTH);
    private final TextField keywordGroupSearchTerm = new TextField(FieldName.KEYWORDS, "", false);
    private final JCheckBox keywordGroupCaseSensitive = new JCheckBox(Localization.lang("Case sensitive"));
    private final JCheckBox keywordGroupRegExp = new JCheckBox(Localization.lang("regular expression"));
    // for SearchGroup
    private final JTextField searchGroupSearchExpression = new JTextField(GroupDialog.TEXTFIELD_LENGTH);
    private final JCheckBox searchGroupCaseSensitive = new JCheckBox(Localization.lang("Case sensitive"));
    private final JCheckBox searchGroupRegExp = new JCheckBox(Localization.lang("regular expression"));
    // for AutoGroup
    private final JRadioButton autoGroupKeywordsOption = new JRadioButton(
            Localization.lang("Generate groups from keywords in a BibTeX field"));
    private final JTextField autoGroupKeywordsField = new JTextField(60);
    private final JTextField autoGroupKeywordsDeliminator = new JTextField(60);
    private final JTextField autoGroupKeywordsHierarchicalDeliminator = new JTextField(60);
    private final JRadioButton autoGroupPersonsOption = new JRadioButton(
            Localization.lang("Generate groups for author last names"));
    private final JTextField autoGroupPersonsField = new JTextField(60);
    // for TexGroup
    private final JTextField texGroupFilePath = new JTextField(60);

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
    private final CardLayout optionsLayout = new CardLayout();
    private boolean isOkPressed;
    private AbstractGroup resultingGroup;

    /**
     * Shows a group add/edit dialog.
     *
     * @param jabrefFrame The parent frame.
     * @param editedGroup The group being edited, or null if a new group is to be
     *                    created.
     */
    public GroupDialog(JabRefFrame jabrefFrame, AbstractGroup editedGroup) {
        super(jabrefFrame, (editedGroup == null) ? Localization.lang("Add group") : Localization.lang("Edit group"),
                true, GroupDialog.class);

        // set default values (overwritten if editedGroup != null)
        keywordGroupSearchField.setText(jabrefFrame.prefs().get(JabRefPreferences.GROUPS_DEFAULT_FIELD));

        // configure elements
        ButtonGroup groupType = new ButtonGroup();
        groupType.add(explicitRadioButton);
        groupType.add(keywordsRadioButton);
        groupType.add(searchRadioButton);
        groupType.add(autoRadioButton);
        groupType.add(texRadioButton);
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

        // for auto group
        ButtonGroup bg = new ButtonGroup();
        bg.add(autoGroupKeywordsOption);
        bg.add(autoGroupPersonsOption);

        FormLayout layoutAutoGroup = new FormLayout("left:20dlu, 4dlu, left:pref, 4dlu, fill:60dlu",
                "p, 2dlu, p, 2dlu, p, p, 2dlu, p, 2dlu, p");
        FormBuilder builderAutoGroup = FormBuilder.create();
        builderAutoGroup.layout(layoutAutoGroup);
        builderAutoGroup.add(autoGroupKeywordsOption).xyw(1, 1, 5);
        builderAutoGroup.add(Localization.lang("Field to group by") + ":").xy(3, 3);
        builderAutoGroup.add(autoGroupKeywordsField).xy(5, 3);
        builderAutoGroup.add(Localization.lang("Use the following delimiter character(s):")).xy(3, 5);
        builderAutoGroup.add(autoGroupKeywordsDeliminator).xy(5, 5);
        builderAutoGroup.add(autoGroupKeywordsHierarchicalDeliminator).xy(5, 6);
        builderAutoGroup.add(autoGroupPersonsOption).xyw(1, 8, 5);
        builderAutoGroup.add(Localization.lang("Field to group by") + ":").xy(3, 10);
        builderAutoGroup.add(autoGroupPersonsField).xy(5, 10);
        optionsPanel.add(builderAutoGroup.build(), String.valueOf(GroupDialog.INDEX_AUTO_GROUP));

        autoGroupKeywordsOption.setSelected(true);
        autoGroupKeywordsField.setText(Globals.prefs.get(JabRefPreferences.GROUPS_DEFAULT_FIELD));
        autoGroupKeywordsDeliminator.setText(Globals.prefs.get(JabRefPreferences.KEYWORD_SEPARATOR));
        autoGroupKeywordsHierarchicalDeliminator.setText(Keyword.DEFAULT_HIERARCHICAL_DELIMITER.toString());
        autoGroupPersonsField.setText(FieldName.AUTHOR);

        // ... for tex group
        FormLayout layoutTG = new FormLayout("right:pref, 4dlu, fill:1dlu:grow");
        DefaultFormBuilder builderTG = new DefaultFormBuilder(layoutTG);
        builderTG.append(Localization.lang("Aux file"));
        builderTG.append(texGroupFilePath);
        optionsPanel.add(builderTG.getPanel(), String.valueOf(GroupDialog.INDEX_TEX_GROUP));

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
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 0dlu, p, 0dlu, p, 0dlu, p, 0dlu, p, 3dlu, p, 3dlu, p, "
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
        builderAll.append(Localization.lang("Description"));
        builderAll.append(descriptionField);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(Localization.lang("Color"));
        builderAll.append(colorField);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(Localization.lang("Icon"));
        builderAll.append(iconField);
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
        builderAll.append(autoRadioButton, 5);
        builderAll.nextLine();
        builderAll.nextLine();
        builderAll.append(texRadioButton, 5);
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
        autoRadioButton.addItemListener(radioButtonItemListener);
        texRadioButton.addItemListener(radioButtonItemListener);

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
                String groupName = nameField.getText().trim();
                if (explicitRadioButton.isSelected()) {
                    Character keywordDelimiter = Globals.prefs.getKeywordDelimiter();
                    if (groupName.contains(Character.toString(keywordDelimiter))) {
                        jabrefFrame.showMessage(
                                Localization.lang("The group name contains the keyword separator \"%0\" and thus probably does not work as expected.", Character.toString(keywordDelimiter)));
                    }

                    Optional<GroupTreeNode> rootGroup = jabrefFrame.getCurrentBasePanel().getBibDatabaseContext().getMetaData().getGroups();
                    if (rootGroup.isPresent()) {
                        int groupsWithSameName = rootGroup.get().findChildrenSatisfying(group -> group.getName().equals(groupName)).size();
                        boolean warnAboutSameName = false;
                        if (editedGroup == null && groupsWithSameName > 0) {
                            // New group but there is already one group with the same name
                            warnAboutSameName = true;
                        }
                        if (editedGroup != null && !editedGroup.getName().equals(groupName) && groupsWithSameName > 0) {
                            // Edit group, changed name to something that is already present
                            warnAboutSameName = true;
                        }

                        if (warnAboutSameName) {
                            jabrefFrame.showMessage(
                                    Localization.lang("There exists already a group with the same name.", Character.toString(keywordDelimiter)));
                            return;
                        }
                    }

                    resultingGroup = new ExplicitGroup(groupName, getContext(),
                            keywordDelimiter);
                } else if (keywordsRadioButton.isSelected()) {
                    // regex is correct, otherwise OK would have been disabled
                    // therefore I don't catch anything here
                    if (keywordGroupRegExp.isSelected()) {
                        resultingGroup = new RegexKeywordGroup(groupName, getContext(),
                                keywordGroupSearchField.getText().trim(), keywordGroupSearchTerm.getText().trim(),
                                keywordGroupCaseSensitive.isSelected());
                    } else {
                        resultingGroup = new WordKeywordGroup(groupName, getContext(),
                                keywordGroupSearchField.getText().trim(), keywordGroupSearchTerm.getText().trim(),
                                keywordGroupCaseSensitive.isSelected(), Globals.prefs.getKeywordDelimiter(), false);
                    }
                } else if (searchRadioButton.isSelected()) {
                    try {
                        // regex is correct, otherwise OK would have been
                        // disabled
                        // therefore I don't catch anything here
                        resultingGroup = new SearchGroup(groupName, getContext(), searchGroupSearchExpression.getText().trim(),
                                isCaseSensitive(), isRegex());
                    } catch (Exception e1) {
                        // should never happen
                    }
                } else if (autoRadioButton.isSelected()) {
                    if (autoGroupKeywordsOption.isSelected()) {
                        resultingGroup = new AutomaticKeywordGroup(
                                groupName, getContext(),
                                autoGroupKeywordsField.getText().trim(),
                                autoGroupKeywordsDeliminator.getText().charAt(0),
                                autoGroupKeywordsHierarchicalDeliminator.getText().charAt(0));
                    } else {
                        resultingGroup = new AutomaticPersonsGroup(groupName, getContext(),
                                autoGroupPersonsField.getText().trim());
                    }
                } else if (texRadioButton.isSelected()) {
                    resultingGroup = new TexGroup(groupName, getContext(),
                            Paths.get(texGroupFilePath.getText().trim()), new DefaultAuxParser(new BibDatabase()), Globals.getFileUpdateMonitor());
                }
                try {
                    resultingGroup.setColor(Color.valueOf(colorField.getText()));
                } catch (IllegalArgumentException ex) {
                    // Ignore invalid color (we should probably notify the user instead...)
                }
                resultingGroup.setDescription(descriptionField.getText());
                resultingGroup.setIconName(iconField.getText());

                dispose();
            } catch (IllegalArgumentException | IOException exception) {
                jabrefFrame.showMessage(exception.getLocalizedMessage());
            }
        });

        CaretListener caretListener = e -> updateComponents();
        ItemListener itemListener = e -> updateComponents();

        nameField.addCaretListener(caretListener);
        colorField.addCaretListener(caretListener);
        descriptionField.addCaretListener(caretListener);
        iconField.addCaretListener(caretListener);
        keywordGroupSearchField.addCaretListener(caretListener);
        keywordGroupSearchTerm.addCaretListener(caretListener);
        keywordGroupCaseSensitive.addItemListener(itemListener);
        keywordGroupRegExp.addItemListener(itemListener);
        searchGroupSearchExpression.addCaretListener(caretListener);
        searchGroupRegExp.addItemListener(itemListener);
        searchGroupCaseSensitive.addItemListener(itemListener);

        // configure for current type
        if (editedGroup == null) {
            // creating new group -> defaults!
            explicitRadioButton.setSelected(true);
            setContext(GroupHierarchyType.INDEPENDENT);
        } else {
            nameField.setText(editedGroup.getName());
            colorField.setText(editedGroup.getColor().map(Color::toString).orElse(""));
            descriptionField.setText(editedGroup.getDescription().orElse(""));
            iconField.setText(editedGroup.getIconName().orElse(""));

            if (editedGroup.getClass() == WordKeywordGroup.class) {
                WordKeywordGroup group = (WordKeywordGroup) editedGroup;
                keywordGroupSearchField.setText(group.getSearchField());
                keywordGroupSearchTerm.setText(group.getSearchExpression());
                keywordGroupCaseSensitive.setSelected(group.isCaseSensitive());
                keywordGroupRegExp.setSelected(false);
                keywordsRadioButton.setSelected(true);
                setContext(editedGroup.getHierarchicalContext());
            } else if (editedGroup.getClass() == RegexKeywordGroup.class) {
                RegexKeywordGroup group = (RegexKeywordGroup) editedGroup;
                keywordGroupSearchField.setText(group.getSearchField());
                keywordGroupSearchTerm.setText(group.getSearchExpression());
                keywordGroupCaseSensitive.setSelected(group.isCaseSensitive());
                keywordGroupRegExp.setSelected(true);
                keywordsRadioButton.setSelected(true);
                setContext(editedGroup.getHierarchicalContext());
            } else if (editedGroup.getClass() == SearchGroup.class) {
                SearchGroup group = (SearchGroup) editedGroup;
                searchGroupSearchExpression.setText(group.getSearchExpression());
                searchGroupCaseSensitive.setSelected(group.isCaseSensitive());
                searchGroupRegExp.setSelected(group.isRegularExpression());
                searchRadioButton.setSelected(true);
                setContext(editedGroup.getHierarchicalContext());
            } else if (editedGroup.getClass() == ExplicitGroup.class) {
                explicitRadioButton.setSelected(true);
                setContext(editedGroup.getHierarchicalContext());
            } else if (editedGroup.getClass() == AutomaticKeywordGroup.class) {
                autoRadioButton.setSelected(true);
                setContext(editedGroup.getHierarchicalContext());

                if (editedGroup.getClass() == AutomaticKeywordGroup.class) {
                    AutomaticKeywordGroup group = (AutomaticKeywordGroup) editedGroup;
                    autoGroupKeywordsDeliminator.setText(group.getKeywordDelimiter().toString());
                    autoGroupKeywordsHierarchicalDeliminator.setText(group.getKeywordHierarchicalDelimiter().toString());
                    autoGroupKeywordsField.setText(group.getField());
                } else if (editedGroup.getClass() == AutomaticPersonsGroup.class) {
                    AutomaticPersonsGroup group = (AutomaticPersonsGroup) editedGroup;
                    autoGroupPersonsField.setText(group.getField());
                }
            } else if (editedGroup.getClass() == TexGroup.class) {
                texRadioButton.setSelected(true);
                setContext(editedGroup.getHierarchicalContext());

                TexGroup group = (TexGroup) editedGroup;
                texGroupFilePath.setText(group.getFilePath().toString());
            }
        }
    }

    public GroupDialog() {
        this(JabRefGUI.getMainFrame(), null);
    }

    public GroupDialog(AbstractGroup editedGroup) {
        this(JabRefGUI.getMainFrame(), editedGroup);
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
        } else if (autoRadioButton.isSelected()) {
            optionsLayout.show(optionsPanel, String.valueOf(GroupDialog.INDEX_AUTO_GROUP));
        } else if (texRadioButton.isSelected()) {
            optionsLayout.show(optionsPanel, String.valueOf(GroupDialog.INDEX_TEX_GROUP));
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
                setDescription(fromTextFlowToHTMLString(SearchDescribers.getSearchDescriberFor(
                        new SearchQuery(s1, isCaseSensitive(), isRegex())).getDescription()));

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

    private String fromTextFlowToHTMLString(TextFlow textFlow) {
        StringBuilder htmlStringBuilder = new StringBuilder();
        for (Node node : textFlow.getChildren()) {
            if (node instanceof Text)
                htmlStringBuilder.append(TooltipTextUtil.textToHTMLString((Text) node));
        }
        return htmlStringBuilder.toString();
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

    @Override
    public Optional<AbstractGroup> showAndWait() {
        this.setVisible(true);
        if (this.okPressed()) {
            AbstractGroup newGroup = getResultingGroup();
            return Optional.of(newGroup);
        } else {
            return Optional.empty();
        }
    }
}
