package org.jabref.gui.preferences.externalfiletypes;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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


    private Validator extensionValidator;
    private Validator sameExtensionValidator;
    private CompositeValidator validator;

    public EditExternalFileTypeViewModel(ExternalFileTypeItemViewModel fileTypeViewModel) {
        this.fileTypeViewModel = fileTypeViewModel;

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
        extensionValidator = new FunctionBasedValidator<>(
                extensionProperty,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Please enter a name for the extension.")));

        validator.addValidators(extensionValidator);
    }

    public ValidationStatus validationStatus() {
        return validator.getValidationStatus();
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
