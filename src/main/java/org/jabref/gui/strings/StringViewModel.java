package org.jabref.gui.strings;

import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class StringViewModel {

    private final StringProperty label = new SimpleStringProperty();
    private final StringProperty content = new SimpleStringProperty();

    private final Validator labelValidator;

    private final Function<String, ValidationMessage> function = input -> {
        if (input == null) {
            return ValidationMessage.error("May not be null2");
        } else if (input.trim().isEmpty()) {
            return ValidationMessage.warning("Should not be empty");
        } else {
            return null; // everything is ok
        }
    };

    public StringViewModel(String label, String content) {

        this.label.setValue(label);
        this.content.setValue(content);

        labelValidator = new FunctionBasedValidator<>(this.label, function);
    }

    public ValidationStatus labelValidation() {
        return labelValidator.getValidationStatus();
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
}
