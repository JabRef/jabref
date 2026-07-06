package org.jabref.gui.libraryproperties.constants;

import java.util.Optional;
import java.util.regex.Pattern;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;

import org.jabref.gui.validation.ValidationConstraints;
import org.jabref.gui.validation.ValidationMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

import org.jfxcore.validation.property.ConstrainedStringProperty;
import org.jfxcore.validation.property.SimpleConstrainedStringProperty;

public class ConstantsItemModel {

    private final static Pattern IS_NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    private final ConstrainedStringProperty<ValidationMessage> labelProperty;
    private final ConstrainedStringProperty<ValidationMessage> contentProperty;

    public ConstantsItemModel(String label, String content) {
        labelProperty = new SimpleConstrainedStringProperty<>(label,
                ValidationConstraints.function(ConstantsItemModel::validateLabel));
        contentProperty = new SimpleConstrainedStringProperty<>(content,
                ValidationConstraints.function(ConstantsItemModel::validateContent));
    }

    public BooleanBinding combinedValidationValidProperty() {
        return Bindings.and(labelProperty.validProperty(), contentProperty.validProperty());
    }

    public ConstrainedStringProperty<ValidationMessage> labelProperty() {
        return this.labelProperty;
    }

    public ConstrainedStringProperty<ValidationMessage> contentProperty() {
        return this.contentProperty;
    }

    public void setLabel(String label) {
        this.labelProperty.setValue(label);
    }

    public void setContent(String content) {
        this.contentProperty.setValue(content);
    }

    private static Optional<ValidationMessage> validateLabel(String input) {
        if (input == null) {
            return Optional.of(ValidationMessage.error("May not be null"));
        } else if (input.isBlank()) {
            return Optional.of(ValidationMessage.error(Localization.lang("Please enter the string's label")));
        } else if (IS_NUMBER.matcher(input).matches()) {
            return Optional.of(ValidationMessage.error(Localization.lang("The label of the string cannot be a number.")));
        } else if (input.contains("#")) {
            return Optional.of(ValidationMessage.error(Localization.lang("The label of the string cannot contain the '#' character.")));
        } else if (input.contains(" ")) {
            return Optional.of(ValidationMessage.error(Localization.lang("The label of the string cannot contain spaces.")));
        } else {
            return Optional.empty(); // everything is ok
        }
    }

    private static Optional<ValidationMessage> validateContent(String input) {
        if (StringUtil.isBlank(input)) {
            return Optional.of(ValidationMessage.error(Localization.lang("Must not be empty!")));
        } else {
            return Optional.empty(); // everything is ok
        }
    }
}
