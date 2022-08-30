package org.jabref.gui.actions;

import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;

public interface Action {
    default Optional<JabRefIcon> getIcon() {
        return Optional.empty();
    }

    default Optional<KeyBinding> getKeyBinding() {
        return Optional.empty();
    }

    String getText();

    default String getDescription() {
        return "";
    }

    class Builder {
        private final ActionImpl actionImpl;

        public Builder(String text) {
            this.actionImpl = new ActionImpl();
            setText(text);
        }

        public Builder() {
            this("");
        }

        public Action setIcon(JabRefIcon icon) {
            Objects.requireNonNull(icon);
            actionImpl.icon = icon;
            return actionImpl;
        }

        public Action setText(String text) {
            Objects.requireNonNull(text);
            actionImpl.text = text;
            return actionImpl;
        }

        public Action setKeyBinding(KeyBinding keyBinding) {
            Objects.requireNonNull(keyBinding);
            actionImpl.keyBinding = keyBinding;
            return actionImpl;
        }

        public Action setDescription(String description) {
            Objects.requireNonNull(description);
            actionImpl.description = description;
            return actionImpl;
        }
    }

    class ActionImpl implements Action {
        private JabRefIcon icon;
        private KeyBinding keyBinding;
        private String text;
        private String description;

        private ActionImpl() {
        }

        @Override
        public Optional<JabRefIcon> getIcon() {
            return Optional.ofNullable(icon);
        }

        @Override
        public Optional<KeyBinding> getKeyBinding() {
            return Optional.ofNullable(keyBinding);
        }

        @Override
        public String getText() {
            return text != null ? text : "";
        }

        @Override
        public String getDescription() {
            return description != null ? description : "";
        }
    }
}
