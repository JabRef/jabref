package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@NullMarked
class PreferencesDialogViewTest extends JavaFxTest {
    private PreferencesDialogView dialog;
    private Label target;

    @Override
    public void start(Stage stage) {
        Localization.setLanguage(Language.ENGLISH);
        Injector.setModelOrService(DialogService.class, mock(DialogService.class));
        Injector.setModelOrService(GuiPreferences.class, mock(GuiPreferences.class));
        Injector.setModelOrService(KeyBindingRepository.class, new KeyBindingRepository());

        PreferencesTab firstTab = tab("First", new Label("First setting"));
        target = new Label("A setting near the bottom");
        Region spacer = new Region();
        spacer.setMinHeight(1200);
        VBox secondContent = new VBox(spacer, target);
        PreferencesTab secondTab = tab("Second", target, secondContent);

        stage.setScene(new Scene(new StackPane(), 900, 600));
        stage.show();
        try (MockedConstruction<PreferencesDialogViewModel> ignored = mockConstruction(PreferencesDialogViewModel.class,
                (viewModel, _) -> {
                    when(viewModel.getPreferenceTabs()).thenReturn(FXCollections.observableArrayList(firstTab, secondTab));
                    when(viewModel.getMemoryStickProperty()).thenReturn(new SimpleBooleanProperty());
                })) {
            dialog = new PreferencesDialogView(firstTab.getClass().asSubclass(PreferencesTab.class));
        }
        dialog.initOwner(stage);
        dialog.show();
        dialog.getDialogPane().applyCss();
        dialog.getDialogPane().layout();
    }

    @AfterAll
    static void tearDown() {
        Injector.forgetAll();
    }

    // [utest->req~ux.preferences.search-scroll~1]
    @Test
    void searchScrollsToMatchOnAFirstVisitAndClearingSearchReturnsToTop() {
        interact(() -> {
            searchBox().setText("setting near the bottom");
            scrollPane().layout();

            Bounds targetBounds = target.localToScene(target.getBoundsInLocal());
            Node viewport = scrollPane().lookup(".viewport");
            Bounds viewportBounds = viewport.localToScene(viewport.getBoundsInLocal());
            assertTrue(targetBounds.getMinY() >= viewportBounds.getMinY());
            assertTrue(targetBounds.getMaxY() <= viewportBounds.getMaxY());
            assertTrue(scrollPane().getVvalue() > 0);

            searchBox().clear();
            assertEquals(0, scrollPane().getVvalue());
        });
    }

    @Test
    void narrowingSearchSelectsTheMatchingTabOnce() {
        interact(() -> {
            @SuppressWarnings("unchecked")
            ListView<PreferencesTab> tabs = (ListView<PreferencesTab>) dialog.getDialogPane().lookup("#preferencesTabList");
            searchBox().setText("setting");
            List<String> selectedTabs = new ArrayList<>();
            tabs.getSelectionModel().selectedItemProperty().addListener((_, _, item) -> {
                if (item != null) {
                    selectedTabs.add(item.getTabName());
                }
            });
            searchBox().setText("setting near the bottom");

            assertEquals(List.of("Second"), selectedTabs);
        });
    }

    private TextField searchBox() {
        return (TextField) dialog.getDialogPane().lookup("#searchBox");
    }

    private ScrollPane scrollPane() {
        return (ScrollPane) dialog.getDialogPane().lookup("#preferencesContainer");
    }

    private static PreferencesTab tab(String name, Label label) {
        return tab(name, label, new VBox(label));
    }

    private static PreferencesTab tab(String name, Label label, Node content) {
        PreferencesTab tab = mock(PreferencesTab.class);
        when(tab.getTabName()).thenReturn(name);
        when(tab.getTitle()).thenReturn(name);
        when(tab.getContent()).thenReturn(content);
        when(tab.getSearchableElements()).thenReturn(List.of(new SearchableElement(label.getText(), label)));
        when(tab.getSearchKeywords()).thenReturn(List.of(name));
        return tab;
    }
}
