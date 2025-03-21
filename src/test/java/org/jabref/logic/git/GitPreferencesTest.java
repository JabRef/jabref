package org.jabref.logic.git;

import javafx.beans.property.BooleanProperty;

import org.jabref.logic.preferences.AutoPushMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class GitPreferencesTest {

    private GitPreferences gitPreferences;

    @BeforeEach
    void setUp() {
        gitPreferences = new GitPreferences(true, AutoPushMode.ON_SAVE, "", "");
    }

    @Test
    void constructorInitialisesValues() {
        assertThat(gitPreferences.getAutoPushEnabled()).isTrue();
        assertThat(gitPreferences.getAutoPushMode()).isEqualTo(AutoPushMode.ON_SAVE);
    }

    @Test
    void gettersReturnCorrectValues() {
        assertThat(gitPreferences.getAutoPushEnabled()).isTrue();
        assertThat(gitPreferences.getAutoPushMode()).isEqualTo(AutoPushMode.ON_SAVE);
    }

    @Test
    void setAutoPushEnabledUpdatesValue() {
        gitPreferences.setAutoPushEnabled(false);
        assertThat(gitPreferences.getAutoPushEnabled()).isFalse();
    }

    @Test
    void javaFXBooleanPropertyUpdates() {
        BooleanProperty autoPushProperty = gitPreferences.getAutoPushEnabledProperty();
        autoPushProperty.set(false);
        assertThat(gitPreferences.getAutoPushEnabled()).isFalse();
    }

    @Test
    void autoPushModeFromString() {
        assertThat(AutoPushMode.fromString("On Save")).isEqualTo(AutoPushMode.ON_SAVE);
    }
}
