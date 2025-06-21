package org.jabref.gui.desktop.os;

import javafx.collections.ListChangeListener;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.os.OS;

import com.pixelduke.window.ThemeWindowManager;
import com.pixelduke.window.ThemeWindowManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener controlling platform-specific window title bar appearance using FXThemes.
 */
public class ThemeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeListener.class);
    private static final ThemeWindowManager THEME_WINDOW_MANAGER = ThemeWindowManagerFactory.create();
    private static final boolean SUPPORTS_DARK_MODE = OS.WINDOWS || OS.OS_X;
    private static boolean initialized = false;
    private static boolean currentDarkMode = false;

    public static void initialize(boolean darkMode) {
        if (initialized || !SUPPORTS_DARK_MODE) {
            return;
        }

        currentDarkMode = darkMode;

        ListChangeListener<Window> windowsListener = change -> {
            while (change.next()) {
                if (!change.wasAdded()) {
                    continue;
                }
                change.getAddedSubList().stream()
                      .filter(Stage.class::isInstance)
                      .map(Stage.class::cast)
                      .forEach(stage -> {
                          BindingsHelper.subscribeFuture(stage.showingProperty(), showing -> {
                              if (showing) {
                                  setDarkMode(stage, currentDarkMode);
                              }
                          });
                          if (stage.isShowing()) {
                              setDarkMode(stage, currentDarkMode);
                          }
                      });
            }
        };

        Window.getWindows().addListener(windowsListener);
        setDarkMode(darkMode);

        initialized = true;
        LOGGER.debug("ThemeListener initialized with window monitoring");
    }

    public static void setDarkMode(Stage stage, boolean darkMode) {
        if (!SUPPORTS_DARK_MODE || stage == null || !stage.isShowing()) {
            return;
        }

        THEME_WINDOW_MANAGER.setDarkModeForWindowFrame(stage, darkMode);
        LOGGER.debug("Applied {} mode to window: {}", darkMode ? "dark" : "light", stage);
    }

    public static void setDarkMode(boolean darkMode) {
        currentDarkMode = darkMode;
        Window.getWindows().stream()
              .filter(Window::isShowing)
              .filter(window -> window instanceof Stage)
              .map(window -> (Stage) window)
              .forEach(stage -> setDarkMode(stage, darkMode));
    }
}
