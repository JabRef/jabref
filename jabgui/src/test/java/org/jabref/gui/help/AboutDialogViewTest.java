package org.jabref.gui.help;

import java.util.List;
import java.util.Objects;

import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.DialogButtonAssertions;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BuildInfo;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

@AllowedToUseClassGetResource("JavaFX internally handles the passed URLs properly.")
class AboutDialogViewTest extends ApplicationTest {

    private static final String FONT_SIZE_CLASS = "font-size-12";

    private AboutDialogView aboutDialogView;
    private ClipBoardManager clipBoardManager;

    private ListChangeListener<Window> raisedFontSizeListener;

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

        Scene mainWindowScene = new Scene(new StackPane(), 1024, 768);
        mainWindowScene.getStylesheets().addAll(
                stylesheet("/org/jabref/gui/theme/jabref-theme.css"),
                stylesheet("/org/jabref/gui/theme/internal/jabref-base.css"));
        stage.setScene(mainWindowScene);

        raisedFontSizeListener = change -> {
            while (change.next()) {
                change.getAddedSubList().stream()
                      .map(Window::getScene)
                      .filter(Objects::nonNull)
                      .forEach(scene -> scene.getRoot().getStyleClass().add(FONT_SIZE_CLASS));
            }
        };
        Window.getWindows().addListener(raisedFontSizeListener);

        aboutDialogView = new AboutDialogView();
        aboutDialogView.initOwner(stage);
        interact(aboutDialogView::show);
    }

    @AfterEach
    void removeRaisedFontSizeListener() {
        if (raisedFontSizeListener != null) {
            Window.getWindows().removeListener(raisedFontSizeListener);
            raisedFontSizeListener = null;
        }
    }

    @Test
    void copyVersionButton() {
        verifyThat("Copy Version", isVisible());

        interact(() -> buttonOf("Copy Version").fire());

        verify(clipBoardManager).setContent(anyString());
    }

    @Test
    void closeButton() {
        verifyThat("Close", isVisible());

        interact(() -> buttonOf("Close").fire());

        assertFalse(aboutDialogView.isShowing());
    }

    @Test
    void buttonCaptionsAreNotTruncatedAtARaisedFontSize() {
        WaitForAsyncUtils.waitForFxEvents();
        DialogPane pane = aboutDialogView.getDialogPane();

        assertEquals(List.of("Copy Version", "Close"),
                DialogButtonAssertions.buttonsOf(pane).stream().map(Button::getText).toList());
        DialogButtonAssertions.assertCaptionsAreNotTruncated(pane);
    }

    /// The buttons are fired rather than clicked: a robot click needs the window manager to let the
    /// application move the pointer, which is not the case on every desktop the tests run on.
    private Button buttonOf(String caption) {
        return DialogButtonAssertions.buttonsOf(aboutDialogView.getDialogPane()).stream()
                                     .filter(button -> caption.equals(button.getText()))
                                     .findFirst()
                                     .orElseThrow(() -> new AssertionError("No button '%s' on the dialog".formatted(caption)));
    }

    private static String stylesheet(String path) {
        return Objects.requireNonNull(AboutDialogViewTest.class.getResource(path)).toExternalForm();
    }
}
