package org.jabref.gui.help;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BuildInfo;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AboutDialogViewTest extends JavaFxTest {

    private AboutDialogView aboutDialogView;
    private ClipBoardManager clipBoardManager;

    @BeforeEach
    void initLocalization() {
        Localization.setLanguage(Language.ENGLISH);
    }

    @Override
    public void start(Stage stage) {
        GuiPreferences preferences = mock(GuiPreferences.class);
        DialogService dialogService = mock(DialogService.class);
        clipBoardManager = mock(ClipBoardManager.class);
        BuildInfo buildInfo = new BuildInfo();
        ThemeManager themeManager = mock(ThemeManager.class);
        KeyBindingRepository keyBindingRepository = mock(KeyBindingRepository.class);
        StateManager stateManager = mock(StateManager.class);

        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(DialogService.class, dialogService);
        Injector.setModelOrService(ClipBoardManager.class, clipBoardManager);
        Injector.setModelOrService(BuildInfo.class, buildInfo);
        Injector.setModelOrService(ThemeManager.class, themeManager);
        Injector.setModelOrService(KeyBindingRepository.class, keyBindingRepository);
        Injector.setModelOrService(StateManager.class, stateManager);

        aboutDialogView = new AboutDialogView();

        aboutDialogView = new AboutDialogView();

        DialogPane pane = aboutDialogView.getDialogPane();

        // 1. Load the CSS into the DialogPane
        pane.getStylesheets().add(AboutDialogView.class.getResource("/org/jabref/gui/theme/jabref-theme.css").toExternalForm());
        // 2. Force the 10pt font style
        pane.setStyle("-fx-font-size: 10pt;");
        // 3. Show the dialog (this triggers BaseDialog's DIALOG_SHOWING listener)
        interact(() -> aboutDialogView.show());
    }

    @Test
    void aboutDialogHeading() {
        assertTrue(aboutDialogView.getDialogPane().lookup(".about-heading").isVisible());
    }

    @Test
    void copyVersionButton() {
        Button copyVersionButton = button("Copy Version");
        assertTrue(copyVersionButton.isVisible());
        interact(copyVersionButton::fire);
        verify(clipBoardManager).setContent(anyString());
    }

    @Test
    void closeButton() {
        Button closeButton = button("Close");
        assertTrue(closeButton.isVisible());
        interact(closeButton::fire);
    }

    @Test
    void buttonsAreNotTruncatedAt10ptFont() throws InterruptedException {
        DialogPane pane = aboutDialogView.getDialogPane();
        for (ButtonType type : pane.getButtonTypes()) {
            Button button = (Button) pane.lookupButton(type);

            // We need to wait for a layout pulse to ensure CSS is applied
            interact(() -> {
                button.applyCss();
                double prefWidth = button.prefWidth(-1);
                double actualWidth = button.getWidth();

                // Assert that the actual rendered width is at least as large
                // as the width required by the 10pt text.
                // If actualWidth < prefWidth, JavaFX will truncate the text.
                assertTrue(actualWidth >= prefWidth,
                        "Button [%s] is truncated! Actual: %.2f, Pref: %.2f".formatted(
                                button.getText(), actualWidth, prefWidth));
            });
            // for debugging purpises
            // Thread.sleep(4000);
        }
    }

    private Button button(String text) {
        return aboutDialogView.getDialogPane().lookupAll(".button")
                              .stream()
                              .map(Button.class::cast)
                              .filter(button -> button.getText().equals(text))
                              .findFirst()
                              .orElseThrow();
    }
}
