package org.jabref.gui.preferences.externalfiletypes;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import org.jabref.gui.validation.ValidationConstraints;
import org.jabref.gui.validation.ValidationMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

import org.jfxcore.validation.property.ConstrainedStringProperty;
import org.jfxcore.validation.property.SimpleConstrainedStringProperty;

public class EditExternalFileTypeViewModel {
    private final ExternalFileTypeItemViewModel fileTypeViewModel;
    private ConstrainedStringProperty<ValidationMessage> nameProperty;
    private ConstrainedStringProperty<ValidationMessage> mimeTypeProperty;
    private ConstrainedStringProperty<ValidationMessage> extensionProperty;
    private final StringProperty selectedApplicationProperty = new SimpleStringProperty("");
    private final BooleanProperty defaultApplicationSelectedProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty customApplicationSelectedProperty = new SimpleBooleanProperty(false);
    private final ObservableList<ExternalFileTypeItemViewModel> fileTypes;
    private final String originalExtension;
    private final String originalName;
    private final String originalMimeType;

    public EditExternalFileTypeViewModel(ExternalFileTypeItemViewModel fileTypeViewModel, ObservableList<ExternalFileTypeItemViewModel> fileTypes) {
        this.fileTypeViewModel = fileTypeViewModel;
        this.fileTypes = fileTypes;
        this.originalExtension = fileTypeViewModel.extensionProperty().getValue();
        this.originalName = fileTypeViewModel.nameProperty().getValue();
        this.originalMimeType = fileTypeViewModel.mimetypeProperty().getValue();

        setupValidation();

        if (fileTypeViewModel.applicationProperty().getValue().isEmpty()) {
            defaultApplicationSelectedProperty.setValue(true);
        } else {
            customApplicationSelectedProperty.setValue(true);
            selectedApplicationProperty.setValue(fileTypeViewModel.applicationProperty().getValue());
        }
    }

    private void setupValidation() {
        extensionProperty = new SimpleConstrainedStringProperty<>(
                fileTypeViewModel.extensionProperty().getValue(),
                ValidationConstraints.predicate(
                        StringUtil::isNotBlank,
                        ValidationMessage.error(Localization.lang("Please enter a name for the extension."))),
                ValidationConstraints.predicate(
                        extension -> {
                            for (ExternalFileTypeItemViewModel fileTypeItem : fileTypes) {
                                if (extension.equalsIgnoreCase(fileTypeItem.extensionProperty().get()) && !extension.equalsIgnoreCase(originalExtension)) {
                                    return false;
                                }
                            }
                            return true;
                        },
                        ValidationMessage.error(Localization.lang("There already exists an external file type with the same extension"))));

        nameProperty = new SimpleConstrainedStringProperty<>(
                fileTypeViewModel.nameProperty().getValue(),
                ValidationConstraints.predicate(
                        StringUtil::isNotBlank,
                        ValidationMessage.error(Localization.lang("Please enter a name."))),
                ValidationConstraints.predicate(
                        name -> {
                            for (ExternalFileTypeItemViewModel fileTypeItem : fileTypes) {
                                if (name.equalsIgnoreCase(fileTypeItem.nameProperty().get()) && !name.equalsIgnoreCase(originalName)) {
                                    return false;
                                }
                            }
                            return true;
                        },
                        ValidationMessage.error(Localization.lang("There already exists an external file type with the same name"))));

        mimeTypeProperty = new SimpleConstrainedStringProperty<>(
                fileTypeViewModel.mimetypeProperty().getValue(),
                ValidationConstraints.predicate(
                        StringUtil::isNotBlank,
                        ValidationMessage.error(Localization.lang("Please enter a name for the MIME type."))),
                ValidationConstraints.predicate(
                        mimeType -> {
                            for (ExternalFileTypeItemViewModel fileTypeItem : fileTypes) {
                                if (mimeType.equalsIgnoreCase(fileTypeItem.mimetypeProperty().get()) && !mimeType.equalsIgnoreCase(originalMimeType)) {
                                    return false;
                                }
                            }
                            return true;
                        },
                        ValidationMessage.error(Localization.lang("There already exists an external file type with the same MIME type"))));
    }

    public BooleanBinding validProperty() {
        return Bindings.and(extensionProperty.validProperty(),
                Bindings.and(nameProperty.validProperty(), mimeTypeProperty.validProperty()));
    }

    public Node getIcon() {
        return fileTypeViewModel.iconProperty().getValue().getGraphicNode();
    }

    public ConstrainedStringProperty<ValidationMessage> nameProperty() {
        return nameProperty;
    }

    public ConstrainedStringProperty<ValidationMessage> extensionProperty() {
        return extensionProperty;
    }

    public ConstrainedStringProperty<ValidationMessage> mimeTypeProperty() {
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
