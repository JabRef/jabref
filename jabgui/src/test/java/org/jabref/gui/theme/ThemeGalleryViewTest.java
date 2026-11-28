package org.jabref.gui.theme;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ToggleButton;

import org.jabref.gui.testutils.JavaFxExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(JavaFxExtension.class)
class ThemeGalleryViewTest {

    private final ObjectProperty<ThemePreset> theme = new SimpleObjectProperty<>(ThemePreset.JABREF);
    private final BooleanProperty customThemeEnabled = new SimpleBooleanProperty(false);
    private ThemeGalleryView gallery;

    @BeforeEach
    void setUp() {
        gallery = new ThemeGalleryView();
        gallery.bind(List.of(ThemePreset.JABREF, ThemePreset.NORD), theme, customThemeEnabled,
                new SimpleObjectProperty<>(ThemeColorScheme.LIGHT));
    }

    private ToggleButton card(int index) {
        return (ToggleButton) gallery.getChildren().get(index);
    }

    @Test
    void selectedCardFollowsTheTheme() {
        theme.set(ThemePreset.NORD);

        assertTrue(card(1).isSelected());
        assertFalse(card(0).isSelected());
    }

    @Test
    void clickOnCardSelectsItsTheme() {
        customThemeEnabled.set(true);

        card(1).fire();

        assertEquals(ThemePreset.NORD, theme.get());
        assertFalse(customThemeEnabled.get());
    }

    @Test
    void clickOnCustomCardEnablesTheCustomTheme() {
        card(2).fire();

        assertTrue(customThemeEnabled.get());
        assertEquals(ThemePreset.JABREF, theme.get(), "the custom theme is applied on top of the selected one");
        assertTrue(card(2).isSelected());
    }

    @Test
    void clickOnSelectedCardKeepsItSelected() {
        card(0).fire();

        assertTrue(card(0).isSelected());
    }
}
