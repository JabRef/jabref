package org.jabref.gui.keyboard;

import java.util.Optional;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.StateManager;
import org.jabref.gui.walkthrough.Walkthrough;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class WalkthroughKeyBindingsTest {
    private final StateManager stateManager = mock(StateManager.class);
    private final KeyBindingRepository keyBindings = mock(KeyBindingRepository.class);

    @Test
    void escapeWithoutWalkthroughRemainsUnconsumed() {
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false);
        when(keyBindings.mapToKeyBinding(event)).thenReturn(Optional.of(KeyBinding.CLOSE));
        when(stateManager.getActiveWalkthrough()).thenReturn(Optional.empty());

        WalkthroughKeyBindings.call(event, stateManager, keyBindings);

        assertFalse(event.isConsumed());
    }

    @Test
    void escapeWithWalkthroughRequestsConfirmationAndConsumesEvent() {
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false);
        Walkthrough walkthrough = mock(Walkthrough.class);
        when(keyBindings.mapToKeyBinding(event)).thenReturn(Optional.of(KeyBinding.CLOSE));
        when(stateManager.getActiveWalkthrough()).thenReturn(Optional.of(walkthrough));

        WalkthroughKeyBindings.call(event, stateManager, keyBindings);

        verify(walkthrough).showQuitConfirmationAndQuit();
        assertTrue(event.isConsumed());
    }
}
