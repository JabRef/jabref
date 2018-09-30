package org.jabref.gui.groups;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.search.rules.describer.SearchDescribers;
import org.jabref.gui.util.BaseDialog;
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

/**
 * Dialog for creating or modifying groups. Operates directly on the Vector
 * containing group information.
 */
class GroupDialog extends BaseDialog<AbstractGroup> {

    private static final int INDEX_EXPLICIT_GROUP = 0;
    private static final int INDEX_KEYWORD_GROUP = 1;
    private static final int INDEX_SEARCH_GROUP = 2;
    private static final int INDEX_AUTO_GROUP = 3;
    private static final int INDEX_TEX_GROUP = 4;
    private static final int TEXTFIELD_LENGTH = 40;
    private static final int HGAP = 7;
    private static final int VGAP = 5;
    private static final Insets PADDING = new Insets(5, 5, 5, 5);

    // for all types
    private final TextField nameField = new TextField();
    private final TextField descriptionField = new TextField();
    private final TextField colorField = new TextField();
    private final TextField iconField = new TextField();
    private final RadioButton explicitRadioButton = new RadioButton(Localization.lang("Statically group entries by manual assignment"));
    private final RadioButton keywordsRadioButton = new RadioButton(Localization.lang("Dynamically group entries by searching a field for a keyword"));
    private final RadioButton searchRadioButton = new RadioButton(Localization.lang("Dynamically group entries by a free-form search expression"));
    private final RadioButton autoRadioButton = new RadioButton(Localization.lang("Automatically create groups"));
    private final RadioButton texRadioButton = new RadioButton(Localization.lang("Group containing entries cited in a given TeX file"));
    private final RadioButton independentButton = new RadioButton(Localization.lang("Independent group: When selected, view only this group's entries"));
    private final RadioButton intersectionButton = new RadioButton(Localization.lang("Refine supergroup: When selected, view entries contained in both this group and its supergroup"));
    private final RadioButton unionButton = new RadioButton(Localization.lang("Include subgroups: When selected, view entries contained in this group or its subgroups"));

    // for KeywordGroup
    private final TextField keywordGroupSearchTerm = new TextField();
    private final TextField keywordGroupSearchField = new TextField();
    private final CheckBox keywordGroupCaseSensitive = new CheckBox(Localization.lang("Case sensitive"));
    private final CheckBox keywordGroupRegExp = new CheckBox(Localization.lang("regular expression"));

    // for SearchGroup
    private final TextField searchGroupSearchExpression = new TextField();
    private final CheckBox searchGroupCaseSensitive = new CheckBox(Localization.lang("Case sensitive"));
    private final CheckBox searchGroupRegExp = new CheckBox(Localization.lang("regular expression"));

    // for AutoGroup
    private final RadioButton autoGroupKeywordsOption = new RadioButton(Localization.lang("Generate groups from keywords in a BibTeX field"));
    private final TextField autoGroupKeywordsField = new TextField();
    private final TextField autoGroupKeywordsDeliminator = new TextField();
    private final TextField autoGroupKeywordsHierarchicalDeliminator = new TextField();
    private final RadioButton autoGroupPersonsOption = new RadioButton(Localization.lang("Generate groups for author last names"));
    private final TextField autoGroupPersonsField = new TextField();

    // for TexGroup
    private final TextField texGroupFilePath = new TextField();

    // for all types
    private final WebView descriptionWebView = new WebView();
    private final StackPane optionsPanel = new StackPane();


    /**
     * Shows a group add/edit dialog.
     *
     * @param jabrefFrame The parent frame.
     * @param editedGroup The group being edited, or null if a new group is to be
     *                    created.
     */
    public GroupDialog(JabRefFrame jabrefFrame, AbstractGroup editedGroup) {
        this.setTitle(Localization.lang("Edit group"));

        nameField.setPrefColumnCount(GroupDialog.TEXTFIELD_LENGTH);
        descriptionField.setPrefColumnCount(GroupDialog.TEXTFIELD_LENGTH);
        colorField.setPrefColumnCount(GroupDialog.TEXTFIELD_LENGTH);
        iconField.setPrefColumnCount(GroupDialog.TEXTFIELD_LENGTH);
        explicitRadioButton.setSelected(true);
        keywordGroupSearchTerm.setPrefColumnCount(GroupDialog.TEXTFIELD_LENGTH);
        keywordGroupSearchField.setPrefColumnCount(GroupDialog.TEXTFIELD_LENGTH);
        searchGroupSearchExpression.setPrefColumnCount(GroupDialog.TEXTFIELD_LENGTH);
        autoGroupKeywordsField.setPrefColumnCount(10);
        autoGroupKeywordsDeliminator.setPrefColumnCount(10);
        autoGroupKeywordsDeliminator.setPrefColumnCount(10);
        autoGroupPersonsField.setPrefColumnCount(10);
        texGroupFilePath.setPrefColumnCount(TEXTFIELD_LENGTH);
        descriptionWebView.setPrefWidth(585);
        optionsPanel.setPadding(PADDING);
        optionsPanel.setStyle("-fx-content-display:top;"
                              + "-fx-border-insets:0 0 0 0;"
                              + "-fx-border-color:#D3D3D3");

        // set default values (overwritten if editedGroup != null)
        keywordGroupSearchField.setText(jabrefFrame.prefs().get(JabRefPreferences.GROUPS_DEFAULT_FIELD));

        // configure elements
        ToggleGroup groupType = new ToggleGroup();
        explicitRadioButton.setToggleGroup(groupType);
        keywordsRadioButton.setToggleGroup(groupType);
        searchRadioButton.setToggleGroup(groupType);
        autoRadioButton.setToggleGroup(groupType);
        texRadioButton.setToggleGroup(groupType);
        ToggleGroup groupHierarchy = new ToggleGroup();
        independentButton.setToggleGroup(groupHierarchy);
        intersectionButton.setToggleGroup(groupHierarchy);
        unionButton.setToggleGroup(groupHierarchy);

        // build individual layout cards for each group
        GridPane explicitPanel = new GridPane();
        GridPane keywordPanel = new GridPane();
        GridPane searchPanel = new GridPane();
        GridPane autoPanel = new GridPane();
        GridPane texPanel = new GridPane();
        // ... for explicit group
        optionsPanel.getChildren().add(explicitPanel);
        explicitPanel.setVisible(true);
        // ... for keyword group
        optionsPanel.getChildren().add(keywordPanel);
        keywordPanel.setVisible(false);
        keywordPanel.setHgap(HGAP);
        keywordPanel.setVgap(VGAP);
        keywordPanel.setPadding(PADDING);
        ColumnConstraints keywordPanelLCol = new ColumnConstraints();
        keywordPanelLCol.setHalignment(HPos.RIGHT);
        keywordPanel.getColumnConstraints().add(keywordPanelLCol);
        ColumnConstraints keywordPanelRCol = new ColumnConstraints();
        keywordPanelRCol.setHalignment(HPos.LEFT);
        keywordPanel.getColumnConstraints().add(keywordPanelRCol);
        keywordPanel.add(new Label(Localization.lang("Field")), 0, 0);
        keywordPanel.add(keywordGroupSearchField, 1, 0);
        keywordPanel.add(new Label(Localization.lang("Keyword")), 0, 1);
        keywordPanel.add(keywordGroupSearchTerm, 1, 1);
        GridPane.setHalignment(keywordGroupCaseSensitive, HPos.LEFT);
        keywordPanel.add(keywordGroupCaseSensitive, 0, 2, 2, 1);
        GridPane.setHalignment(keywordGroupRegExp, HPos.LEFT);
        keywordPanel.add(keywordGroupRegExp, 0, 3, 2, 1);
        // ... for search group
        optionsPanel.getChildren().add(searchPanel);
        searchPanel.setVisible(false);
        searchPanel.setHgap(HGAP);
        searchPanel.setVgap(VGAP);
        searchPanel.setPadding(PADDING);
        searchPanel.add(new Label(Localization.lang("Search expression")), 0, 0);
        searchPanel.add(searchGroupSearchExpression, 1, 0, 2, 1);
        searchPanel.add(searchGroupCaseSensitive, 0, 1, 2, 1);
        searchPanel.add(searchGroupRegExp, 0, 2, 2, 1);
        // ... for auto group
        optionsPanel.getChildren().add(autoPanel);
        autoPanel.setVisible(false);
        autoPanel.setHgap(HGAP);
        autoPanel.setVgap(VGAP);
        autoPanel.setPadding(PADDING);
        ToggleGroup tg = new ToggleGroup();
        autoGroupKeywordsOption.setToggleGroup(tg);
        autoGroupPersonsOption.setToggleGroup(tg);
        Label placeholderLabel = new Label();
        placeholderLabel.setPrefWidth(30);
        autoPanel.add(autoGroupKeywordsOption, 0, 0, 3, 1);
        autoPanel.add(placeholderLabel, 0, 1);
        autoPanel.add(new Label(Localization.lang("Field to group by") + ":"), 1, 1);
        autoPanel.add(autoGroupKeywordsField, 2, 1);
        autoPanel.add(new Label(Localization.lang("Use the following delimiter character(s):")), 1, 2);
        autoPanel.add(autoGroupKeywordsDeliminator, 2, 2);
        autoPanel.add(autoGroupKeywordsHierarchicalDeliminator, 2, 3);
        autoPanel.add(autoGroupPersonsOption, 0, 4, 3, 1);
        autoPanel.add(new Label(Localization.lang("Field to group by") + ":"), 1, 5);
        autoPanel.add(autoGroupPersonsField, 2, 5);
        autoGroupKeywordsOption.setSelected(true);
        autoGroupKeywordsField.setText(Globals.prefs.get(JabRefPreferences.GROUPS_DEFAULT_FIELD));
        autoGroupKeywordsDeliminator.setText(Globals.prefs.get(JabRefPreferences.KEYWORD_SEPARATOR));
        autoGroupKeywordsHierarchicalDeliminator.setText(Keyword.DEFAULT_HIERARCHICAL_DELIMITER.toString());
        autoGroupPersonsField.setText(FieldName.AUTHOR);
        // ... for tex group
        optionsPanel.getChildren().add(texPanel);
        texPanel.setVisible(false);
        texPanel.setHgap(HGAP);
        texPanel.setVgap(VGAP);
        texPanel.setPadding(PADDING);
        texPanel.add(new Label(Localization.lang("Aux file")), 0, 0);
        texPanel.add(texGroupFilePath, 1, 0);

        // ... for buttons panel
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        // General panel
        GridPane generalPanel = new GridPane();
        GridPane textFieldPanel = new GridPane();
        GridPane selectPanel = new GridPane();
        generalPanel.add(textFieldPanel, 0, 0);
        generalPanel.add(selectPanel, 0, 1);
        generalPanel.setVgap(VGAP);
        generalPanel.setPadding(PADDING);
        generalPanel.setStyle("-fx-content-display:top;"
                              + "-fx-border-insets:0 0 0 0;"
                              + "-fx-border-color:#D3D3D3");

        ColumnConstraints columnLabel = new ColumnConstraints();
        columnLabel.setHalignment(HPos.RIGHT);
        textFieldPanel.getColumnConstraints().add(columnLabel);
        ColumnConstraints columnTextField = new ColumnConstraints();
        columnTextField.setHalignment(HPos.LEFT);
        textFieldPanel.getColumnConstraints().add(columnTextField);
        textFieldPanel.setVgap(VGAP);
        textFieldPanel.setHgap(HGAP);
        textFieldPanel.add(new Label(Localization.lang("Name")), 0, 0);
        textFieldPanel.add(nameField, 1, 0);
        textFieldPanel.add(new Label(Localization.lang("Description")), 0, 1);
        textFieldPanel.add(descriptionField, 1, 1);
        textFieldPanel.add(new Label(Localization.lang("Color")), 0, 2);
        textFieldPanel.add(colorField, 1, 2);
        textFieldPanel.add(new Label(Localization.lang("Icon")), 0, 3);
        textFieldPanel.add(iconField, 1, 3);

        selectPanel.setVgap(VGAP);
        selectPanel.add(explicitRadioButton, 0, 0);
        selectPanel.add(keywordsRadioButton, 0, 1);
        selectPanel.add(searchRadioButton, 0, 2);
        selectPanel.add(autoRadioButton, 0, 3);
        selectPanel.add(texRadioButton, 0, 4);

        // Context panel
        GridPane contextPanel = new GridPane();
        contextPanel.setVgap(VGAP);
        contextPanel.setHgap(HGAP);
        contextPanel.setPadding(PADDING);
        contextPanel.setStyle("-fx-content-display:top;"
                              + "-fx-border-insets:0 0 0 0;"
                              + "-fx-border-color:#D3D3D3");
        contextPanel.add(independentButton, 0, 0);
        contextPanel.add(intersectionButton, 0, 1);
        contextPanel.add(unionButton, 0, 2);

        // Description panel
        ScrollPane sp = new ScrollPane(descriptionWebView);
        sp.setPadding(PADDING);
        sp.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-content-display:top;"
                    + "-fx-border-insets:0 0 0 0;"
                    + "-fx-border-color:#D3D3D3");

        // create border
        HBox title1 = new HBox();
        HBox title2 = new HBox();
        HBox title3 = new HBox();
        HBox title4 = new HBox();
        Label title1Label = new Label(Localization.lang("General"));
        Label title2Label = new Label(Localization.lang("Hierarchical context"));
        Label title3Label = new Label(Localization.lang("Options"));
        Label title4Label = new Label(Localization.lang("Description"));
        title1Label.setTextFill(Color.web("#778899"));
        title2Label.setTextFill(Color.web("#778899"));
        title3Label.setTextFill(Color.web("#778899"));
        title4Label.setTextFill(Color.web("#778899"));
        title1.setPadding(new Insets(10, 0, 0, 0));
        title2.setPadding(new Insets(10, 0, 0, 0));
        title3.setPadding(new Insets(10, 0, 0, 0));
        title4.setPadding(new Insets(10, 0, 0, 0));
        title1.getChildren().add(title1Label);
        title2.getChildren().add(title2Label);
        title3.getChildren().add(title3Label);
        title4.getChildren().add(title4Label);

        // create layout
        GridPane mainPanel = new GridPane();
        getDialogPane().setContent(mainPanel);
        mainPanel.setPrefHeight(810);
        mainPanel.setPrefWidth(630);
        mainPanel.setPadding(new Insets(5, 15, 5, 5));
        mainPanel.add(title1, 0, 0);
        mainPanel.add(generalPanel, 0, 1);
        mainPanel.add(title2, 0, 2);
        mainPanel.add(contextPanel, 0, 3);
        mainPanel.add(title3, 0, 4);
        mainPanel.add(optionsPanel, 0, 5);
        mainPanel.add(title4, 0, 6);
        mainPanel.add(sp, 0, 7);

        updateComponents();

        // add listeners
        groupType.selectedToggleProperty().addListener((ObservableValue<? extends Toggle> ov, Toggle old_Toggle,
                                                        Toggle new_Toggle) -> {
            int select = INDEX_EXPLICIT_GROUP;
            if (groupType.getSelectedToggle() == explicitRadioButton) {
                select = INDEX_EXPLICIT_GROUP;
            } else if (groupType.getSelectedToggle() == keywordsRadioButton) {
                select = INDEX_KEYWORD_GROUP;
            } else if (groupType.getSelectedToggle() == searchRadioButton) {
                select = INDEX_SEARCH_GROUP;
            } else if (groupType.getSelectedToggle() == autoRadioButton) {
                select = INDEX_AUTO_GROUP;
            } else if (groupType.getSelectedToggle() == texRadioButton) {
                select = INDEX_TEX_GROUP;
            }
            for (Node n : optionsPanel.getChildren()) {
                n.setVisible(false);
            }
            optionsPanel.getChildren().get(select).setVisible(true);
            updateComponents();
        });

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                AbstractGroup resultingGroup = null;
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
                            if ((editedGroup == null) && (groupsWithSameName > 0)) {
                                // New group but there is already one group with the same name
                                warnAboutSameName = true;
                            }
                            if ((editedGroup != null) && !editedGroup.getName().equals(groupName) && (groupsWithSameName > 0)) {
                                // Edit group, changed name to something that is already present
                                warnAboutSameName = true;
                            }

                            if (warnAboutSameName) {
                                jabrefFrame.showMessage(
                                        Localization.lang("There exists already a group with the same name.", Character.toString(keywordDelimiter)));
                                return null;
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
                        resultingGroup = new SearchGroup(groupName, getContext(), searchGroupSearchExpression.getText().trim(),
                                isCaseSensitive(), isRegex());
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
                    return resultingGroup;
                } catch (IllegalArgumentException | IOException exception) {
                    jabrefFrame.showMessage(exception.getLocalizedMessage());
                    return null;
                }
            }
            return null;
        });

        ChangeListener<String> caretListener = (ObservableValue<? extends String> ov, String oldValue,
                                                String newValue) -> updateComponents();
        ChangeListener<Boolean> itemListener = (ObservableValue<? extends Boolean> ov, Boolean oldBoolean,
                                                Boolean newBoolean) -> updateComponents();

        nameField.textProperty().addListener(caretListener);
        colorField.textProperty().addListener(caretListener);
        descriptionField.textProperty().addListener(caretListener);
        iconField.textProperty().addListener(caretListener);
        keywordGroupSearchField.textProperty().addListener(caretListener);
        keywordGroupSearchTerm.textProperty().addListener(caretListener);
        keywordGroupCaseSensitive.selectedProperty().addListener(itemListener);
        keywordGroupRegExp.selectedProperty().addListener(itemListener);
        searchGroupSearchExpression.textProperty().addListener(caretListener);
        searchGroupRegExp.selectedProperty().addListener(itemListener);
        searchGroupRegExp.selectedProperty().addListener(itemListener);

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

    private void updateComponents() {
        // all groups need a name
        boolean okEnabled = !nameField.getText().trim().isEmpty();
        if (!okEnabled) {
            setDescription(Localization.lang("Please enter a name for the group."));
            //TODO: okButton.setDisable(true);
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
                                                                                               new SearchQuery(s1, isCaseSensitive(), isRegex()))
                                                                        .getDescription()));

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
        //TODO: okButton.setDisable(!okEnabled);
    }

    private String fromTextFlowToHTMLString(TextFlow textFlow) {
        StringBuilder htmlStringBuilder = new StringBuilder();
        for (Node node : textFlow.getChildren()) {
            if (node instanceof Text) {
                htmlStringBuilder.append(TooltipTextUtil.textToHTMLString((Text) node));
            }
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
        descriptionWebView.getEngine().loadContent("<html>" + description + "</html>");
    }

    /**
     * Sets the font of the name entry field.
     */
    private void setNameFontItalic(boolean italic) {
        Font f = nameField.getFont();
        if (italic) {
            Font.font(f.getFamily(), FontPosture.ITALIC, f.getSize());
        } else {
            Font.font(f.getFamily(), FontPosture.REGULAR, f.getSize());
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
