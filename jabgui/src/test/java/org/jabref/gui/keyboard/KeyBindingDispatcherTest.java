package org.jabref.gui.keyboard;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyBindingDispatcherTest {

    private final List<KeyBinding> executedBindings = new ArrayList<>();
    private KeyBindingDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new KeyBindingDispatcher(new KeyBindingRepository())
                .on(KeyBinding.SELECT_FIRST_ENTRY, () -> executedBindings.add(KeyBinding.SELECT_FIRST_ENTRY))
                .on(KeyBinding.SELECT_LAST_ENTRY, () -> executedBindings.add(KeyBinding.SELECT_LAST_ENTRY));
    }

    @Test
    void registeredKeyBindingRunsItsAction() {
        dispatcher.handle(createKeyPressedEvent(KeyCode.HOME));

        assertEquals(List.of(KeyBinding.SELECT_FIRST_ENTRY), executedBindings);
    }

    @Test
    void registeredKeyBindingConsumesEvent() {
        KeyEvent event = createKeyPressedEvent(KeyCode.HOME);

        dispatcher.handle(event);

        assertTrue(event.isConsumed());
    }

    @Test
    void unregisteredKeyBindingDoesNotRunAnyAction() {
        dispatcher.handle(createKeyPressedEvent(KeyCode.ESCAPE));

        assertEquals(List.of(), executedBindings);
    }

    @Test
    void unregisteredKeyBindingLeavesEventForParent() {
        KeyEvent event = createKeyPressedEvent(KeyCode.ESCAPE);

        dispatcher.handle(event);

        assertFalse(event.isConsumed());
    }

    @Test
    void keyWithoutKeyBindingLeavesEventForParent() {
        KeyEvent event = createKeyPressedEvent(KeyCode.Q);

        dispatcher.handle(event);

        assertEquals(List.of(), executedBindings);
        assertFalse(event.isConsumed());
    }

    @Test
    void laterRegistrationReplacesEarlierAction() {
        dispatcher.on(KeyBinding.SELECT_FIRST_ENTRY, () -> executedBindings.add(KeyBinding.CLOSE));

        dispatcher.handle(createKeyPressedEvent(KeyCode.HOME));

        assertEquals(List.of(KeyBinding.CLOSE), executedBindings);
    }

    @Test
    void actionWithoutConsumingRuns() {
        dispatcher.onWithoutConsuming(KeyBinding.CLOSE, () -> executedBindings.add(KeyBinding.CLOSE));

        dispatcher.handle(createKeyPressedEvent(KeyCode.ESCAPE));

        assertEquals(List.of(KeyBinding.CLOSE), executedBindings);
    }

    @Test
    void actionWithoutConsumingLeavesEventForParent() {
        dispatcher.onWithoutConsuming(KeyBinding.CLOSE, () -> executedBindings.add(KeyBinding.CLOSE));
        KeyEvent event = createKeyPressedEvent(KeyCode.ESCAPE);

        dispatcher.handle(event);

        assertFalse(event.isConsumed());
    }

    @Test
    void conditionalActionThatHandledEventConsumesIt() {
        dispatcher.onConditionally(KeyBinding.CLOSE, () -> true);
        KeyEvent event = createKeyPressedEvent(KeyCode.ESCAPE);

        dispatcher.handle(event);

        assertTrue(event.isConsumed());
    }

    @Test
    void conditionalActionThatDidNotHandleEventLeavesItForParent() {
        dispatcher.onConditionally(KeyBinding.CLOSE, () -> {
            executedBindings.add(KeyBinding.CLOSE);
            return false;
        });
        KeyEvent event = createKeyPressedEvent(KeyCode.ESCAPE);

        dispatcher.handle(event);

        assertEquals(List.of(KeyBinding.CLOSE), executedBindings);
        assertFalse(event.isConsumed());
    }

    private static KeyEvent createKeyPressedEvent(KeyCode keyCode) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", keyCode.getName(), keyCode, false, false, false, false);
    }
}
