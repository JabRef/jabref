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
import org.jabref.gui.theme.ThemeManager;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BuildInfo;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

class AboutDialogViewTest extends ApplicationTest {

    private AboutDialogView aboutDialogView;
    private ClipBoardManager clipBoardManager;

    @BeforeEach
    void initLocalization() {
        Localization.setLanguage(Language.ENGLISH);
    }

    @Override
    public void start(Stage stage) throws Exception {
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
        pane.getStylesheets().add(AboutDialogView.class.getResource("/org/jabref/gui/Base.css").toExternalForm());
        // 2. Force the 10pt font style
        pane.setStyle("-fx-font-size: 10pt;");
        // 3. Show the dialog (this triggers BaseDialog's DIALOG_SHOWING listener)
        interact(() -> aboutDialogView.show());
    }

    @Test
    void testAboutDialogHeading() {
        verifyThat(".about-heading", isVisible());
    }

    @Test
    void testCopyVersionButton() {
        verifyThat("Copy Version", isVisible());
        clickOn("Copy Version");
        verify(clipBoardManager).setContent(anyString());
    }

    @Test
    void testCloseButton() {
        verifyThat("Close", isVisible());
        clickOn("Close");
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
                        String.format("Button [%s] is truncated! Actual: %.2f, Pref: %.2f",
                                button.getText(), actualWidth, prefWidth));
            });
            // for debugging purpises
            // Thread.sleep(4000);
        }
    }
}
