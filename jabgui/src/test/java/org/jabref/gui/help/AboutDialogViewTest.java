package org.jabref.gui.help;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.DialogButtonAssertions;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BuildInfo;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@AllowedToUseClassGetResource("JavaFX internally handles the passed URLs properly.")
class AboutDialogViewTest extends JavaFxTest {

    private static final String FONT_SIZE_CLASS = "font-size-12";
    private static final String COPY_VERSION_BUTTON = "#copyVersionButton";
    private static final String CLOSE_BUTTON = "#closeButton";

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
                stylesheet("/org/jabref/gui/theme/themes.jabref.org/jabref-theme.css"),
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
            interact(() -> Window.getWindows().removeListener(raisedFontSizeListener));
            raisedFontSizeListener = null;
        }
    }

    @Test
    void copyVersionButton() {
        interact(() -> {
            Button copyVersionButton = button(COPY_VERSION_BUTTON);
            assertTrue(copyVersionButton.isVisible());
            copyVersionButton.fire();
        });

        verify(clipBoardManager).setContent(anyString());
    }

    @Test
    void closeButton() {
        interact(() -> {
            Button closeButton = button(CLOSE_BUTTON);
            assertTrue(closeButton.isVisible());
            closeButton.fire();
        });

        interact(() -> assertFalse(aboutDialogView.isShowing()));
    }

    @Test
    void buttonCaptionsAreNotTruncatedAtARaisedFontSize() {
        awaitEvents();

        interact(() -> {
            assertEquals(List.of("Copy Version", "Close"), buttons().stream().map(Button::getText).toList());
            DialogButtonAssertions.assertCaptionsAreNotTruncated(buttons());
        });
    }

    private List<Button> buttons() {
        return Stream.of(COPY_VERSION_BUTTON, CLOSE_BUTTON).map(this::button).toList();
    }

    /// The dialog's buttons sit at the bottom right of its content, not in a button bar - the pane
    /// creates none - so they are looked up by id rather than through the button types.
    private Button button(String id) {
        Node button = aboutDialogView.getDialogPane().lookup(id);
        assertNotNull(button, "No button '%s' on the dialog".formatted(id));
        return (Button) button;
    }

    private static String stylesheet(String path) {
        return Objects.requireNonNull(AboutDialogViewTest.class.getResource(path)).toExternalForm();
    }
}
