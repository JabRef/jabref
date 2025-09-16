package org.jabref.gui.walkthrough.declarative;

import java.util.List;
import java.util.Optional;

import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;

/// Resolves windows using various strategies.
@FunctionalInterface
public interface WindowResolver {
    /// Resolves a window.
    ///
    /// @return an optional containing the found window, or empty if not found
    Optional<Window> resolve();

    /// Creates a resolver that finds a window by its title.
    ///
    /// @param key the language key of the window title
    /// @return a resolver that finds the window by title
    static WindowResolver title(@NonNull String key) {
        return () -> Window.getWindows().stream()
                           .filter(Window::isShowing)
                           .filter(Stage.class::isInstance)
                           .map(Stage.class::cast)
                           .filter(stage -> stage.getTitle().contains(Localization.lang(key)))
                           .map(Window.class::cast)
                           .findFirst();
    }

    /// Creates a resolver that finds a window that's not the window specified.
    ///
    /// @param window the window to exclude from the search. Usually this is the current
    ///                             window.
    /// @return a resolver that finds any window except the specified one
    static WindowResolver not(Window window) {
        return () -> {
            List<Window> windows = Window.getWindows()
                                         .stream()
                                         .filter(Window::isShowing)
                                         .filter(w -> !w.equals(window))
                                         .filter(Stage.class::isInstance)
                                         .toList();
            if (windows.size() > 1) {
                throw new IllegalStateException("More than one window resolved");
            }
            return windows.stream().findFirst();
        };
    }

    /// Create a resolver that finds a window by its class.
    ///
    /// @param clazz the class of the window
    /// @return a resolver that finds the window by class
    static WindowResolver clazz(@NonNull Class<? extends Window> clazz) {
        return () -> Window.getWindows().stream()
                           .filter(clazz::isInstance)
                           .filter(Window::isShowing)
                           .findFirst();
    }
}
