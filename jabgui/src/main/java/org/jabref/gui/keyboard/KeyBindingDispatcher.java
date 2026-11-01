package org.jabref.gui.keyboard;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import javafx.scene.input.KeyEvent;

import org.jspecify.annotations.NullMarked;

/// Maps key bindings to the actions a view runs for them.
///
/// An event is consumed only if an action is registered for its key binding.
/// All other events stay unconsumed, so they bubble up to the parent nodes (e.g., to reach the global shortcuts of the main window).
@NullMarked
public class KeyBindingDispatcher {

    private final KeyBindingRepository keyBindingRepository;
    private final Map<KeyBinding, Runnable> actions = new EnumMap<>(KeyBinding.class);

    public KeyBindingDispatcher(KeyBindingRepository keyBindingRepository) {
        this.keyBindingRepository = keyBindingRepository;
    }

    public KeyBindingDispatcher on(KeyBinding keyBinding, Runnable action) {
        actions.put(keyBinding, action);
        return this;
    }

    /// Runs the action registered for the key binding of the given event and consumes the event.
    public void handle(KeyEvent event) {
        Optional<Runnable> action = keyBindingRepository.mapToKeyBinding(event).map(actions::get);
        action.ifPresent(registeredAction -> {
            registeredAction.run();
            event.consume();
        });
    }
}
