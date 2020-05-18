package org.jabref.gui.groups;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class GroupDialogViewModel {
    // Basic Settings
    private final StringProperty nameProperty = new SimpleStringProperty("");
    private final StringProperty descriptionProperty = new SimpleStringProperty("");
    private final StringProperty iconProperty = new SimpleStringProperty("");
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
    private final BooleanProperty searchGroupCaseSensitiveProperty = new SimpleBooleanProperty();
    private final BooleanProperty searchGroupRegexProperty = new SimpleBooleanProperty();

    private final BooleanProperty autoGroupKeywordsOptionProperty = new SimpleBooleanProperty();
    private final StringProperty autoGroupKeywordsFieldProperty = new SimpleStringProperty("");
    private final StringProperty autoGroupKeywordsDelimiterProperty = new SimpleStringProperty("");
    private final StringProperty autoGroupKeywordsHierarchicalDelimiterProperty = new SimpleStringProperty("");
    private final BooleanProperty autoGroupPersonsOptionProperty = new SimpleBooleanProperty();
    private final StringProperty autoGroupPersonsFieldProperty = new SimpleStringProperty("");

    private final StringProperty texGroupFilePathProperty = new SimpleStringProperty("");

    private Validator nameValidator;
    private Validator nameContainsDelimiterValidator;
    private Validator sameNameValidator;
    private Validator keywordRegexValidator;
    private Validator keywordFieldEmptyValidator;
    private Validator keywordSearchTermEmptyValidator;
    private Validator searchRegexValidator;
    private Validator searchSearchTermEmptyValidator;
    private final CompositeValidator validator = new CompositeValidator();

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final BibDatabaseContext currentDatabase;
    private final AbstractGroup editedGroup;

    public GroupDialogViewModel(DialogService dialogService, BibDatabaseContext currentDatabase, PreferencesService preferencesService, AbstractGroup editedGroup) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.currentDatabase = currentDatabase;
        this.editedGroup = editedGroup;

        setupValidation();
        setValues();
    }

    private void setupValidation() {
        nameValidator = new FunctionBasedValidator<>(
                nameProperty,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Please enter a name for the group.")));

        nameContainsDelimiterValidator = new FunctionBasedValidator<>(
                nameProperty,
                name -> !name.contains(Character.toString(preferencesService.getKeywordDelimiter())),
                ValidationMessage.warning(
                        Localization.lang(
                                "The group name contains the keyword separator \"%0\" and thus probably does not work as expected.",
                                Character.toString(preferencesService.getKeywordDelimiter())
                        )));

        sameNameValidator = new FunctionBasedValidator<>(
                nameProperty,
                name -> {
                    Optional<GroupTreeNode> rootGroup = currentDatabase.getMetaData().getGroups();
                    if (rootGroup.isPresent()) {
                        int groupsWithSameName = rootGroup.get().findChildrenSatisfying(group -> group.getName().equals(name)).size();
                        if ((editedGroup == null) && (groupsWithSameName > 0)) {
                            // New group but there is already one group with the same name
                            return false;
                        }

                        // Edit group, changed name to something that is already present
                        return (editedGroup == null) || editedGroup.getName().equals(name) || (groupsWithSameName <= 0);
                    }
                    return true;
                },
                ValidationMessage.error(Localization.lang("There exists already a group with the same name.")));

        keywordRegexValidator = new FunctionBasedValidator<>(
                keywordGroupSearchTermProperty,
                input -> {
                    if (!keywordGroupRegexProperty.getValue()) {
                        return true;
                    }

                    if (StringUtil.isNullOrEmpty(input)) {
                        return false;
                    }

                    try {
                        Pattern.compile(input);
                        return true;
                    } catch (PatternSyntaxException ignored) {
                        return false;
                    }
                },
                ValidationMessage.error(String.format("%s > %n %s %n %n %s",
                        Localization.lang("Searching for keywords"),
                        Localization.lang("Keywords"),
                        Localization.lang("Invalid regular expression."))));

        keywordFieldEmptyValidator = new FunctionBasedValidator<>(
                keywordGroupSearchFieldProperty,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Please enter a field name to search for keywords.")));

        keywordSearchTermEmptyValidator = new FunctionBasedValidator<>(
                keywordGroupSearchTermProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error(String.format("%s > %n %s %n %n %s",
                        Localization.lang("Searching for keywords"),
                        Localization.lang("Keywords"),
                        Localization.lang("Search term is empty.")
                )));

        searchRegexValidator = new FunctionBasedValidator<>(
                searchGroupSearchTermProperty,
                input -> {
                    if (!searchGroupRegexProperty.getValue()) {
                        return true;
                    }

                    if (StringUtil.isNullOrEmpty(input)) {
                        return false;
                    }

                    try {
                        Pattern.compile(input);
                        return true;
                    } catch (PatternSyntaxException ignored) {
                        return false;
                    }
                },
                ValidationMessage.error(String.format("%s > %n %s",
                        Localization.lang("Free search expression"),
                        Localization.lang("Invalid regular expression."))));

        searchSearchTermEmptyValidator = new FunctionBasedValidator<>(
                searchGroupSearchTermProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error(String.format("%s > %n %s",
                        Localization.lang("Free search expression"),
                        Localization.lang("Search term is empty.")
                )));

        validator.addValidators(nameValidator, sameNameValidator);

        typeSearchProperty.addListener((obs, _oldValue, isSelected) -> {
            if (isSelected) {
                validator.addValidators(searchRegexValidator, searchSearchTermEmptyValidator);
            } else {
                validator.removeValidators(searchRegexValidator, searchSearchTermEmptyValidator);
            }
        });

        typeKeywordsProperty.addListener((obs, _oldValue, isSelected) -> {
            if (isSelected) {
                validator.addValidators(keywordFieldEmptyValidator, keywordRegexValidator, keywordSearchTermEmptyValidator);
            } else {
                validator.removeValidators(keywordFieldEmptyValidator, keywordRegexValidator, keywordSearchTermEmptyValidator);
            }
        });
    }

    public void validationHandler(Event event) {
        ValidationStatus validationStatus = validator.getValidationStatus();
        if (validationStatus.getHighestMessage().isPresent()) {
            dialogService.showErrorDialogAndWait(validationStatus.getHighestMessage().get().getMessage());
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
            if (typeExplicitProperty.getValue()) {
                resultingGroup = new ExplicitGroup(
                        groupName,
                        groupHierarchySelectedProperty.getValue(),
                        preferencesService.getKeywordDelimiter());
            } else if (typeKeywordsProperty.getValue()) {
                if (keywordGroupRegexProperty.getValue()) {
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
                            preferencesService.getKeywordDelimiter(),
                            false);
                }
            } else if (typeSearchProperty.getValue()) {
                resultingGroup = new SearchGroup(
                        groupName,
                        groupHierarchySelectedProperty.getValue(),
                        searchGroupSearchTermProperty.getValue().trim(),
                        searchGroupCaseSensitiveProperty.getValue(),
                        searchGroupRegexProperty.getValue());
            } else if (typeAutoProperty.getValue()) {
                if (autoGroupKeywordsOptionProperty.getValue()) {
                    resultingGroup = new AutomaticKeywordGroup(
                            groupName,
                            groupHierarchySelectedProperty.getValue(),
                            FieldFactory.parseField(autoGroupKeywordsFieldProperty.getValue().trim()),
                            autoGroupKeywordsDelimiterProperty.getValue().charAt(0),
                            autoGroupKeywordsHierarchicalDelimiterProperty.getValue().charAt(0));
                } else {
                    resultingGroup = new AutomaticPersonsGroup(
                            groupName,
                            groupHierarchySelectedProperty.getValue(),
                            FieldFactory.parseField(autoGroupPersonsFieldProperty.getValue().trim()));
                }
            } else if (typeTexProperty.getValue()) {
                resultingGroup = TexGroup.create(
                        groupName,
                        groupHierarchySelectedProperty.getValue(),
                        Path.of(texGroupFilePathProperty.getValue().trim()),
                        new DefaultAuxParser(new BibDatabase()),
                        Globals.getFileUpdateMonitor(),
                        currentDatabase.getMetaData());
            }

            if (resultingGroup != null) {
                resultingGroup.setColor(colorProperty.getValue());
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
            colorProperty.setValue(IconTheme.getDefaultGroupColor());
            typeExplicitProperty.setValue(true);
            groupHierarchySelectedProperty.setValue(GroupHierarchyType.INDEPENDENT);
        } else {
            nameProperty.setValue(editedGroup.getName());
            colorProperty.setValue(editedGroup.getColor().orElse(IconTheme.getDefaultGroupColor()));
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
                searchGroupCaseSensitiveProperty.setValue(group.isCaseSensitive());
                searchGroupRegexProperty.setValue(group.isRegularExpression());
            } else if (editedGroup.getClass() == ExplicitGroup.class) {
                typeExplicitProperty.setValue(true);
            } else if (editedGroup instanceof AutomaticGroup) {
                typeAutoProperty.setValue(true);

                if (editedGroup.getClass() == AutomaticKeywordGroup.class) {
                    AutomaticKeywordGroup group = (AutomaticKeywordGroup) editedGroup;
                    autoGroupKeywordsDelimiterProperty.setValue(group.getKeywordDelimiter().toString());
                    autoGroupKeywordsHierarchicalDelimiterProperty.setValue(group.getKeywordHierarchicalDelimiter().toString());
                    autoGroupKeywordsFieldProperty.setValue(group.getField().getName());
                } else if (editedGroup.getClass() == AutomaticPersonsGroup.class) {
                    AutomaticPersonsGroup group = (AutomaticPersonsGroup) editedGroup;
                    autoGroupPersonsFieldProperty.setValue(group.getField().getName());
                }
            } else if (editedGroup.getClass() == TexGroup.class) {
                typeTexProperty.setValue(true);

                TexGroup group = (TexGroup) editedGroup;
                texGroupFilePathProperty.setValue(group.getFilePath().toString());
            }
        }
    }

    public void texGroupBrowse() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.AUX)
                .withDefaultExtension(StandardFileType.AUX)
                .withInitialDirectory(preferencesService.getWorkingDir()).build();
        dialogService.showFileOpenDialog(fileDialogConfiguration)
                     .ifPresent(file -> texGroupFilePathProperty.setValue(
                             FileUtil.relativize(file.toAbsolutePath(), getFileDirectoriesAsPaths()).toString()
                     ));
    }

    public void openHelpPage() {
        HelpAction.openHelpPage(HelpFile.GROUPS);
    }

    private List<Path> getFileDirectoriesAsPaths() {
        List<Path> fileDirs = new ArrayList<>();
        MetaData metaData = currentDatabase.getMetaData();
        metaData.getLatexFileDirectory(preferencesService.getFilePreferences().getUser()).ifPresent(fileDirs::add);

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

    public ValidationStatus searchRegexValidationStatus() {
        return searchRegexValidator.getValidationStatus();
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

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public StringProperty descriptionProperty() {
        return descriptionProperty;
    }

    public StringProperty iconProperty() {
        return iconProperty;
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

    public BooleanProperty searchGroupCaseSensitiveProperty() {
        return searchGroupCaseSensitiveProperty;
    }

    public BooleanProperty searchGroupRegexProperty() {
        return searchGroupRegexProperty;
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
}
