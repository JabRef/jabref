package org.jabref.gui.preferences.externalfiletypes;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;

import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class EditExternalFileTypeViewModel {
    private final ExternalFileTypeItemViewModel fileTypeViewModel;
    private final StringProperty nameProperty = new SimpleStringProperty("");
    private final StringProperty mimeTypeProperty = new SimpleStringProperty("");
    private final StringProperty extensionProperty = new SimpleStringProperty("");
    private final StringProperty selectedApplicationProperty = new SimpleStringProperty("");
    private final BooleanProperty defaultApplicationSelectedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty customApplicationSelectedProperty = new SimpleBooleanProperty(false);
    private final ObservableList<ExternalFileTypeItemViewModel> fileTypes;
    private final String originalExtension;
    private final String originalName;
    private final String originalMimeType;
    private CompositeValidator extensionValidator;
    private CompositeValidator nameValidator;
    private CompositeValidator mimeTypeValidator;
    private CompositeValidator validator;

    public EditExternalFileTypeViewModel(ExternalFileTypeItemViewModel fileTypeViewModel, ObservableList<ExternalFileTypeItemViewModel> fileTypes) {
        this.fileTypeViewModel = fileTypeViewModel;
        this.fileTypes = fileTypes;
        this.originalExtension = fileTypeViewModel.extensionProperty().getValue();
        this.originalName = fileTypeViewModel.nameProperty().getValue();
        this.originalMimeType = fileTypeViewModel.mimetypeProperty().getValue();
        extensionProperty.setValue(fileTypeViewModel.extensionProperty().getValue());
        nameProperty.setValue(fileTypeViewModel.nameProperty().getValue());
        mimeTypeProperty.setValue(fileTypeViewModel.mimetypeProperty().getValue());

        if (fileTypeViewModel.applicationProperty().getValue().isEmpty()) {
            defaultApplicationSelectedProperty.setValue(true);
        } else {
            customApplicationSelectedProperty.setValue(true);
            selectedApplicationProperty.setValue(fileTypeViewModel.applicationProperty().getValue());
        }

        setupValidation();
    }

    private void setupValidation() {
        validator = new CompositeValidator();
        extensionValidator = new CompositeValidator();

        Validator extensionisNotBlankValidator = new FunctionBasedValidator<>(
                extensionProperty,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Please enter a name for the extension."))
        );
        Validator sameExtensionValidator = new FunctionBasedValidator<>(
                extensionProperty,
                extension -> {
                    for (ExternalFileTypeItemViewModel fileTypeItem : fileTypes) {
                        if (extension.equalsIgnoreCase(fileTypeItem.extensionProperty().get()) && !extension.equalsIgnoreCase(originalExtension)) {
                            return false;
                        }
                    }
                    return true;
                },
                ValidationMessage.error(Localization.lang("There already exists an external file type with the same extension"))
        );
        extensionValidator.addValidators(sameExtensionValidator, extensionisNotBlankValidator);

        nameValidator = new CompositeValidator();
        Validator sameNameValidator = new FunctionBasedValidator<>(
                nameProperty,
                name -> {
                    for (ExternalFileTypeItemViewModel fileTypeItem : fileTypes) {
                        if (name.equalsIgnoreCase(fileTypeItem.nameProperty().get()) && !name.equalsIgnoreCase(originalName)) {
                            return false;
                        }
                    }
                    return true;
                },
                ValidationMessage.error(Localization.lang("There already exists an external file type with the same name"))
        );

        Validator nameIsNotBlankValidator = new FunctionBasedValidator<>(
                nameProperty,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Please enter a name."))
        );
        nameValidator.addValidators(sameNameValidator, nameIsNotBlankValidator);

        mimeTypeValidator = new CompositeValidator();
        Validator mimeTypeIsNotBlankValidator = new FunctionBasedValidator<>(
                mimeTypeProperty,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Please enter a name for the MIME type."))
        );

        Validator sameMimeTypeValidator = new FunctionBasedValidator<>(
                mimeTypeProperty,
                mimeType -> {
                    for (ExternalFileTypeItemViewModel fileTypeItem : fileTypes) {
                        if (mimeType.equalsIgnoreCase(fileTypeItem.mimetypeProperty().get()) && !mimeType.equalsIgnoreCase(originalMimeType)) {
                            return false;
                        }
                    }
                    return true;
                },
                ValidationMessage.error(Localization.lang("There already exists an external file type with the same MIME type"))
        );
        mimeTypeValidator.addValidators(sameMimeTypeValidator, mimeTypeIsNotBlankValidator);

        validator.addValidators(extensionValidator, sameExtensionValidator, nameValidator, sameNameValidator, mimeTypeValidator, sameMimeTypeValidator);
    }

    public ValidationStatus validationStatus() {
        return validator.getValidationStatus();
    }

    public ValidationStatus extensionValidation() {
        return extensionValidator.getValidationStatus();
    }

    public ValidationStatus mimeTypeValidation() {
        return mimeTypeValidator.getValidationStatus();
    }

    public ValidationStatus nameValidation() {
        return nameValidator.getValidationStatus();
    }

    public Node getIcon() {
        return fileTypeViewModel.iconProperty().getValue().getGraphicNode();
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public StringProperty extensionProperty() {
        return extensionProperty;
    }

    public StringProperty mimeTypeProperty() {
        return mimeTypeProperty;
    }

    public StringProperty selectedApplicationProperty() {
        return selectedApplicationProperty;
    }

    public BooleanProperty defaultApplicationSelectedProperty() {
        return defaultApplicationSelectedProperty;
    }

    public BooleanProperty customApplicationSelectedProperty() {
        return customApplicationSelectedProperty;
    }

    public BooleanProperty validExtensionTypeProperty() {
        return defaultApplicationSelectedProperty;
    }

    public void storeSettings() {
        fileTypeViewModel.nameProperty().setValue(nameProperty.getValue().trim());
        fileTypeViewModel.mimetypeProperty().setValue(mimeTypeProperty.getValue().trim());

        String ext = extensionProperty.getValue().trim();
        if (!ext.isEmpty() && (ext.charAt(0) == '.')) {
            fileTypeViewModel.extensionProperty().setValue(ext.substring(1));
        } else {
            fileTypeViewModel.extensionProperty().setValue(ext);
        }

        String application = selectedApplicationProperty.getValue().trim();

        // store application as empty if the "Default" option is selected, or if the application name is empty:
        if (defaultApplicationSelectedProperty.getValue() || application.isEmpty()) {
            fileTypeViewModel.applicationProperty().setValue("");
            selectedApplicationProperty.setValue("");
        } else {
            fileTypeViewModel.applicationProperty().setValue(application);
        }
    }
}
