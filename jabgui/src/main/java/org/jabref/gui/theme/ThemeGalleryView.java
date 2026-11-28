package org.jabref.gui.theme;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;

/// Picks a theme by its look: one card per theme, showing its bundled screenshot in the selected
/// color scheme above its name, the selected one highlighted. Unlike a combo box, every option is
/// visible at once, which is the point of choosing a theme.
///
/// The last card stands for a custom theme, a stylesheet of the user's own. It is applied on top
/// of the theme selected before, which therefore stays selected underneath while the custom card is.
@NullMarked
public class ThemeGalleryView extends FlowPane {

    private static final double THUMBNAIL_WIDTH = 160;
    private static final double THUMBNAIL_HEIGHT = 100;
    private static final double GAP = 12;

    private final ToggleGroup cards = new ToggleGroup();
    private final List<ToggleButton> presetCards = new ArrayList<>();
    private final ToggleButton customCard = new ToggleButton(Localization.lang("Custom theme"));

    public ThemeGalleryView() {
        super(GAP, GAP);
        getStyleClass().add("theme-gallery");
    }

    /// Shows `themes` plus the custom card and keeps the selected card in sync with `theme` and
    /// `customThemeEnabled`. The cards follow `colorScheme`, as a theme's screenshot and, for some
    /// themes, its name depend on it.
    ///
    /// A click on a card writes the properties, and the properties alone decide which card is
    /// selected: a click on the selected card, which would deselect it, therefore leaves it selected.
    public void bind(List<ThemePreset> themes,
                     Property<ThemePreset> theme,
                     BooleanProperty customThemeEnabled,
                     ObservableValue<ThemeColorScheme> colorScheme) {
        Runnable select = () -> select(theme.getValue(), customThemeEnabled.get());
        for (ThemePreset preset : themes) {
            ToggleButton card = card(new ToggleButton(), () -> {
                customThemeEnabled.set(false);
                theme.setValue(preset);
            }, select);
            card.setUserData(preset);
            presetCards.add(card);
        }
        StackPane plus = new StackPane(IconTheme.JabRefIcons.ADD_NOBOX.getGraphicNode());
        plus.setPrefSize(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        plus.getStyleClass().add("theme-card-custom");
        customCard.setGraphic(plus);
        card(customCard, () -> customThemeEnabled.set(true), select);

        show(colorScheme.getValue());
        colorScheme.addListener((_, _, newColorScheme) -> show(newColorScheme));

        select.run();
        theme.addListener(_ -> select.run());
        customThemeEnabled.addListener(_ -> select.run());
    }

    private ToggleButton card(ToggleButton card, Runnable onClick, Runnable select) {
        card.setToggleGroup(cards);
        card.setContentDisplay(ContentDisplay.TOP);
        card.getStyleClass().add("theme-card");
        card.setOnAction(_ -> {
            onClick.run();
            select.run();
        });
        getChildren().add(card);
        return card;
    }

    private void show(ThemeColorScheme colorScheme) {
        for (ToggleButton card : presetCards) {
            ThemePreset preset = (ThemePreset) card.getUserData();
            card.setText(preset.getLocalizedName(colorScheme));
            card.setGraphic(preset.getPreview(colorScheme)
                                  .map(url -> ThemePreviewView.screenshot(url, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT))
                                  .orElse(null));
        }
    }

    private void select(ThemePreset theme, boolean customThemeEnabled) {
        if (customThemeEnabled) {
            cards.selectToggle(customCard);
            return;
        }
        presetCards.stream()
                   .filter(card -> card.getUserData() == theme)
                   .findFirst()
                   .ifPresent(cards::selectToggle);
    }
}
