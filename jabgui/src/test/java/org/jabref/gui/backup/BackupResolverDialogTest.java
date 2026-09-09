package org.jabref.gui.backup;

import java.nio.file.Path;
import java.util.Objects;

import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.util.DialogButtonAssertions;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@AllowedToUseClassGetResource("JavaFX internally handles the passed URLs properly.")
class BackupResolverDialogTest extends JavaFxTest {

    private static final String FONT_SIZE_CLASS = "font-size-12";

    private BackupResolverDialog backupResolverDialog;

    private ListChangeListener<Window> raisedFontSizeListener;

    @BeforeEach
    void initLocalization() {
        Localization.setLanguage(Language.ENGLISH);
    }

    @Override
    public void start(Stage stage) {
        Injector.setModelOrService(KeyBindingRepository.class, mock(KeyBindingRepository.class));

        Scene mainWindowScene = new Scene(new StackPane(), 1024, 768);
        mainWindowScene.getStylesheets().addAll(
                stylesheet("/org/jabref/gui/theme/community/jabref-theme.css"),
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

        backupResolverDialog = new BackupResolverDialog(
                Path.of("library.bib"), Path.of("backups"), mock(ExternalApplicationsPreferences.class));
        backupResolverDialog.initOwner(stage);
        interact(backupResolverDialog::show);
    }

    @AfterEach
    void removeRaisedFontSizeListener() {
        if (raisedFontSizeListener != null) {
            interact(() -> Window.getWindows().removeListener(raisedFontSizeListener));
            raisedFontSizeListener = null;
        }
    }

    /// The dialog has three buttons with long captions, so they only fit when the window grows to the
    /// size the raised font needs.
    @Test
    void buttonCaptionsAreNotTruncatedAtARaisedFontSize() {
        awaitEvents();

        interact(() -> DialogButtonAssertions.assertCaptionsAreNotTruncated(backupResolverDialog.getDialogPane()));
    }

    @Test
    void windowIsAsWideAsItsContentNeeds() {
        awaitEvents();

        interact(() -> {
            DialogPane pane = backupResolverDialog.getDialogPane();
            double windowWidth = pane.getScene().getWindow().getWidth();
            assertTrue(windowWidth >= pane.prefWidth(-1),
                    "Window is %.1fpx wide, but its content needs %.1fpx".formatted(windowWidth, pane.prefWidth(-1)));
        });
    }

    private static String stylesheet(String path) {
        return Objects.requireNonNull(BackupResolverDialogTest.class.getResource(path)).toExternalForm();
    }
}
