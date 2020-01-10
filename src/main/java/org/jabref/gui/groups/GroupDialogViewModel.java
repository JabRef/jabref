package org.jabref.gui.groups;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
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
import org.jabref.preferences.JabRefPreferences;

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

    // Hierarchical Context
    private final BooleanProperty hierarchyIndependentProperty = new SimpleBooleanProperty();
    private final BooleanProperty hierarchyIntersectionProperty = new SimpleBooleanProperty();
    private final BooleanProperty hierarchyUnionProperty = new SimpleBooleanProperty();

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
    private final BooleanProperty keywordGroupRegExpProperty = new SimpleBooleanProperty();

    private final StringProperty searchGroupSearchExpressionProperty = new SimpleStringProperty("");
    private final BooleanProperty searchGroupCaseSensitiveProperty = new SimpleBooleanProperty();
    private final BooleanProperty searchGroupRegExpProperty = new SimpleBooleanProperty();

    private final BooleanProperty autoGroupKeywordsOptionProperty = new SimpleBooleanProperty();
    private final StringProperty autoGroupKeywordsFieldProperty = new SimpleStringProperty("");
    private final StringProperty autoGroupKeywordsDelimiterProperty = new SimpleStringProperty("");
    private final StringProperty autoGroupKeywordsHierarchicalDelimiterProperty = new SimpleStringProperty("");
    private final BooleanProperty autoGroupPersonsOptionProperty = new SimpleBooleanProperty();
    private final StringProperty autoGroupPersonsFieldProperty = new SimpleStringProperty("");

    private final StringProperty texGroupFilePathProperty = new SimpleStringProperty("");

    // Description text
    private final StringProperty hintTextProperty = new SimpleStringProperty("");

    private final Validator nameValidator;
    private final Validator sameNameValidator;
    private final Validator keywordRegexValidator;
    private final Validator searchRegexValidator;

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final BasePanel basePanel;
    private final AbstractGroup editedGroup;

    public GroupDialogViewModel(DialogService dialogService, BasePanel basePanel, JabRefPreferences preferences, AbstractGroup editedGroup) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.basePanel = basePanel;
        this.editedGroup = editedGroup;

        nameValidator = new FunctionBasedValidator<>(
                nameProperty,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Please enter a name for the group.")));

        sameNameValidator = new FunctionBasedValidator<>(
                nameProperty,
                name -> {
                    Optional<GroupTreeNode> rootGroup = basePanel.getBibDatabaseContext().getMetaData().getGroups();
                    if (rootGroup.isPresent()) {
                        int groupsWithSameName = rootGroup.get().findChildrenSatisfying(group -> group.getName().equals(name)).size();
                        if ((editedGroup == null) && (groupsWithSameName > 0)) {
                            // New group but there is already one group with the same name
                            return false;
                        }

                        if ((editedGroup != null) && !editedGroup.getName().equals(name) && (groupsWithSameName > 0)) {
                            // Edit group, changed name to something that is already present
                            return false;
                        }
                    }
                    return true;
                },
                ValidationMessage.error(Localization.lang("There exists already a group with the same name.")));

        keywordRegexValidator = new FunctionBasedValidator<>(
                keywordGroupSearchTermProperty,
                input -> {
                    if (!keywordGroupRegExpProperty.getValue()) {
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
                ValidationMessage.error(String.format("%s > %n %s %n %n %s %n %s",
                        Localization.lang("Dynamically group entries by searching a field for a keyword"),
                        Localization.lang("Keywords"),
                        Localization.lang("Invalid regular expression:"),
                        keywordGroupSearchTermProperty.getValue())));

        searchRegexValidator = new FunctionBasedValidator<>(
                searchGroupSearchExpressionProperty,
                input -> {
                    if (!searchGroupRegExpProperty.getValue()) {
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
                ValidationMessage.error(String.format("%s > %n %s %n %n %s %n %s",
                        Localization.lang("Dynamically group entries by a free-form search expression"),
                        Localization.lang("Keywords"),
                        Localization.lang("Invalid regular expression:"),
                        searchGroupSearchExpressionProperty.getValue())));

        setValues();
    }

    public AbstractGroup resultConverter(ButtonType button) {
        if (button == ButtonType.OK) {
            AbstractGroup resultingGroup = null;
            try {
                String groupName = nameProperty.getValue().trim();
                if (typeExplicitProperty.getValue()) {
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
                } else if (typeKeywordsProperty.getValue()) {
                    if (keywordGroupRegExpProperty.getValue()) {
                        resultingGroup = new RegexKeywordGroup(
                                groupName,
                                getContext(),
                                FieldFactory.parseField(keywordGroupSearchFieldProperty.getValue().trim()),
                                keywordGroupSearchTermProperty.getValue().trim(),
                                keywordGroupCaseSensitiveProperty.getValue());
                    } else {
                        resultingGroup = new WordKeywordGroup(
                                groupName,
                                getContext(),
                                FieldFactory.parseField(keywordGroupSearchFieldProperty.getValue().trim()),
                                keywordGroupSearchTermProperty.getValue().trim(),
                                keywordGroupCaseSensitiveProperty.getValue(),
                                Globals.prefs.getKeywordDelimiter(),
                                false);
                    }
                } else if (typeSearchProperty.getValue()) {
                    resultingGroup = new SearchGroup(
                            groupName,
                            getContext(),
                            searchGroupSearchExpressionProperty.getValue().trim(),
                            searchGroupCaseSensitiveProperty.getValue(),
                            searchGroupRegExpProperty.getValue());
                } else if (typeAutoProperty.getValue()) {
                    if (autoGroupKeywordsOptionProperty.getValue()) {
                        resultingGroup = new AutomaticKeywordGroup(
                                groupName,
                                getContext(),
                                FieldFactory.parseField(autoGroupKeywordsFieldProperty.getValue().trim()),
                                autoGroupKeywordsDelimiterProperty.getValue().charAt(0),
                                autoGroupKeywordsHierarchicalDelimiterProperty.getValue().charAt(0));
                    } else {
                        resultingGroup = new AutomaticPersonsGroup(
                                groupName,
                                getContext(),
                                FieldFactory.parseField(autoGroupPersonsFieldProperty.getValue().trim()));
                    }
                } else if (typeTexProperty.getValue()) {
                    resultingGroup = TexGroup.create(
                            groupName,
                            getContext(),
                            Paths.get(texGroupFilePathProperty.getValue().trim()),
                            new DefaultAuxParser(new BibDatabase()),
                            Globals.getFileUpdateMonitor(),
                            basePanel.getBibDatabaseContext().getMetaData());
                }

                if (resultingGroup != null) {
                    resultingGroup.setColor(colorProperty.getValue());
                    resultingGroup.setDescription(descriptionProperty.getValue());
                    resultingGroup.setIconName(iconProperty.getValue());
                    return resultingGroup;
                } else {
                    return null;
                }

            } catch (IllegalArgumentException | IOException exception) {
                dialogService.showErrorDialogAndWait(exception.getLocalizedMessage(), exception);
                return null;
            }
        }
        return null;
    }

    public void setValues() {
        if (editedGroup == null) {
            // creating new group -> defaults!
            colorProperty.setValue(IconTheme.getDefaultGroupColor());
            typeExplicitProperty.setValue(true);
            setContext(GroupHierarchyType.INDEPENDENT);
        } else {
            nameProperty.setValue(editedGroup.getName());
            colorProperty.setValue(editedGroup.getColor().orElse(IconTheme.getDefaultGroupColor()));
            descriptionProperty.setValue(editedGroup.getDescription().orElse(""));
            iconProperty.setValue(editedGroup.getIconName().orElse(""));
            setContext(editedGroup.getHierarchicalContext());

            if (editedGroup.getClass() == WordKeywordGroup.class) {
                WordKeywordGroup group = (WordKeywordGroup) editedGroup;
                keywordGroupSearchFieldProperty.setValue(group.getSearchField().getName());
                keywordGroupSearchTermProperty.setValue(group.getSearchExpression());
                keywordGroupCaseSensitiveProperty.setValue(group.isCaseSensitive());
                keywordGroupRegExpProperty.setValue(false);

                typeExplicitProperty.setValue(false);
                typeKeywordsProperty.setValue(true);
                typeSearchProperty.setValue(false);
                typeAutoProperty.setValue(false);
                typeTexProperty.setValue(false);
            } else if (editedGroup.getClass() == RegexKeywordGroup.class) {
                RegexKeywordGroup group = (RegexKeywordGroup) editedGroup;
                keywordGroupSearchFieldProperty.setValue(group.getSearchField().getName());
                keywordGroupSearchTermProperty.setValue(group.getSearchExpression());
                keywordGroupCaseSensitiveProperty.setValue(group.isCaseSensitive());
                keywordGroupRegExpProperty.setValue(true);

                typeExplicitProperty.setValue(false);
                typeKeywordsProperty.setValue(true);
                typeSearchProperty.setValue(false);
                typeAutoProperty.setValue(false);
                typeTexProperty.setValue(false);
            } else if (editedGroup.getClass() == SearchGroup.class) {
                SearchGroup group = (SearchGroup) editedGroup;
                searchGroupSearchExpressionProperty.setValue(group.getSearchExpression());
                searchGroupCaseSensitiveProperty.setValue(group.isCaseSensitive());
                searchGroupRegExpProperty.setValue(group.isRegularExpression());

                typeExplicitProperty.setValue(false);
                typeKeywordsProperty.setValue(false);
                typeSearchProperty.setValue(true);
                typeAutoProperty.setValue(false);
                typeTexProperty.setValue(false);
            } else if (editedGroup.getClass() == ExplicitGroup.class) {
                typeExplicitProperty.setValue(true);
                typeKeywordsProperty.setValue(false);
                typeSearchProperty.setValue(false);
                typeAutoProperty.setValue(false);
            } else if (editedGroup instanceof AutomaticGroup) {
                typeExplicitProperty.setValue(false);
                typeKeywordsProperty.setValue(false);
                typeSearchProperty.setValue(false);
                typeAutoProperty.setValue(true);
                typeTexProperty.setValue(false);

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
                typeExplicitProperty.setValue(false);
                typeKeywordsProperty.setValue(false);
                typeSearchProperty.setValue(false);
                typeAutoProperty.setValue(false);
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
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> texGroupFilePathProperty.setValue(relativise(file.toAbsolutePath()).toString()));
    }

    private Path relativise(Path path) {
        List<Path> fileDirectories = getFileDirectoriesAsPaths();
        return FileUtil.relativize(path, fileDirectories);
    }

    private List<Path> getFileDirectoriesAsPaths() {
        List<Path> fileDirs = new ArrayList<>();
        MetaData metaData = basePanel.getBibDatabaseContext().getMetaData();
        metaData.getLaTexFileDirectory(preferences.getFilePreferences().getUser()).ifPresent(fileDirs::add);

        return fileDirs;
    }

    public ValidationStatus validationStatus() {
        CompositeValidator validator = new CompositeValidator();

        validator.addValidators(nameValidator);
        validator.addValidators(sameNameValidator);

        if (typeSearchProperty.getValue()) {
            validator.addValidators(searchRegexValidator);
        }

        if (typeKeywordsProperty.getValue()) {
            validator.addValidators(keywordRegexValidator);
        }

        return validator.getValidationStatus();
    }

    public ValidationStatus nameValidationStatus() { return nameValidator.getValidationStatus(); }

    public ValidationStatus sameNameValidationStatus() { return sameNameValidator.getValidationStatus(); }

    public ValidationStatus searchRegexValidationStatus() { return searchRegexValidator.getValidationStatus(); }

    public ValidationStatus keywordRegexValidationStatus() { return keywordRegexValidator.getValidationStatus(); }

    private GroupHierarchyType getContext() {
        if (hierarchyIndependentProperty.getValue()) {
            return GroupHierarchyType.INDEPENDENT;
        }
        if (hierarchyIntersectionProperty.getValue()) {
            return GroupHierarchyType.REFINING;
        }
        if (hierarchyUnionProperty.getValue()) {
            return GroupHierarchyType.INCLUDING;
        }
        return GroupHierarchyType.INDEPENDENT; // default
    }

    private void setContext(GroupHierarchyType context) {
        switch (context) {
            case INDEPENDENT:
                hierarchyIndependentProperty.setValue(true);
                break;
            case REFINING:
                hierarchyIntersectionProperty.setValue(true);
                break;
            case INCLUDING:
                hierarchyUnionProperty.setValue(true);
                break;
        }
    }

    public StringProperty nameProperty() { return nameProperty; }

    public StringProperty descriptionProperty() { return descriptionProperty; }

    public StringProperty iconProperty() { return iconProperty; }

    public ObjectProperty<Color> colorFieldProperty() { return colorProperty; }

    public BooleanProperty hierarchyIndependentProperty() { return hierarchyIndependentProperty; }

    public BooleanProperty hierarchyIntersectionProperty() { return hierarchyIntersectionProperty; }

    public BooleanProperty hierarchyUnionProperty() { return hierarchyUnionProperty; }

    public BooleanProperty typeExplicitProperty() { return typeExplicitProperty; }

    public BooleanProperty typeKeywordsProperty() { return typeKeywordsProperty; }

    public BooleanProperty typeSearchProperty() { return typeSearchProperty; }

    public BooleanProperty typeAutoProperty() { return typeAutoProperty; }

    public BooleanProperty typeTexProperty() { return typeTexProperty; }

    public StringProperty keywordGroupSearchTermProperty() { return keywordGroupSearchTermProperty; }

    public StringProperty keywordGroupSearchFieldProperty() { return keywordGroupSearchFieldProperty; }

    public BooleanProperty keywordGroupCaseSensitiveProperty() { return keywordGroupCaseSensitiveProperty; }

    public BooleanProperty keywordGroupRegExpProperty() { return keywordGroupRegExpProperty; }

    public StringProperty searchGroupSearchExpressionProperty() { return searchGroupSearchExpressionProperty; }

    public BooleanProperty searchGroupCaseSensitiveProperty() { return searchGroupCaseSensitiveProperty; }

    public BooleanProperty searchGroupRegExpProperty() { return searchGroupRegExpProperty; }

    public BooleanProperty autoGroupKeywordsOptionProperty() { return autoGroupKeywordsOptionProperty; }

    public StringProperty autoGroupKeywordsFieldProperty() { return autoGroupKeywordsFieldProperty; }

    public StringProperty autoGroupKeywordsDeliminatorProperty() { return autoGroupKeywordsDelimiterProperty; }

    public StringProperty autoGroupKeywordsHierarchicalDeliminatorProperty() { return autoGroupKeywordsHierarchicalDelimiterProperty; }

    public BooleanProperty autoGroupPersonsOptionProperty() { return autoGroupPersonsOptionProperty; }

    public StringProperty autoGroupPersonsFieldProperty() { return autoGroupPersonsFieldProperty; }

    public StringProperty texGroupFilePathProperty() { return texGroupFilePathProperty; }

    public StringProperty hintTextProperty() { return hintTextProperty; }
}
