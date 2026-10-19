package org.jabref.gui.frame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.event.Event;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.testutils.JavaFxExtension;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxExtension.class)
@NullMarked
@ResourceLock("Localization.lang")
class JabRefFrameTest {

    @Test
    void closesSelectedWelcomeTabAndRunsItsCleanup() {
        Tab welcomeTab = new Tab("Welcome");
        AtomicBoolean cleanedUp = new AtomicBoolean();
        welcomeTab.setOnClosed(_ -> cleanedUp.set(true));
        TabPane tabbedPane = new TabPane(welcomeTab);
        tabbedPane.getSelectionModel().select(welcomeTab);

        JabRefFrame.closeSelectedNonLibraryTab(tabbedPane);

        assertEquals(List.of(), tabbedPane.getTabs());
        assertTrue(cleanedUp.get());
    }

    @Test
    void keepsTabVetoingItsClose() {
        Tab vetoingTab = new Tab("Vetoing");
        vetoingTab.setOnCloseRequest(Event::consume);
        TabPane tabbedPane = new TabPane(vetoingTab);
        tabbedPane.getSelectionModel().select(vetoingTab);

        JabRefFrame.closeSelectedNonLibraryTab(tabbedPane);

        assertEquals(List.of(vetoingTab), tabbedPane.getTabs());
    }

    @Test
    void keepsNonClosableTab() {
        Tab fixedTab = new Tab("Fixed");
        fixedTab.setClosable(false);
        TabPane tabbedPane = new TabPane(fixedTab);
        tabbedPane.getSelectionModel().select(fixedTab);

        JabRefFrame.closeSelectedNonLibraryTab(tabbedPane);

        assertEquals(List.of(fixedTab), tabbedPane.getTabs());
    }

    // [utest->req~ai.chat.find.unavailable~1]
    @Test
    void librarySearchRunsAfterUnconsumedLocalSearch() {
        Region target = new Region();
        StackPane root = new StackPane(target);
        List<String> handledBy = new ArrayList<>();
        target.addEventHandler(KeyEvent.KEY_PRESSED, _ -> handledBy.add("local"));
        JabRefFrame.installSearchKeyBinding(root, searchKeyBindings(), () -> handledBy.add("library"));

        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F10, false, false, false, false));

        assertEquals(List.of("local", "library"), handledBy);
    }

    // [utest->req~ai.chat.find.open~1]
    @Test
    void consumedLocalSearchDoesNotReachLibrarySearch() {
        Region target = new Region();
        StackPane root = new StackPane(target);
        List<String> handledBy = new ArrayList<>();
        target.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            handledBy.add("local");
            event.consume();
        });
        JabRefFrame.installSearchKeyBinding(root, searchKeyBindings(), () -> handledBy.add("library"));

        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F10, false, false, false, false));

        assertEquals(List.of("local"), handledBy);
    }

    @Test
    void unrelatedShortcutDoesNotFocusLibrarySearch() {
        StackPane root = new StackPane();
        List<String> handledBy = new ArrayList<>();
        JabRefFrame.installSearchKeyBinding(root, searchKeyBindings(), () -> handledBy.add("library"));

        Event.fireEvent(root, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F11, false, false, false, false));

        assertEquals(List.of(), handledBy);
    }

    private static KeyBindingRepository searchKeyBindings() {
        KeyBindingRepository keyBindings = mock(KeyBindingRepository.class);
        when(keyBindings.matches(any(KeyEvent.class), eq(KeyBinding.SEARCH)))
                .thenAnswer(invocation -> ((KeyEvent) invocation.getArgument(0)).getCode() == KeyCode.F10);
        return keyBindings;
    }
}
