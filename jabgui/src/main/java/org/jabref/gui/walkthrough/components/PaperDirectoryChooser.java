package org.jabref.gui.walkthrough.components;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chooses the main directory for storing and searching PDF files in JabRef.
 */
public class PaperDirectoryChooser extends HBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaperDirectoryChooser.class);
    private final Label currentDirectoryLabel;
    private final StringProperty currentDirectory;

    public PaperDirectoryChooser() {
        setSpacing(4);
        setPrefHeight(32);

        this.currentDirectory = new SimpleStringProperty();
        currentDirectoryLabel = new Label();
        currentDirectoryLabel.setWrapText(true);
        currentDirectoryLabel.setPrefHeight(32);
        HBox.setHgrow(currentDirectoryLabel, Priority.ALWAYS);
        currentDirectoryLabel.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button browseButton = new Button(Localization.lang("Browse..."));
        browseButton.setOnAction(_ -> showDirectoryChooser());

        getChildren().addAll(browseButton, currentDirectoryLabel);
        currentDirectory.addListener((_, _, newVal) -> updateDirectoryDisplay(newVal));
        updateCurrentDirectory();
    }

    private void updateCurrentDirectory() {
        GuiPreferences guiPreferences = Injector.instantiateModelOrService(GuiPreferences.class);
        FilePreferences filePreferences = guiPreferences.getFilePreferences();
        Optional<Path> mainFileDirectory = filePreferences.getMainFileDirectory();
        currentDirectory.set(mainFileDirectory.map(Path::toString).orElse(""));
    }

    private void updateDirectoryDisplay(String directory) {
        if (!directory.trim().isEmpty()) {
            currentDirectoryLabel.setText(Localization.lang("Current paper directory: %0", directory));
        } else {
            currentDirectoryLabel.setText(Localization.lang("No directory currently set."));
        }
    }

    private void showDirectoryChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(Localization.lang("Choose directory"));

        String currentDir = currentDirectory.get();
        if (currentDir != null && !currentDir.trim().isEmpty()) {
            File currentFile = new File(currentDir);
            if (currentFile.exists() && currentFile.isDirectory()) {
                directoryChooser.setInitialDirectory(currentFile);
            }
        }

        Window ownerWindow = getScene() != null ? getScene().getWindow() : null;
        File selectedDirectory = directoryChooser.showDialog(ownerWindow);

        if (selectedDirectory == null) {
            return;
        }

        try {
            GuiPreferences guiPreferences = Injector.instantiateModelOrService(GuiPreferences.class);
            FilePreferences filePreferences = guiPreferences.getFilePreferences();
            filePreferences.setMainFileDirectory(selectedDirectory.getAbsolutePath());
            currentDirectory.set(selectedDirectory.getAbsolutePath());
        } catch (Exception e) {
            currentDirectory.set(selectedDirectory.getAbsolutePath());
        }
    }
}
