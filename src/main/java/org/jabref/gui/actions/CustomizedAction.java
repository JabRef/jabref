package org.jabref.gui.actions;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;

import java.util.Optional;

public class CustomizedAction implements Action{
    private final String text;
    private final String description;
    private final Optional<JabRefIcon> icon;
    private final Optional<KeyBinding> keyBinding;

    CustomizedAction(String text) {
        this(text, "");
    }

    CustomizedAction(String text, IconTheme.JabRefIcons icon) {
        this.text = text;
        this.description = "";
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.empty();
    }

    CustomizedAction(String text, IconTheme.JabRefIcons icon, KeyBinding keyBinding) {
        this.text = text;
        this.description = "";
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.of(keyBinding);
    }

    CustomizedAction(String text, String description, IconTheme.JabRefIcons icon) {
        this.text = text;
        this.description = description;
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.empty();
    }

    CustomizedAction(String text, String description, IconTheme.JabRefIcons icon, KeyBinding keyBinding) {
        this.text = text;
        this.description = description;
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.of(keyBinding);
    }

    CustomizedAction(String text, KeyBinding keyBinding) {
        this.text = text;
        this.description = "";
        this.keyBinding = Optional.of(keyBinding);
        this.icon = Optional.empty();
    }

    public CustomizedAction(String text, String description) {
        this.text = text;
        this.description = description;
        this.icon = Optional.empty();
        this.keyBinding = Optional.empty();
    }

    CustomizedAction(String text, String description, KeyBinding keyBinding) {
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
