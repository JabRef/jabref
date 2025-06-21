package org.jabref.gui.desktop.os;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.logic.os.OS;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.platform.win32.WinDef;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for controlling Windows-specific window title bar appearance.
 */
public class WindowTitleBarUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(WindowTitleBarUtil.class);

    private static final BooleanProperty DARK_MODE = new SimpleBooleanProperty(false);
    private static final AtomicBoolean IS_MONITORING_WINDOWS = new AtomicBoolean(false);

    static {
        if (OS.WINDOWS) {
            startMonitoringWindows();
            DARK_MODE.addListener((_, _, _) -> updateAllWindows());
        }
    }

    /**
     * Sets dark mode for all current and future windows
     *
     * @param darkMode true to enable dark mode, false for light mode
     */
    public static void setDarkMode(boolean darkMode) {
        DARK_MODE.set(darkMode);
    }

    /**
     * Updates all existing windows with the current dark mode setting
     */
    private static void updateAllWindows() {
        Window.getWindows().stream()
              .filter(Window::isShowing)
              .filter(Stage.class::isInstance)
              .map(Stage.class::cast)
              .forEach(stage -> setDarkMode(stage, DARK_MODE.get()));
    }

    /**
     * Starts monitoring for window changes to apply dark mode to new windows
     */
    private static void startMonitoringWindows() {
        if (IS_MONITORING_WINDOWS.getAndSet(true)) {
            return;
        }

        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().stream()
                          .filter(Stage.class::isInstance)
                          .map(Stage.class::cast)
                          .forEach(stage -> {
                              Platform.runLater(() -> setDarkMode(stage, DARK_MODE.get()));
                              stage.showingProperty().addListener((_, _, showing) -> {
                                  if (showing) {
                                      setDarkMode(stage, DARK_MODE.get());
                                  }
                              });
                          });
                }
            }
        });

        LOGGER.debug("Started monitoring windows for titlebar updates");
    }

    /**
     * Sets the dark mode state of a specific window title bar
     *
     * @param stage    the JavaFX Stage to modify
     * @param darkMode true to enable dark mode, false for light mode
     * @see <a href="https://stackoverflow.com/questions/42583779/how-to-change-the-color-of-title-bar-in-framework-javafx/76543216#76543216">...</a>
     */
    private static void setDarkMode(@NonNull Stage stage, boolean darkMode) {
        if (!OS.WINDOWS || !stage.isShowing()) {
            return;
        }

        getNativeHandleForStage(stage).ifPresent(hwnd -> {
            try {
                DWM dwmapi = DWM.INSTANCE;
                WinDef.BOOLByReference darkModeRef = new WinDef.BOOLByReference(new WinDef.BOOL(darkMode));
                int result = dwmapi.DwmSetWindowAttribute(hwnd, DWM.DWMWA_USE_IMMERSIVE_DARK_MODE, darkModeRef, Native.getNativeSize(WinDef.BOOL.class));

                if (result != 0) {
                    LOGGER.warn("DwmSetWindowAttribute failed with error code {} for stage: {}", result, stage);
                    return;
                }

                LOGGER.debug("Successfully set dark mode to {} for stage: {}", darkMode, stage);
                boolean success = User32.INSTANCE.PostMessage(hwnd, User32.WM_NCPAINT, new WinDef.WPARAM(1), new WinDef.LPARAM(0));
                if (!success) {
                    LOGGER.warn("PostMessage failed to repaint title bar for stage: {}", stage);
                }
            } catch (NullPointerException | UnsatisfiedLinkError e) {
                LOGGER.warn("Failed to set dark mode for stage: {}", stage, e);
            }
        });
    }

    private static Optional<WinDef.HWND> getNativeHandleForStage(Stage stage) {
        try {
            Method getPeer = Window.class.getDeclaredMethod("getPeer");
            getPeer.setAccessible(true);
            Object tkStage = getPeer.invoke(stage);
            Method getRawHandle = tkStage.getClass().getMethod("getRawHandle");
            getRawHandle.setAccessible(true);
            Pointer pointer = new Pointer((Long) getRawHandle.invoke(tkStage));
            return Optional.of(new WinDef.HWND(pointer));
        } catch (NoSuchMethodException |
                InvocationTargetException |
                IllegalAccessException e) {
            LOGGER.error("Reflection error while getting native handle for stage", e);
        }
        return Optional.empty();
    }

    private interface DWM extends Library {
        DWM INSTANCE = Native.load("dwmapi", DWM.class);

        int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;

        int DwmSetWindowAttribute(WinDef.HWND hwnd, int dwAttribute, PointerType pvAttribute, int cbAttribute);
    }

    private interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class);

        int WM_NCPAINT = 0x0085;

        boolean PostMessage(WinDef.HWND hWnd, int Msg, WinDef.WPARAM wParam, WinDef.LPARAM lParam);
    }
}
