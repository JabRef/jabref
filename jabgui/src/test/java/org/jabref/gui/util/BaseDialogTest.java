package org.jabref.gui.util;

import java.util.concurrent.atomic.AtomicReference;

import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.testutils.JavaFxTest;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class BaseDialogTest extends JavaFxTest {

    private final KeyBindingRepository keyBindingRepository = mock(KeyBindingRepository.class);

    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setScene(new Scene(new StackPane()));
        Injector.setModelOrService(KeyBindingRepository.class, keyBindingRepository);
    }

    @Test
    // [utest->req~ux.dialogs.escape-closes~1]
    void escapeClosesDialog() {
        BaseDialog<Void> dialog = show(true);
        press(dialog, KeyCode.ESCAPE);
        interact(() -> assertFalse(dialog.isShowing()));
    }

    @Test
    // [utest->req~ux.dialogs.escape-closes~1]
    void escapeKeepsOptedOutDialogOpen() {
        BaseDialog<Void> dialog = show(false);
        press(dialog, KeyCode.ESCAPE);
        interact(() -> assertTrue(dialog.isShowing()));
    }

    @Test
    void customCloseShortcutClosesOptedOutDialog() {
        when(keyBindingRepository.checkKeyCombinationEquality(eq(KeyBinding.CLOSE), any(KeyEvent.class))).thenReturn(true);
        BaseDialog<Void> dialog = show(false);
        press(dialog, KeyCode.W);
        interact(() -> assertFalse(dialog.isShowing()));
    }

    private BaseDialog<Void> show(boolean closesOnEscape) {
        AtomicReference<BaseDialog<Void>> dialog = new AtomicReference<>();
        interact(() -> {
            dialog.set(new BaseDialog<>() {
                @Override
                protected boolean closesOnEscape() {
                    return closesOnEscape;
                }
            });
            dialog.get().getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.get().initOwner(stage);
            dialog.get().show();
        });
        return dialog.get();
    }

    private void press(BaseDialog<Void> dialog, KeyCode key) {
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, false, false, false);
        interact(() -> Event.fireEvent(dialog.getDialogPane(), event));
    }
}
