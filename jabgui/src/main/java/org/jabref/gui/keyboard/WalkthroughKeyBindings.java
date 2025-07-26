package org.jabref.gui.keyboard;

import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.StateManager;

import com.airhacks.afterburner.injection.Injector;

/// Handles key bindings related to walkthrough functionality.
public class WalkthroughKeyBindings {

    /// Handles ESC key to quit active walkthrough with confirmation.
    ///
    /// @param scene the scene where the key event occurred
    /// @param event the key event
    /// @param keyBindingRepository the key binding repository
    public static void call(Scene scene, KeyEvent event, KeyBindingRepository keyBindingRepository) {
        keyBindingRepository.mapToKeyBinding(event).ifPresent(binding -> {
            if (binding == KeyBinding.QUIT_WALKTHROUGH) {
                handleQuitWalkthrough();
                event.consume();
            }
        });
    }

    private static void handleQuitWalkthrough() {
        StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);

        if (stateManager.hasActiveWalkthrough()) {
            stateManager.getActiveWalkthrough().ifPresent(walkthrough -> 
                walkthrough.showQuitConfirmationAndQuit());
        }
    }
}
