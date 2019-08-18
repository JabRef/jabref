package org.jabref.gui.preferences;

import java.util.Optional;

import org.jabref.gui.actions.Action;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;

public enum PreferencesActions implements Action {
    COLUMN_SORT_UP(Localization.lang("Sort up"), Localization.lang("Sort column one step upwards"), IconTheme.JabRefIcons.UP),
    COLUMN_SORT_DOWN(Localization.lang("Sort down"), Localization.lang("Sort column one step downwards"), IconTheme.JabRefIcons.DOWN),
    COLUMN_ADD(Localization.lang("Add"), Localization.lang("Add custom column"), IconTheme.JabRefIcons.ADD_NOBOX),
    COLUMN_REMOVE(Localization.lang("Remove"), Localization.lang("Remove selected custom column"), IconTheme.JabRefIcons.REMOVE_NOBOX),
    COLUMNS_UPDATE(Localization.lang("Update"), Localization.lang("Update to current column order"), IconTheme.JabRefIcons.REFRESH);

    private final String text;
    private final String description;
    private final Optional<JabRefIcon> icon;
    private final Optional<KeyBinding> keyBinding;

    PreferencesActions(String text) {
        this(text, "");
    }

    PreferencesActions(String text, IconTheme.JabRefIcons icon) {
        this.text = text;
        this.description = "";
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.empty();
    }

    PreferencesActions(String text, IconTheme.JabRefIcons icon, KeyBinding keyBinding) {
        this.text = text;
        this.description = "";
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.of(keyBinding);
    }

    PreferencesActions(String text, String description, IconTheme.JabRefIcons icon) {
        this.text = text;
        this.description = description;
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.empty();
    }

    PreferencesActions(String text, String description, IconTheme.JabRefIcons icon, KeyBinding keyBinding) {
        this.text = text;
        this.description = description;
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.of(keyBinding);
    }

    PreferencesActions(String text, KeyBinding keyBinding) {
        this.text = text;
        this.description = "";
        this.keyBinding = Optional.of(keyBinding);
        this.icon = Optional.empty();
    }

    PreferencesActions(String text, String description) {
        this.text = text;
        this.description = description;
        this.icon = Optional.empty();
        this.keyBinding = Optional.empty();
    }

    PreferencesActions(String text, String description, KeyBinding keyBinding) {
        this.text = text;
        this.description = description;
        this.icon = Optional.empty();
        this.keyBinding = Optional.of(keyBinding);
    }

    @Override
    public Optional<JabRefIcon> getIcon() {
        return icon;
    }

    @Override
    public Optional<KeyBinding> getKeyBinding() {
        return keyBinding;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getDescription() {
        return description;
    }
}


