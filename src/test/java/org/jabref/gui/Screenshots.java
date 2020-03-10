package org.jabref.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.function.Function;

import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.jabref.Globals;
import org.jabref.JabRefException;
import org.jabref.JabRefGUI;
import org.jabref.JabRefMain;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.framework.junit5.Stop;
import org.testfx.service.support.CaptureSupport;
import org.testfx.util.DebugUtils;

@GUITest
@ExtendWith(ApplicationExtension.class)
public class Screenshots {

    private static final Logger LOGGER = LoggerFactory.getLogger(Screenshots.class);
    final static Path root = Path.of("build/screenshots").toAbsolutePath();

    @BeforeAll
    static void createDirectory() throws IOException {
        if (!Files.exists(root)) {
            Files.createDirectory(root);
        }
    }

    @Start
    public void onStart(Stage stage) throws JabRefException {
        final JabRefPreferences preferences = JabRefPreferences.getInstance();
        Globals.prefs = preferences;
        preferences.putBoolean(JabRefPreferences.WINDOW_MAXIMISED, false);
        preferences.putDouble(JabRefPreferences.POS_X, 0);
        preferences.putDouble(JabRefPreferences.POS_Y, 0);
        preferences.putDouble(JabRefPreferences.SIZE_X, 1024);
        preferences.putDouble(JabRefPreferences.SIZE_Y, 768);
        JabRefMain.applyPreferences(preferences);
        Globals.startBackgroundTasks();
        new JabRefGUI(stage, Collections.emptyList(), true);
    }

    @Stop
    public void onStop() {
        Globals.stopBackgroundTasks();
    }

    @Test
    @Order(1)
    void screenshotMainMenu() {
        StringBuilder stringBuilder = new StringBuilder();
        DebugUtils.saveScreenshot(() -> root.resolve("main-empty.png"), "").apply(stringBuilder);
        LOGGER.debug(stringBuilder.toString());
    }

    @Test
    @Order(2)
    void screenshotMainMenuFile(FxRobot robot) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        Path jabrefAuthorsBib = Path.of("src/test/resources/testbib/jabref-authors.bib");
        Path jabrefAuthorsBibSav = jabrefAuthorsBib.getParent().resolve(jabrefAuthorsBib.getFileName() + ".sav");
        Files.deleteIfExists(jabrefAuthorsBibSav);
        JabRefGUI.getMainFrame().getOpenDatabaseAction().openFile(jabrefAuthorsBib, true);
        // give database some time load
        robot.interrupt(100);
        DebugUtils.saveScreenshot(() -> root.resolve("main-jabref-authors.png"), "").apply(stringBuilder);
        LOGGER.debug(stringBuilder.toString());
    }
}
