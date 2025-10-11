package org.jabref.gui.push;

import java.util.Optional;

import org.jabref.gui.actions.Action;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;

public class GuiPushToApplicationAction implements Action {

    private final String displayName;
    private final JabRefIcon applicationIcon;

    public GuiPushToApplicationAction(String displayName, JabRefIcon applicationIcon) {
        this.displayName = displayName;
        this.applicationIcon = applicationIcon;
    }

    @Override
    public String getText() {
        return Localization.lang("Push entries to external application (%0)", displayName);
    }

    @Override
    public Optional<JabRefIcon> getIcon() {
        return Optional.of(applicationIcon);
    }

    @Override
    public Optional<KeyBinding> getKeyBinding() {
        return Optional.of(KeyBinding.PUSH_TO_APPLICATION);
    }
}
