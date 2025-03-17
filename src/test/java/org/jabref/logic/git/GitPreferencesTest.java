package org.jabref.logic.git;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;

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
    void testConstructorInitialisesValues() {
        assertThat(gitPreferences.getAutoPushEnabled()).isTrue();
        assertThat(gitPreferences.getAutoPushMode()).isEqualTo(AutoPushMode.ON_SAVE);
    }

    @Test
    void testGettersReturnCorrectValues() {
        assertThat(gitPreferences.getAutoPushEnabled()).isTrue();
        assertThat(gitPreferences.getAutoPushMode()).isEqualTo(AutoPushMode.ON_SAVE);
    }

    @Test
    void testSetAutoPushEnabledUpdatesValue() {
        gitPreferences.setAutoPushEnabled(false);
        assertThat(gitPreferences.getAutoPushEnabled()).isFalse();
    }

    @Test
    void testJavaFXBooleanPropertyUpdates() {
        BooleanProperty autoPushProperty = gitPreferences.getAutoPushEnabledProperty();
        autoPushProperty.set(false);
        assertThat(gitPreferences.getAutoPushEnabled()).isFalse();
    }

    @Test
    void testAutoPushModeUpdates() {
        ObjectProperty<AutoPushMode> autoPushModeProperty = gitPreferences.getAutoPushModeProperty();
        autoPushModeProperty.set(AutoPushMode.MANUALLY);
        assertThat(gitPreferences.getAutoPushMode()).isEqualTo(AutoPushMode.MANUALLY);
    }

    @Test
    void testAutoPushModeFromString() {
        assertThat(AutoPushMode.fromString("On Save")).isEqualTo(AutoPushMode.ON_SAVE);
        assertThat(AutoPushMode.fromString("Manually")).isEqualTo(AutoPushMode.MANUALLY);
        assertThat(AutoPushMode.fromString("invalid")).isEqualTo(AutoPushMode.MANUALLY);
    }
}
