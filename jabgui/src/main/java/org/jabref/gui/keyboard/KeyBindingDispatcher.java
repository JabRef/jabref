package org.jabref.gui.keyboard;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import javafx.scene.input.KeyEvent;

import org.jspecify.annotations.NullMarked;

/// Maps key bindings to the actions a view runs for them.
///
/// An event is consumed only if the registered action handled it.
/// All other events stay unconsumed, so they still reach the focused control, the event handlers of the parent nodes, and the menu accelerators of the window.
///
/// See the "Keyboard shortcuts" section of the [JavaFX code howto](https://devdocs.jabref.org/code-howtos/javafx.html) for when to use an event filter or an event handler.
@NullMarked
public class KeyBindingDispatcher {

    private final KeyBindingRepository keyBindingRepository;
    private final Map<KeyBinding, BooleanSupplier> actions = new EnumMap<>(KeyBinding.class);

    public KeyBindingDispatcher(KeyBindingRepository keyBindingRepository) {
        this.keyBindingRepository = keyBindingRepository;
    }

    /// Registers an action that always handles the event, so the event is consumed.
    public KeyBindingDispatcher on(KeyBinding keyBinding, Runnable action) {
        return onConditionally(keyBinding, () -> {
            action.run();
            return true;
        });
    }

    /// Registers an action that runs, but leaves the event unconsumed.
    ///
    /// Use this if other nodes need to react to the same key, too (e.g., Esc unfocusing a field and closing a dialog).
    public KeyBindingDispatcher onWithoutConsuming(KeyBinding keyBinding, Runnable action) {
        return onConditionally(keyBinding, () -> {
            action.run();
            return false;
        });
    }

    /// Registers an action that returns whether it handled the event. Only then the event is consumed.
    ///
    /// Use this if the action applies only in some states (e.g., only if a file is selected).
    public KeyBindingDispatcher onConditionally(KeyBinding keyBinding, BooleanSupplier action) {
        actions.put(keyBinding, action);
        return this;
    }

    /// Runs the action registered for the key binding of the given event and consumes the event if the action handled it.
    public void handle(KeyEvent event) {
        boolean handled = keyBindingRepository.mapToKeyBinding(event)
                                              .map(actions::get)
                                              .map(BooleanSupplier::getAsBoolean)
                                              .orElse(false);
        if (handled) {
            event.consume();
        }
    }
}
