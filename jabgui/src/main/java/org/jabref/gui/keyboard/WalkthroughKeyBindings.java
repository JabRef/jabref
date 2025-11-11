package org.jabref.gui.keyboard;

import javafx.scene.input.KeyEvent;

import org.jabref.gui.StateManager;
import org.jabref.gui.walkthrough.Walkthrough;

public class WalkthroughKeyBindings {

    /// Handles ESC key to quit active walkthrough with confirmation.
    ///
    /// @param event                the key event
    /// @param stateManager         the state manager
    /// @param keyBindingRepository the key binding repository
    public static void call(KeyEvent event, StateManager stateManager, KeyBindingRepository keyBindingRepository) {
        keyBindingRepository.mapToKeyBinding(event).ifPresent(binding -> {
            if (binding == KeyBinding.CLOSE) { // NOTE: CLOSE is using Esc key. Therefore, we didn't introduce a new key binding entry since this would lead to conflicts with other key bindings.
                stateManager.getActiveWalkthrough().ifPresent(Walkthrough::showQuitConfirmationAndQuit);
                event.consume();
            }
        });
    }
}
