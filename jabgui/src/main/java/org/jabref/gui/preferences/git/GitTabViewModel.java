package org.jabref.gui.preferences.git;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class GitTabViewModel implements PreferenceTabViewModel {
    private final StringProperty usernameProperty = new SimpleStringProperty("");
    private final StringProperty patProperty = new SimpleStringProperty("");
    private final BooleanProperty persistPatProperty = new SimpleBooleanProperty();
    private final BooleanProperty passwordPersistAvailable = new SimpleBooleanProperty();
    private final StringProperty pullIntervalInMinutesProperty = new SimpleStringProperty("");

    private final GitPreferences gitPreferences;

    private final Validator pullIntervalValidator;

    public GitTabViewModel(GitPreferences gitPreferences) {
        this.gitPreferences = gitPreferences;

        pullIntervalValidator = new FunctionBasedValidator<>(
                pullIntervalInMinutesProperty,
                input -> getIntervalAsInt(input).filter(interval -> interval > 0).isPresent(),
                ValidationMessage.error("%s > %s %n %n %s".formatted(
                        Localization.lang("Git"),
                        Localization.lang("Automatic synchronization"),
                        Localization.lang("You must enter a positive integer value"))));
    }

    @Override
    public void setValues() {
        usernameProperty.setValue(gitPreferences.getUsername());
        patProperty.setValue(gitPreferences.getPat());
        persistPatProperty.setValue(gitPreferences.getPersistPat());
        passwordPersistAvailable.setValue(OS.isKeyringAvailable());
        pullIntervalInMinutesProperty.setValue(String.valueOf(gitPreferences.getPullIntervalInMinutes()));
    }

    @Override
    public void storeSettings() {
        gitPreferences.setUsername(usernameProperty.getValue().trim());
        gitPreferences.setPersistPat(persistPatProperty.getValue());
        gitPreferences.setPat(patProperty.getValue().trim());
        gitPreferences.setPullIntervalInMinutes(Integer.parseInt(pullIntervalInMinutesProperty.getValue().trim()));
    }

    @Override
    public boolean validateSettings() {
        return pullIntervalValidator.getValidationStatus().isValid();
    }

    private Optional<Integer> getIntervalAsInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public StringProperty usernameProperty() {
        return usernameProperty;
    }

    public StringProperty patProperty() {
        return patProperty;
    }

    public BooleanProperty persistPatProperty() {
        return persistPatProperty;
    }

    public ReadOnlyBooleanProperty passwordPersistAvailable() {
        return passwordPersistAvailable;
    }

    public StringProperty pullIntervalInMinutesProperty() {
        return pullIntervalInMinutesProperty;
    }

    public ValidationStatus pullIntervalValidationStatus() {
        return pullIntervalValidator.getValidationStatus();
    }
}
