package org.jabref.gui.fieldeditors.contextmenu;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class ProtectedTermsMenuTest extends JavaFxTest {

    @AfterEach
    void tearDown() {
        Injector.forgetAll();
    }

    @Test
    void noExternalListsShowsDisabledItemWithTooltip() {
        Injector.setModelOrService(KeyBindingRepository.class, new KeyBindingRepository());
        Injector.setModelOrService(ProtectedTermsLoader.class, new ProtectedTermsLoader(
                new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(), List.of(), List.of(), List.of())));

        AtomicReference<Stage> stageRef = new AtomicReference<>();
        AtomicReference<ContextMenu> popupRef = new AtomicReference<>();
        AtomicReference<MenuItem> noListItemRef = new AtomicReference<>();
        interact(() -> {
            TextField textField = new TextField("In CDMA");
            Stage stage = new Stage();
            stage.setScene(new Scene(new StackPane(textField), 200, 100));
            stage.show();
            stageRef.set(stage);

            ProtectedTermsMenu menu = new ProtectedTermsMenu(textField);
            Menu subMenu = assertInstanceOf(Menu.class, menu.getItems().get(1));
            assertEquals(1, subMenu.getItems().size());
            MenuItem noListItem = subMenu.getItems().getFirst();
            noListItemRef.set(noListItem);

            // Detach into a direct ContextMenu so JavaFX creates its row node and fires WINDOW_SHOWN in headless mode
            subMenu.getItems().clear();
            ContextMenu popup = new ContextMenu(noListItem);
            popupRef.set(popup);
            popup.show(textField, 0, 0);
        });
        awaitEvents();

        AtomicReference<Optional<String>> tooltipTextRef = new AtomicReference<>(Optional.empty());
        interact(() -> {
            try {
                Optional.ofNullable(noListItemRef.get())
                        .flatMap(item -> Optional.ofNullable(item.getStyleableNode()))
                        .map(row -> row.getProperties().get("javafx.scene.control.Tooltip"))
                        .filter(Tooltip.class::isInstance)
                        .map(Tooltip.class::cast)
                        .map(Tooltip::getText)
                        .ifPresent(text -> tooltipTextRef.set(Optional.of(text)));
            } finally {
                Optional.ofNullable(popupRef.get()).ifPresent(ContextMenu::hide);
                Optional.ofNullable(stageRef.get()).ifPresent(Stage::hide);
            }
        });

        MenuItem noListItem = Optional.ofNullable(noListItemRef.get()).orElseThrow();
        assertEquals(Localization.lang("No list enabled"), noListItem.getText());
        assertTrue(noListItem.isDisable());
        assertEquals(
                Optional.of(Localization.lang("Please create a user-defined list (Preferences > Protected Terms Files)")),
                tooltipTextRef.get());
    }

    @Test
    void externalListAppearsInSubmenuInsteadOfNoListEnabled(@TempDir Path tempDir) {
        Injector.setModelOrService(KeyBindingRepository.class, new KeyBindingRepository());
        ProtectedTermsLoader loader = new ProtectedTermsLoader(
                new ProtectedTermsPreferences(List.of(), List.of(), ProtectedTermsLoader.getInternalLists(), List.of()));
        loader.addNewProtectedTermsList("My terms", tempDir.resolve("custom.terms").toString(), true);
        Injector.setModelOrService(ProtectedTermsLoader.class, loader);

        AtomicReference<List<String>> itemTexts = new AtomicReference<>(List.of());
        interact(() -> {
            TextField textField = new TextField("In CDMA");
            ProtectedTermsMenu menu = new ProtectedTermsMenu(textField);
            Menu subMenu = assertInstanceOf(Menu.class, menu.getItems().get(1));
            itemTexts.set(subMenu.getItems().stream().map(MenuItem::getText).toList());
        });

        assertEquals(List.of("My terms"), itemTexts.get());
        assertNotEquals(List.of(Localization.lang("No list enabled")), itemTexts.get());
    }

    @Test
    void formatFieldProtectsTermsInTextControl() {
        Injector.setModelOrService(KeyBindingRepository.class, new KeyBindingRepository());
        Injector.setModelOrService(ProtectedTermsLoader.class, new ProtectedTermsLoader(
                new ProtectedTermsPreferences(ProtectedTermsLoader.getInternalLists(), List.of(), List.of(), List.of())));

        AtomicReference<String> resultText = new AtomicReference<>("");
        interact(() -> {
            TextField textField = new TextField("In CDMA");
            ProtectedTermsMenu menu = new ProtectedTermsMenu(textField);
            MenuItem formatFieldItem = menu.getItems().stream()
                                           .filter(item -> Localization.lang("Format field").equals(item.getText()))
                                           .findFirst()
                                           .orElseThrow();
            formatFieldItem.fire();
            resultText.set(textField.getText());
        });

        assertEquals("In {CDMA}", resultText.get());
    }
}
