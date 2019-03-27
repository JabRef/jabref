package org.jabref.gui.groups;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.search.rules.describer.SearchDescribers;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.util.StandardFileType;
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

import com.jfoenix.controls.JFXColorPicker;

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

    // for all types
    private final TextField nameField = new TextField();
    private final TextField descriptionField = new TextField();
    private final JFXColorPicker colorField = new JFXColorPicker();
    private final TextField iconField = new TextField();
    private final RadioButton explicitRadioButton = new RadioButton(Localization.lang("Statically group entries by manual assignment"));
    private final RadioButton keywordsRadioButton = new RadioButton(Localization.lang("Dynamically group entries by searching a field for a keyword"));
    private final RadioButton searchRadioButton = new RadioButton(Localization.lang("Dynamically group entries by a free-form search expression"));
    private final RadioButton autoRadioButton = new RadioButton(Localization.lang("Automatically create groups"));
    private final RadioButton texRadioButton = new RadioButton(Localization.lang("Group containing entries cited in a given TeX file"));
    private final RadioButton independentButton = new RadioButton(Localization.lang("Independent group: When selected, view only this group's entries"));
    private final RadioButton intersectionButton = new RadioButton(Localization.lang("Refine supergroup: When selected, view entries contained in both this group and its supergroup"));
    private final RadioButton unionButton = new RadioButton(Localization.lang("Include subgroups: When selected, view entries contained in this group or its subgroups"));
    private final DialogService dialogService;
    private final JabRefPreferences prefs;

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
    private final Button texGroupBrowseButton = new Button("Browse");
    private final HBox texGroupHBox = new HBox(10);

    // for all types
    private final TextFlow descriptionTextFlow = new TextFlow();
    private final StackPane optionsPanel = new StackPane();


    /**
     * Shows a group add/edit dialog.
     *
     * @param jabrefFrame The parent frame.
     * @param editedGroup The group being edited, or null if a new group is to be
     *                    created.
     */
    public GroupDialog(DialogService dialogService, BasePanel basePanel, JabRefPreferences prefs, AbstractGroup editedGroup) {

        if (editedGroup == null) {
            this.setTitle(Localization.lang("Add subgroup"));
        } else {
            this.setTitle(Localization.lang("Edit group"));
        }

        explicitRadioButton.setSelected(true);

        descriptionTextFlow.setMinWidth(585);
        descriptionTextFlow.setPrefWidth(585);
        descriptionTextFlow.setMinHeight(180);
        descriptionTextFlow.setPrefHeight(180);

        this.dialogService = dialogService;
        this.prefs = prefs;

        // set default values (overwritten if editedGroup != null)
        keywordGroupSearchField.setText(prefs.get(JabRefPreferences.GROUPS_DEFAULT_FIELD));

        // configure elements
        ToggleGroup groupType = new ToggleGroup();
        explicitRadioButton.setToggleGroup(groupType);
        keywordsRadioButton.setToggleGroup(groupType);
        searchRadioButton.setToggleGroup(groupType);
        autoRadioButton.setToggleGroup(groupType);
        texRadioButton.setToggleGroup(groupType);

        // Build individual layout cards for each group
        VBox explicitPanel = createOptionsExplicitGroup();
        explicitPanel.setVisible(true);
        VBox keywordPanel = createOptionsKeywordGroup();
        VBox searchPanel = createOptionsSearchGroup();
        VBox autoPanel = createOptionsAutoGroup();
        VBox texPanel = createOptionsTexGroup();
        optionsPanel.getChildren().addAll(explicitPanel, keywordPanel, searchPanel, autoPanel, texPanel);
        optionsPanel.setPadding(new Insets(0, 0, 0, 10));

        // ... for buttons panel
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        // General information
        VBox contextPanel = new VBox(10);
        contextPanel.setPadding(new Insets(0, 0, 0, 10));
        contextPanel.getChildren().setAll(
                independentButton,
                intersectionButton,
                unionButton
        );
        ToggleGroup groupHierarchy = new ToggleGroup();
        independentButton.setToggleGroup(groupHierarchy);
        intersectionButton.setToggleGroup(groupHierarchy);
        unionButton.setToggleGroup(groupHierarchy);

        colorField.setMinHeight(20);
        VBox generalPanel = new VBox(10);
        generalPanel.getChildren().setAll(
                new VBox(
                        new Label(Localization.lang("Name")),
                        nameField
                ),
                new VBox(
                        new Label(Localization.lang("Description")),
                        descriptionField
                ),
                new HBox(30,
                        new VBox(
                                new Label(Localization.lang("Icon")),
                                iconField
                        ),
                        new VBox(
                                new Label(Localization.lang("Color")),
                                colorField
                        )
                ),
                new VBox(5,
                        new Label(Localization.lang("Hierarchical context")),
                        contextPanel
                )
        );

        VBox selectPanel = new VBox(10,
                explicitRadioButton,
                keywordsRadioButton,
                searchRadioButton,
                autoRadioButton,
                texRadioButton
        );
        selectPanel.setPadding(new Insets(0, 0, 0, 10));

        // Description panel
        ScrollPane descriptionPane = new ScrollPane(descriptionTextFlow);
        descriptionPane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        descriptionPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        // create layout
        HBox mainPanel = new HBox(15);
        getDialogPane().setContent(mainPanel);
        mainPanel.setPadding(new Insets(5, 15, 5, 15));
        mainPanel.getChildren().setAll(
                new VBox(5,
                        generalPanel,
                        new VBox(5,
                                new Label(Localization.lang("Type")),
                                selectPanel
                        )
                ),
                new Separator(Orientation.VERTICAL),
                new VBox(5,
                        new VBox(
                                new Label(Localization.lang("Options")),
                                optionsPanel
                        ),
                        new Label(Localization.lang("Description")),
                        descriptionPane
                )
        );

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
                            dialogService.showWarningDialogAndWait(null, Localization.lang("The group name contains the keyword separator \"%0\" and thus probably does not work as expected.", Character.toString(keywordDelimiter)));
                        }

                        Optional<GroupTreeNode> rootGroup = basePanel.getBibDatabaseContext().getMetaData().getGroups();
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
                                dialogService.showErrorDialogAndWait(Localization.lang("There exists already a group with the same name.", Character.toString(keywordDelimiter)));
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

                    resultingGroup.setColor(colorField.getValue());
                    resultingGroup.setDescription(descriptionField.getText());
                    resultingGroup.setIconName(iconField.getText());
                    return resultingGroup;
                } catch (IllegalArgumentException | IOException exception) {
                    dialogService.showErrorDialogAndWait(exception.getLocalizedMessage(), exception);
                    return null;
                }
            }
            return null;
        });

        EventHandler<ActionEvent> actionHandler = (ActionEvent e) -> updateComponents();
        nameField.setOnAction(actionHandler);
        descriptionField.setOnAction(actionHandler);
        iconField.setOnAction(actionHandler);
        keywordGroupSearchField.setOnAction(actionHandler);
        keywordGroupSearchTerm.setOnAction(actionHandler);
        keywordGroupCaseSensitive.setOnAction(actionHandler);
        keywordGroupRegExp.setOnAction(actionHandler);
        searchGroupSearchExpression.setOnAction(actionHandler);
        searchGroupRegExp.setOnAction(actionHandler);

        // configure for current type
        if (editedGroup == null) {
            // creating new group -> defaults!
            explicitRadioButton.setSelected(true);
            setContext(GroupHierarchyType.INDEPENDENT);
        } else {
            nameField.setText(editedGroup.getName());
            editedGroup.getColor().ifPresent(colorField::setValue);
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

        setResizable(false);
        getDialogPane().getScene().getWindow().sizeToScene();
    }

    public GroupDialog(DialogService dialogService) {
        this(dialogService, JabRefGUI.getMainFrame().getCurrentBasePanel(), Globals.prefs, null);
    }

    public GroupDialog(DialogService dialogService, AbstractGroup editedGroup) {
        this(dialogService, JabRefGUI.getMainFrame().getCurrentBasePanel(), Globals.prefs, editedGroup);
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

    private VBox createOptionsTexGroup() {
        VBox texPanel = new VBox();
        texPanel.setVisible(false);
        texPanel.getChildren().add(new Label(Localization.lang("Aux file")));
        texGroupBrowseButton.setOnAction((ActionEvent e) -> openBrowseDialog());
        texGroupHBox.getChildren().add(texGroupFilePath);
        texGroupHBox.getChildren().add(texGroupBrowseButton);
        texGroupHBox.setHgrow(texGroupFilePath, Priority.ALWAYS);
        texPanel.getChildren().add(texGroupHBox);
        return texPanel;
    }

    private VBox createOptionsAutoGroup() {
        VBox autoPanel = new VBox(10);
        autoPanel.setVisible(false);
        ToggleGroup tg = new ToggleGroup();
        autoGroupKeywordsOption.setToggleGroup(tg);
        autoGroupPersonsOption.setToggleGroup(tg);
        VBox fieldToGroupByKeywords = new VBox(
                new Label(Localization.lang("Field to group by") + ":"),
                autoGroupKeywordsField
        );
        fieldToGroupByKeywords.setPadding(new Insets(0, 0, 0, 20));
        VBox delimiterCharacters = new VBox(
                new Label(Localization.lang("Use the following delimiter character(s):")),
                new HBox(10,
                        autoGroupKeywordsDeliminator,
                        autoGroupKeywordsHierarchicalDeliminator
                )
        );
        delimiterCharacters.setPadding(new Insets(0, 0, 0, 20));
        VBox fieldToGroupByPersons = new VBox(
                new Label(Localization.lang("Field to group by") + ":"),
                autoGroupPersonsField
        );
        fieldToGroupByPersons.setPadding(new Insets(0, 0, 0, 20));
        autoPanel.getChildren().setAll(
                autoGroupKeywordsOption,
                fieldToGroupByKeywords,
                delimiterCharacters,
                autoGroupPersonsOption,
                fieldToGroupByPersons
        );
        autoGroupKeywordsOption.setSelected(true);
        autoGroupKeywordsField.setText(Globals.prefs.get(JabRefPreferences.GROUPS_DEFAULT_FIELD));
        autoGroupKeywordsDeliminator.setText(Globals.prefs.get(JabRefPreferences.KEYWORD_SEPARATOR));
        autoGroupKeywordsHierarchicalDeliminator.setText(Keyword.DEFAULT_HIERARCHICAL_DELIMITER.toString());
        autoGroupPersonsField.setText(FieldName.AUTHOR);
        return autoPanel;
    }

    private VBox createOptionsSearchGroup() {
        VBox searchPanel = new VBox(10);
        searchPanel.setVisible(false);
        searchPanel.getChildren().setAll(
                new VBox(
                        new Label(Localization.lang("Search expression")),
                        searchGroupSearchExpression
                ),
                searchGroupCaseSensitive,
                searchGroupRegExp
        );
        return searchPanel;
    }

    private VBox createOptionsExplicitGroup() {
        return new VBox();
    }

    private VBox createOptionsKeywordGroup() {
        VBox keywordPanel = new VBox(10);
        keywordPanel.setVisible(false);
        keywordPanel.getChildren().setAll(
                new VBox(
                        new Label(Localization.lang("Field")),
                        keywordGroupSearchField
                ),
                new VBox(
                        new Label(Localization.lang("Keyword")),
                        keywordGroupSearchTerm
                ),
                keywordGroupCaseSensitive,
                keywordGroupRegExp
        );
        return keywordPanel;
    }

    private void updateComponents() {
        // all groups need a name
        boolean okEnabled = !nameField.getText().trim().isEmpty();
        if (!okEnabled) {
            setDescription(Localization.lang("Please enter a name for the group."));
            getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
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
        getDialogPane().lookupButton(ButtonType.OK).setDisable(!okEnabled);
    }

    private void openBrowseDialog() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
            .addExtensionFilter(StandardFileType.AUX)
            .withDefaultExtension(StandardFileType.AUX)
            .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> texGroupFilePath.setText(file.toAbsolutePath().toString()));
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
        descriptionTextFlow.getChildren().setAll(createFormattedDescription(description));
    }

    private ArrayList<Node> createFormattedDescription(String descriptionHTML) {
        ArrayList<Node> nodes = new ArrayList<>();

        descriptionHTML = descriptionHTML.replaceAll("<p>|<br>", "\n");

        String[] boldSplit = descriptionHTML.split("(?=<b>)|(?<=</b>)|(?=<i>)|(?<=</i>)|(?=<tt>)|(?<=</tt>)|(?=<kbd>)|(?<=</kbd>)");

        for (String bs : boldSplit) {

            if (bs.matches("<b>[^<>]*</b>")) {

                bs = bs.replaceAll("<b>|</b>", "");
                Text textElement = new Text(bs);
                textElement.setStyle("-fx-font-weight: bold");
                nodes.add(textElement);

            } else if (bs.matches("<i>[^<>]*</i>")) {

                bs = bs.replaceAll("<i>|</i>", "");
                Text textElement = new Text(bs);
                textElement.setStyle("-fx-font-style: italic");
                nodes.add(textElement);

            } else if (bs.matches("<tt>[^<>]*</tt>|<kbd>[^<>]*</kbd>")) {

                bs = bs.replaceAll("<tt>|</tt>|<kbd>|</kbd>", "");
                Text textElement = new Text(bs);
                textElement.setStyle("-fx-font-family: 'Courier New', Courier, monospace");
                nodes.add(textElement);

            } else {
                nodes.add(new Text(bs));
            }
        }

        return nodes;
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
