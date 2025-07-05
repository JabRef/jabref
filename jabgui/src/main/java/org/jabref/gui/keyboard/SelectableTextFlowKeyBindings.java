package org.jabref.gui.keyboard;

import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.util.SelectableTextFlow;

public class SelectableTextFlowKeyBindings {
    public static void call(Scene scene, KeyEvent event, KeyBindingRepository keyBindingRepository) {
        if (scene.focusOwnerProperty().get() instanceof SelectableTextFlow selectableTextFlow) {
            keyBindingRepository.mapToKeyBinding(event).ifPresent(binding -> {
                switch (binding) {
                    case COPY -> {
                        selectableTextFlow.copySelectedText();
                        event.consume();
                    }
                    case SELECT_ALL -> {
                        selectableTextFlow.selectAll();
                        event.consume();
                    }
                }
            });
        }
    }
}
