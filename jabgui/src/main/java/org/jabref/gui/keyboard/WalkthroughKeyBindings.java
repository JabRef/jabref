package org.jabref.gui.keyboard;

import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.StateManager;
import org.jabref.gui.walkthrough.Walkthrough;

import com.airhacks.afterburner.injection.Injector;

/// Handles key bindings related to walkthrough functionality.
public class WalkthroughKeyBindings {

    /// Handles ESC key to quit active walkthrough with confirmation.
    ///
    /// @param event                the key event
    /// @param keyBindingRepository the key binding repository
    public static void call(KeyEvent event, KeyBindingRepository keyBindingRepository) {
        keyBindingRepository.mapToKeyBinding(event).ifPresent(binding -> {
            if (binding == KeyBinding.CLOSE) { // NOTE: CLOSE is using Esc key.
                handleQuitWalkthrough();
                event.consume();
            }
        });
    }

    private static void handleQuitWalkthrough() {
        StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);
        stateManager.getActiveWalkthrough().ifPresent(Walkthrough::showQuitConfirmationAndQuit);
    }
}
