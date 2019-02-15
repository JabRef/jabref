package org.jabref.gui.metadata;

import java.util.regex.Pattern;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.l10n.Localization;

import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class BibtexStringViewModel {

    private final static Pattern IS_NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    private final StringProperty label = new SimpleStringProperty();
    private final StringProperty content = new SimpleStringProperty();

    private final Validator labelValidator;
    private final Validator contentValidator;
    private final CompositeValidator combinedValidator;

    public BibtexStringViewModel(String label, String content) {
        this.label.setValue(label);
        this.content.setValue(content);

        labelValidator = new FunctionBasedValidator<>(this.label, BibtexStringViewModel::validateLabel);
        contentValidator = new FunctionBasedValidator<>(this.content, BibtexStringViewModel::validateContent);
        combinedValidator = new CompositeValidator(labelValidator, contentValidator);
    }

    public Validator getlabelValidator() {
        return labelValidator;
    }

    public Validator getContentValidator() {
        return contentValidator;
    }

    public ValidationStatus labelValidation() {
        return labelValidator.getValidationStatus();
    }

    public ValidationStatus contentValidation() {
        return contentValidator.getValidationStatus();
    }

    public ValidationStatus combinedValidation() {
        return combinedValidator.getValidationStatus();
    }

    public ReadOnlyBooleanProperty combinedValidationValidProperty() {
        return combinedValidator.getValidationStatus().validProperty();
    }

    public StringProperty getLabel() {
        return label;
    }

    public StringProperty getContent() {
        return content;
    }

    public void setLabel(String label) {
        this.label.setValue(label);
    }

    public void setContent(String content) {
        this.content.setValue(content);
    }

    private static ValidationMessage validateLabel(String input) {
        if (input == null) {
            return ValidationMessage.error("May not be null");
        } else if (input.trim().isEmpty()) {
            return ValidationMessage.error(Localization.lang("Please enter the string's label"));
        } else if (IS_NUMBER.matcher(input).matches()) {
            return ValidationMessage.error(Localization.lang("The label of the string cannot be a number."));
        } else if (input.contains("#")) {
            return ValidationMessage.error(Localization.lang("The label of the string cannot contain the '#' character."));
        } else if (input.contains(" ")) {
            return ValidationMessage.error(Localization.lang("The label of the string cannot contain spaces."));
        } else {
            return null; // everything is ok
        }
    }

    private static ValidationMessage validateContent(String input) {

        if (input == null) {
            return ValidationMessage.error(Localization.lang("Must not be empty!"));
        } else if (input.trim().isEmpty()) {
            return ValidationMessage.error(Localization.lang("Must not be empty!"));
        } else {
            return null; // everything is ok
        }
    }
}
