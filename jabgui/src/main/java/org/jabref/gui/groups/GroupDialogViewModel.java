package org.jabref.gui.groups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.importer.actions.SearchGroupsMigrationAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.IndexManager;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticDateGroup;
import org.jabref.model.groups.AutomaticGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.DateGranularity;
import org.jabref.model.groups.DirectoryGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.util.FileUpdateMonitor;

import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.jspecify.annotations.Nullable;

public class GroupDialogViewModel {
    // Basic Settings
    private final StringProperty nameProperty = new SimpleStringProperty("");
    private final StringProperty descriptionProperty = new SimpleStringProperty("");
    private final StringProperty iconProperty = new SimpleStringProperty("");
    private final BooleanProperty colorUseProperty = new SimpleBooleanProperty();
    private final ObjectProperty<Color> colorProperty = new SimpleObjectProperty<>();
    private final ListProperty<GroupHierarchyType> groupHierarchyListProperty = new SimpleListProperty<>();
    private final ObjectProperty<GroupHierarchyType> groupHierarchySelectedProperty = new SimpleObjectProperty<>();

    // Type
    private final BooleanProperty typeExplicitProperty = new SimpleBooleanProperty();
    private final BooleanProperty typeKeywordsProperty = new SimpleBooleanProperty();
    private final BooleanProperty typeSearchProperty = new SimpleBooleanProperty();
    private final BooleanProperty typeAutoProperty = new SimpleBooleanProperty();
    private final BooleanProperty typeTexProperty = new SimpleBooleanProperty();

    // Option Groups
    private final StringProperty keywordGroupSearchTermProperty = new SimpleStringProperty("");
    private final StringProperty keywordGroupSearchFieldProperty = new SimpleStringProperty("");
    private final BooleanProperty keywordGroupCaseSensitiveProperty = new SimpleBooleanProperty();
    private final BooleanProperty keywordGroupRegexProperty = new SimpleBooleanProperty();

    private final StringProperty searchGroupSearchTermProperty = new SimpleStringProperty("");
    private final ObjectProperty<EnumSet<SearchFlags>> searchFlagsProperty = new SimpleObjectProperty<>(EnumSet.noneOf(SearchFlags.class));

    private final BooleanProperty autoGroupKeywordsOptionProperty = new SimpleBooleanProperty();
    private final StringProperty autoGroupKeywordsFieldProperty = new SimpleStringProperty("");
    private final StringProperty autoGroupKeywordsDelimiterProperty = new SimpleStringProperty("");
    private final StringProperty autoGroupKeywordsHierarchicalDelimiterProperty = new SimpleStringProperty("");
    private final BooleanProperty autoGroupPersonsOptionProperty = new SimpleBooleanProperty();
    private final StringProperty autoGroupPersonsFieldProperty = new SimpleStringProperty("");

    private final StringProperty texGroupFilePathProperty = new SimpleStringProperty("");

    private final BooleanProperty typeDirectoryProperty = new SimpleBooleanProperty();
    private final StringProperty directoryGroupPathProperty = new SimpleStringProperty("");

    // Date Group Properties
    private final BooleanProperty dateRadioButtonSelectedProperty = new SimpleBooleanProperty();
    private final ObjectProperty<Field> dateGroupFieldProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<DateGranularity> dateGroupOptionProperty = new SimpleObjectProperty<>();
    private final BooleanProperty dateGroupIncludeEmptyProperty = new SimpleBooleanProperty();

    private Validator nameValidator;
    private Validator nameContainsDelimiterValidator;
    private Validator sameNameValidator;
    private Validator keywordRegexValidator;
    private Validator keywordFieldEmptyValidator;
    private Validator keywordSearchTermEmptyValidator;
    private Validator searchSearchTermEmptyValidator;
    private Validator texGroupFilePathValidator;
    private Validator directoryGroupPathValidator;
    private CompositeValidator validator;

    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibDatabaseContext currentDatabase;
    private final AbstractGroup editedGroup;
    private final GroupTreeNode parentNode;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final StateManager stateManager;

    public GroupDialogViewModel(DialogService dialogService,
                                BibDatabaseContext currentDatabase,
                                GuiPreferences preferences,
                                @Nullable AbstractGroup editedGroup,
                                @Nullable GroupTreeNode parentNode,
                                FileUpdateMonitor fileUpdateMonitor,
                                StateManager stateManager) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.currentDatabase = currentDatabase;
        this.editedGroup = editedGroup;
        this.parentNode = parentNode;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.stateManager = stateManager;

        setupValidation();
        setValues();
    }

    private void setupValidation() {
        validator = new CompositeValidator();

        nameValidator = new FunctionBasedValidator<>(
                nameProperty,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Please enter a name for the group.")));

        nameContainsDelimiterValidator = new FunctionBasedValidator<>(
                nameProperty,
                name -> !name.contains(Character.toString(preferences.getBibEntryPreferences().getKeywordSeparator())),
                ValidationMessage.warning(
                        Localization.lang(
                                "The group name contains the keyword separator \"%0\" and thus probably does not work as expected.",
                                Character.toString(preferences.getBibEntryPreferences().getKeywordSeparator())
                        )));

        sameNameValidator = new FunctionBasedValidator<>(
                nameProperty,
                name -> {
                    Optional<GroupTreeNode> rootGroup = currentDatabase.getMetaData().getGroups();
                    if (rootGroup.isPresent()) {
                        boolean groupsExistWithSameName = !rootGroup.get().findChildrenSatisfying(group -> group.getName().equals(name)).isEmpty();
                        if ((editedGroup == null) && groupsExistWithSameName) {
                            // New group but there is already one group with the same name
                            return false;
                        }

                        // Edit group, changed name to something that is already present
                        return (editedGroup == null) || editedGroup.getName().equals(name) || !groupsExistWithSameName;
                    }
                    return true;
                },
                ValidationMessage.warning(
                        Localization.lang("There already exists a group with the same name.\nIf you use it, it will inherit all entries from this other group.")
                )
        );

        keywordRegexValidator = new FunctionBasedValidator<>(
                keywordGroupSearchTermProperty,
                input -> {
                    if (!keywordGroupRegexProperty.get()) {
                        return true;
                    }

                    if (StringUtil.isNullOrEmpty(input)) {
                        return false;
                    }

                    try {
                        Pattern.compile(input);
                        return true;
                    } catch (PatternSyntaxException _) {
                        return false;
                    }
                },
                ValidationMessage.error("%s > %n %s %n %n %s".formatted(
                        Localization.lang("Searching for a keyword"),
                        Localization.lang("Keywords"),
                        Localization.lang("Invalid regular expression."))));

        keywordFieldEmptyValidator = new FunctionBasedValidator<>(
                keywordGroupSearchFieldProperty,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Please enter a field name to search for a keyword.")));

        keywordSearchTermEmptyValidator = new FunctionBasedValidator<>(
                keywordGroupSearchTermProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error("%s > %n %s %n %n %s".formatted(
                        Localization.lang("Searching for a keyword"),
                        Localization.lang("Keywords"),
                        Localization.lang("Search term is empty.")
                )));

        searchSearchTermEmptyValidator = new FunctionBasedValidator<>(
                searchGroupSearchTermProperty,
                input -> {
                    if (StringUtil.isNullOrEmpty(input)) {
                        return false;
                    }
                    return new SearchQuery(input).isValid();
                },
                ValidationMessage.error(Localization.lang("Illegal search expression")));

        texGroupFilePathValidator = new FunctionBasedValidator<>(
                texGroupFilePathProperty,
                input -> {
                    if (StringUtil.isBlank(input)) {
                        return false;
                    } else {
                        Path inputPath = getAbsoluteTexGroupPath(input);
                        if (!inputPath.isAbsolute() || !Files.isRegularFile(inputPath)) {
                            return false;
                        }
                        return FileUtil.getFileExtension(input)
                                       .map("aux"::equalsIgnoreCase)
                                       .orElse(false);
                    }
                },
                ValidationMessage.error(Localization.lang("Please provide a valid aux file.")));

        directoryGroupPathValidator = new FunctionBasedValidator<>(
                directoryGroupPathProperty,
                input -> {
                    if (StringUtil.isBlank(input)) {
                        return false;
                    }
                    Path inputPath = Path.of(input);
                    return Files.isDirectory(inputPath);
                },
                ValidationMessage.error(Localization.lang("Please provide a valid directory.")));

        typeSearchProperty.addListener((_, _, isSelected) -> {
            if (isSelected) {
                validator.addValidators(searchSearchTermEmptyValidator);
            } else {
                validator.removeValidators(searchSearchTermEmptyValidator);
            }
        });

        typeKeywordsProperty.addListener((_, _, isSelected) -> {
            if (isSelected) {
                validator.addValidators(keywordFieldEmptyValidator, keywordRegexValidator, keywordSearchTermEmptyValidator);
            } else {
                validator.removeValidators(keywordFieldEmptyValidator, keywordRegexValidator, keywordSearchTermEmptyValidator);
            }
        });

        typeTexProperty.addListener((_, _, isSelected) -> {
            if (isSelected) {
                validator.addValidators(texGroupFilePathValidator);
            } else {
                validator.removeValidators(texGroupFilePathValidator);
            }
        });

        typeDirectoryProperty.addListener((_, _, isSelected) -> {
            if (isSelected) {
                validator.addValidators(directoryGroupPathValidator);
            } else {
                validator.removeValidators(directoryGroupPathValidator);
            }
        });

        validator.addValidators(nameValidator,
                nameContainsDelimiterValidator,
                sameNameValidator);
    }

    /**
     * Gets the absolute path relative to the LatexFileDirectory, if given a relative path
     *
     * @param input the user input path
     * @return an absolute path if LatexFileDirectory exists; otherwise, returns input
     */
    private Path getAbsoluteTexGroupPath(String input) {
        Optional<Path> latexFileDirectory = currentDatabase.getMetaData().getLatexFileDirectory(preferences.getFilePreferences().getUserAndHost());
        return latexFileDirectory.map(path -> path.resolve(input)).orElse(Path.of(input));
    }

    public void validationHandler(Event event) {
        ValidationStatus validationStatus = validator.getValidationStatus();
        Optional<ValidationMessage> highestMessage = validationStatus.getHighestMessage();
        if (highestMessage.isPresent()) {
            dialogService.showErrorDialogAndWait(highestMessage.get().getMessage());
            // consume the event to prevent the dialog to close
            event.consume();
        }
    }

    public AbstractGroup resultConverter(ButtonType button) {
        if (button != ButtonType.OK) {
            return null;
        }

        AbstractGroup resultingGroup = null;
        try {
            String groupName = nameProperty.getValue().trim();
            // Check Directory structure checkbox first (has priority over radio buttons)
            if (typeDirectoryProperty.get()) {
                resultingGroup = new DirectoryGroup(
                        groupName,
                        groupHierarchySelectedProperty.getValue(),
                        Path.of(directoryGroupPathProperty.getValue().trim())
                );
            } else if (typeExplicitProperty.get()) {
                resultingGroup = new ExplicitGroup(
                        groupName,
                        groupHierarchySelectedProperty.getValue(),
                        preferences.getBibEntryPreferences().getKeywordSeparator());
            } else if (typeKeywordsProperty.get()) {
                if (keywordGroupRegexProperty.get()) {
                    resultingGroup = new RegexKeywordGroup(
                            groupName,
                            groupHierarchySelectedProperty.getValue(),
                            FieldFactory.parseField(keywordGroupSearchFieldProperty.getValue().trim()),
                            keywordGroupSearchTermProperty.getValue().trim(),
                            keywordGroupCaseSensitiveProperty.getValue());
                } else {
                    resultingGroup = new WordKeywordGroup(
                            groupName,
                            groupHierarchySelectedProperty.getValue(),
                            FieldFactory.parseField(keywordGroupSearchFieldProperty.getValue().trim()),
                            keywordGroupSearchTermProperty.getValue().trim(),
                            keywordGroupCaseSensitiveProperty.getValue(),
                            preferences.getBibEntryPreferences().getKeywordSeparator(),
                            false);
                }
            } else if (typeSearchProperty.get()) {
                resultingGroup = new SearchGroup(
                        groupName,
                        groupHierarchySelectedProperty.getValue(),
                        searchGroupSearchTermProperty.getValue().trim(),
                        searchFlagsProperty.getValue());

                if (currentDatabase.getMetaData().getGroupSearchSyntaxVersion().isEmpty()) {
                    // If the syntax version for search groups is not present, it indicates that the groups
                    // have not been migrated to the new syntax, or this is the first search group in the library.
                    // If this is the first search group, set the syntax version to the new version.
                    // Otherwise, it means that the user did not accept the migration to the new version.
                    Optional<GroupTreeNode> groups = currentDatabase.getMetaData().getGroups();
                    if (groups.filter(this::groupOrSubgroupIsSearchGroup).isEmpty()) {
                        currentDatabase.getMetaData().setGroupSearchSyntaxVersion(SearchGroupsMigrationAction.VERSION_6_0_ALPHA_1);
                    }
                }

                Optional<IndexManager> indexManager = stateManager.getIndexManager(currentDatabase);
                if (indexManager.isPresent()) {
                    SearchGroup searchGroup = (SearchGroup) resultingGroup;
                    searchGroup.setMatchedEntries(indexManager.get().search(searchGroup.getSearchQuery()).getMatchedEntries());
                }
            } else if (typeAutoProperty.get()) {
                if (autoGroupKeywordsOptionProperty.get()) {
                    // Set default value for delimiters: ',' for base and '>' for hierarchical
                    char delimiter = ',';
                    char hierarDelimiter = Keyword.DEFAULT_HIERARCHICAL_DELIMITER;
                    autoGroupKeywordsOptionProperty.setValue(Boolean.TRUE);
                    // Modify values for delimiters if user provided customized values
                    if (!autoGroupKeywordsDelimiterProperty.getValue().isEmpty()) {
                        delimiter = autoGroupKeywordsDelimiterProperty.getValue().charAt(0);
                    }
                    if (!autoGroupKeywordsHierarchicalDelimiterProperty.getValue().isEmpty()) {
                        hierarDelimiter = autoGroupKeywordsHierarchicalDelimiterProperty.getValue().charAt(0);
                    }
                    resultingGroup = new AutomaticKeywordGroup(
                            groupName,
                            groupHierarchySelectedProperty.getValue(),
                            FieldFactory.parseField(autoGroupKeywordsFieldProperty.getValue().trim()),
                            delimiter,
                            hierarDelimiter);
                } else {
                    resultingGroup = new AutomaticPersonsGroup(
                            groupName,
                            groupHierarchySelectedProperty.getValue(),
                            FieldFactory.parseField(autoGroupPersonsFieldProperty.getValue().trim()));
                }
            } else if (typeTexProperty.get()) {
                resultingGroup = TexGroup.create(
                        groupName,
                        groupHierarchySelectedProperty.getValue(),
                        Path.of(texGroupFilePathProperty.getValue().trim()),
                        new DefaultAuxParser(new BibDatabase()),
                        fileUpdateMonitor,
                        currentDatabase.getMetaData(),
                        preferences.getFilePreferences().getUserAndHost()
                );
            } else if (dateRadioButtonSelectedProperty.get()) {
                resultingGroup = new AutomaticDateGroup(
                        groupName,
                        groupHierarchySelectedProperty.getValue(),
                        dateGroupFieldProperty.getValue(),
                        dateGroupOptionProperty.getValue()
                );
            }

            if (resultingGroup != null) {
                preferences.getGroupsPreferences().setDefaultHierarchicalContext(groupHierarchySelectedProperty.getValue());

                resultingGroup.setColor(colorUseProperty.get() ? colorProperty.getValue().toString() : null);
                resultingGroup.setDescription(descriptionProperty.getValue());
                resultingGroup.setIconName(iconProperty.getValue());
                return resultingGroup;
            }

            return null;
        } catch (IllegalArgumentException | IOException exception) {
            dialogService.showErrorDialogAndWait(exception.getLocalizedMessage(), exception);
            return null;
        }
    }

    public void setValues() {
        groupHierarchyListProperty.setValue(FXCollections.observableArrayList(GroupHierarchyType.values()));

        if (editedGroup == null) {
            // creating new group -> defaults!
            // TODO: Create default group (via org.jabref.logic.groups.GroupsFactory) and use values

            colorUseProperty.setValue(false);
            colorProperty.setValue(determineColor());
            if (parentNode != null) {
                parentNode.getGroup()
                          .getIconName()
                          .filter(iconName -> !GroupsFactory.ALL_ENTRIES_GROUP_DEFAULT_ICON.equals(iconName))
                          .ifPresent(iconProperty::setValue);
                parentNode.getGroup().getColor().ifPresent(color -> colorUseProperty.setValue(true));
            }
            typeExplicitProperty.setValue(true);
            groupHierarchySelectedProperty.setValue(preferences.getGroupsPreferences().getDefaultHierarchicalContext());
            autoGroupKeywordsOptionProperty.setValue(Boolean.TRUE);

            // Initialize Date Group defaults
            dateGroupFieldProperty.setValue(StandardField.DATE);
            dateGroupOptionProperty.setValue(DateGranularity.YEAR);
            dateGroupIncludeEmptyProperty.setValue(false);
        } else {
            nameProperty.setValue(editedGroup.getName());
            colorUseProperty.setValue(editedGroup.getColor().isPresent());
            colorProperty.setValue(editedGroup.getColor().map(Color::valueOf).orElse(IconTheme.getDefaultGroupColor()));
            descriptionProperty.setValue(editedGroup.getDescription().orElse(""));
            iconProperty.setValue(editedGroup.getIconName().orElse(""));
            groupHierarchySelectedProperty.setValue(editedGroup.getHierarchicalContext());

            if (editedGroup.getClass() == WordKeywordGroup.class) {
                typeKeywordsProperty.setValue(true);

                WordKeywordGroup group = (WordKeywordGroup) editedGroup;
                keywordGroupSearchFieldProperty.setValue(group.getSearchField().getName());
                keywordGroupSearchTermProperty.setValue(group.getSearchExpression());
                keywordGroupCaseSensitiveProperty.setValue(group.isCaseSensitive());
                keywordGroupRegexProperty.setValue(false);
            } else if (editedGroup.getClass() == RegexKeywordGroup.class) {
                typeKeywordsProperty.setValue(true);

                RegexKeywordGroup group = (RegexKeywordGroup) editedGroup;
                keywordGroupSearchFieldProperty.setValue(group.getSearchField().getName());
                keywordGroupSearchTermProperty.setValue(group.getSearchExpression());
                keywordGroupCaseSensitiveProperty.setValue(group.isCaseSensitive());
                keywordGroupRegexProperty.setValue(true);
            } else if (editedGroup.getClass() == SearchGroup.class) {
                typeSearchProperty.setValue(true);

                SearchGroup group = (SearchGroup) editedGroup;
                searchGroupSearchTermProperty.setValue(group.getSearchExpression());
                searchFlagsProperty.setValue(group.getSearchFlags());
            } else if (editedGroup.getClass() == ExplicitGroup.class) {
                typeExplicitProperty.setValue(true);
            } else if (editedGroup instanceof AutomaticGroup) {
                typeAutoProperty.setValue(true);

                if (editedGroup.getClass() == AutomaticKeywordGroup.class) {
                    AutomaticKeywordGroup group = (AutomaticKeywordGroup) editedGroup;
                    autoGroupKeywordsOptionProperty.setValue(Boolean.TRUE);
                    autoGroupKeywordsDelimiterProperty.setValue(group.getKeywordDelimiter().toString());
                    autoGroupKeywordsHierarchicalDelimiterProperty.setValue(group.getKeywordHierarchicalDelimiter().toString());
                    autoGroupKeywordsFieldProperty.setValue(group.getField().getName());
                } else if (editedGroup.getClass() == AutomaticPersonsGroup.class) {
                    AutomaticPersonsGroup group = (AutomaticPersonsGroup) editedGroup;
                    autoGroupPersonsOptionProperty.setValue(Boolean.TRUE);
                    autoGroupPersonsFieldProperty.setValue(group.getField().getName());
                } else if (editedGroup.getClass() == AutomaticDateGroup.class) {
                    AutomaticDateGroup group = (AutomaticDateGroup) editedGroup;
                    dateRadioButtonSelectedProperty.setValue(Boolean.TRUE);
                    dateGroupFieldProperty.setValue(group.getField());
                    dateGroupOptionProperty.setValue(group.getGranularity());
                    dateGroupIncludeEmptyProperty.setValue(false);
                }
            } else if (editedGroup.getClass() == TexGroup.class) {
                typeTexProperty.setValue(true);

                TexGroup group = (TexGroup) editedGroup;
                texGroupFilePathProperty.setValue(group.getFilePath().toString());
            } else if (editedGroup instanceof DirectoryGroup group) {
                typeDirectoryProperty.setValue(true);
                directoryGroupPathProperty.setValue(group.getDirectoryPath().toString());
            }
        }
    }

    private Color determineColor() {
        Color color;
        if (parentNode == null) {
            color = GroupColorPicker.generateColor(List.of());
        } else {
            List<Color> colorsOfSiblings = parentNode.getChildren().stream()
                                                     .map(child -> child.getGroup().getColor())
                                                     .flatMap(Optional::stream)
                                                     .map(Color::valueOf)
                                                     .toList();
            Optional<Color> parentColor = parentNode.getGroup().getColor().map(Color::valueOf);
            color = parentColor.map(value -> GroupColorPicker.generateColor(colorsOfSiblings, value))
                               .orElseGet(() -> GroupColorPicker.generateColor(colorsOfSiblings));
        }
        return color;
    }

    public void texGroupBrowse() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.AUX)
                .withDefaultExtension(StandardFileType.AUX)
                .withInitialDirectory(texGroupFilePathProperty.getValue().isBlank() ?
                                      currentDatabase.getMetaData()
                                                     .getLatexFileDirectory(preferences.getFilePreferences().getUserAndHost())
                                                     .orElse(FileUtil.getInitialDirectory(currentDatabase, preferences.getFilePreferences().getWorkingDirectory())).toString() : texGroupFilePathProperty.get()).build();
        dialogService.showFileOpenDialog(fileDialogConfiguration)
                     .ifPresent(file -> texGroupFilePathProperty.setValue(
                             FileUtil.relativize(file.toAbsolutePath(), getFileDirectoriesAsPaths()).toString()
                     ));
    }

    /**
     * Opens a directory chooser dialog for selecting the directory path for DirectoryGroup.
     */
    public void directoryGroupBrowse() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(directoryGroupPathProperty.getValue().isBlank() ?
                                      FileUtil.getInitialDirectory(currentDatabase, preferences.getFilePreferences().getWorkingDirectory()) :
                                      Path.of(directoryGroupPathProperty.getValue()))
                .build();
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(directory -> directoryGroupPathProperty.setValue(directory.toAbsolutePath().toString()));
    }

    private List<Path> getFileDirectoriesAsPaths() {
        List<Path> fileDirs = new ArrayList<>();
        MetaData metaData = currentDatabase.getMetaData();
        metaData.getLatexFileDirectory(preferences.getFilePreferences().getUserAndHost()).ifPresent(fileDirs::add);

        return fileDirs;
    }

    public ValidationStatus validationStatus() {
        return validator.getValidationStatus();
    }

    public ValidationStatus nameValidationStatus() {
        return nameValidator.getValidationStatus();
    }

    public ValidationStatus nameContainsDelimiterValidationStatus() {
        return nameContainsDelimiterValidator.getValidationStatus();
    }

    public ValidationStatus sameNameValidationStatus() {
        return sameNameValidator.getValidationStatus();
    }

    public ValidationStatus searchSearchTermEmptyValidationStatus() {
        return searchSearchTermEmptyValidator.getValidationStatus();
    }

    public ValidationStatus keywordRegexValidationStatus() {
        return keywordRegexValidator.getValidationStatus();
    }

    public ValidationStatus keywordFieldEmptyValidationStatus() {
        return keywordFieldEmptyValidator.getValidationStatus();
    }

    public ValidationStatus keywordSearchTermEmptyValidationStatus() {
        return keywordSearchTermEmptyValidator.getValidationStatus();
    }

    public ValidationStatus texGroupFilePathValidatonStatus() {
        return texGroupFilePathValidator.getValidationStatus();
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public StringProperty descriptionProperty() {
        return descriptionProperty;
    }

    public StringProperty iconProperty() {
        return iconProperty;
    }

    public BooleanProperty colorUseProperty() {
        return colorUseProperty;
    }

    public ObjectProperty<Color> colorFieldProperty() {
        return colorProperty;
    }

    public ListProperty<GroupHierarchyType> groupHierarchyListProperty() {
        return groupHierarchyListProperty;
    }

    public ObjectProperty<GroupHierarchyType> groupHierarchySelectedProperty() {
        return groupHierarchySelectedProperty;
    }

    public BooleanProperty typeExplicitProperty() {
        return typeExplicitProperty;
    }

    public BooleanProperty typeKeywordsProperty() {
        return typeKeywordsProperty;
    }

    public BooleanProperty typeSearchProperty() {
        return typeSearchProperty;
    }

    public BooleanProperty typeAutoProperty() {
        return typeAutoProperty;
    }

    public BooleanProperty typeTexProperty() {
        return typeTexProperty;
    }

    public StringProperty keywordGroupSearchTermProperty() {
        return keywordGroupSearchTermProperty;
    }

    public StringProperty keywordGroupSearchFieldProperty() {
        return keywordGroupSearchFieldProperty;
    }

    public BooleanProperty keywordGroupCaseSensitiveProperty() {
        return keywordGroupCaseSensitiveProperty;
    }

    public BooleanProperty keywordGroupRegexProperty() {
        return keywordGroupRegexProperty;
    }

    public StringProperty searchGroupSearchTermProperty() {
        return searchGroupSearchTermProperty;
    }

    public ObjectProperty<EnumSet<SearchFlags>> searchFlagsProperty() {
        return searchFlagsProperty;
    }

    public void setSearchFlag(SearchFlags searchFlag, boolean value) {
        if (value) {
            searchFlagsProperty.getValue().add(searchFlag);
        } else {
            searchFlagsProperty.getValue().remove(searchFlag);
        }
    }

    public BooleanProperty autoGroupKeywordsOptionProperty() {
        return autoGroupKeywordsOptionProperty;
    }

    public StringProperty autoGroupKeywordsFieldProperty() {
        return autoGroupKeywordsFieldProperty;
    }

    public StringProperty autoGroupKeywordsDeliminatorProperty() {
        return autoGroupKeywordsDelimiterProperty;
    }

    public StringProperty autoGroupKeywordsHierarchicalDeliminatorProperty() {
        return autoGroupKeywordsHierarchicalDelimiterProperty;
    }

    public BooleanProperty autoGroupPersonsOptionProperty() {
        return autoGroupPersonsOptionProperty;
    }

    public StringProperty autoGroupPersonsFieldProperty() {
        return autoGroupPersonsFieldProperty;
    }

    public StringProperty texGroupFilePathProperty() {
        return texGroupFilePathProperty;
    }

    public BooleanProperty typeDirectoryProperty() {
        return typeDirectoryProperty;
    }

    public StringProperty directoryGroupPathProperty() {
        return directoryGroupPathProperty;
    }

    public ValidationStatus directoryGroupPathValidationStatus() {
        return directoryGroupPathValidator.getValidationStatus();
    }

    public BooleanProperty dateRadioButtonSelectedProperty() {
        return dateRadioButtonSelectedProperty;
    }

    public ObjectProperty<Field> dateGroupFieldProperty() {
        return dateGroupFieldProperty;
    }

    public ObjectProperty<DateGranularity> dateGroupOptionProperty() {
        return dateGroupOptionProperty;
    }

    public BooleanProperty dateGroupIncludeEmptyProperty() {
        return dateGroupIncludeEmptyProperty;
    }

    private boolean groupOrSubgroupIsSearchGroup(GroupTreeNode groupTreeNode) {
        if (groupTreeNode.getGroup() instanceof SearchGroup) {
            return true;
        }
        for (GroupTreeNode child : groupTreeNode.getChildren()) {
            if (groupOrSubgroupIsSearchGroup(child)) {
                return true;
            }
        }
        return false;
    }
}
