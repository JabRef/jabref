package org.jabref.preferences;

import java.nio.file.Path;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.mergeentries.DiffMode;
import org.jabref.logic.util.io.FileHistory;

public class GuiPreferences {
    private final DoubleProperty positionX;
    private final DoubleProperty positionY;
    private final DoubleProperty sizeX;
    private final DoubleProperty sizeY;

    private final BooleanProperty windowMaximised;

    private final ObservableList<String> lastFilesOpened;
    private final ObjectProperty<Path> lastFocusedFile;
    private final FileHistory fileHistory;

    private final StringProperty lastSelectedIdBasedFetcher;

    private final ObjectProperty<DiffMode> mergeDiffMode;

    public GuiPreferences(double positionX,
                          double positionY,
                          double sizeX,
                          double sizeY,
                          boolean windowMaximised,
                          List<String> lastFilesOpened,
                          Path lastFocusedFile,
                          FileHistory fileHistory,
                          String lastSelectedIdBasedFetcher,
                          DiffMode mergeDiffMode) {
        this.positionX = new SimpleDoubleProperty(positionX);
        this.positionY = new SimpleDoubleProperty(positionY);
        this.sizeX = new SimpleDoubleProperty(sizeX);
        this.sizeY = new SimpleDoubleProperty(sizeY);
        this.windowMaximised = new SimpleBooleanProperty(windowMaximised);
        this.lastFilesOpened = FXCollections.observableArrayList(lastFilesOpened);
        this.lastFocusedFile = new SimpleObjectProperty<>(lastFocusedFile);
        this.lastSelectedIdBasedFetcher = new SimpleStringProperty(lastSelectedIdBasedFetcher);
        this.mergeDiffMode = new SimpleObjectProperty<>(mergeDiffMode);
        this.fileHistory = fileHistory;
    }

    public double getPositionX() {
        return positionX.get();
    }

    public DoubleProperty positionXProperty() {
        return positionX;
    }

    public void setPositionX(double positionX) {
        this.positionX.set(positionX);
    }

    public double getPositionY() {
        return positionY.get();
    }

    public DoubleProperty positionYProperty() {
        return positionY;
    }

    public void setPositionY(double positionY) {
        this.positionY.set(positionY);
    }

    public double getSizeX() {
        return sizeX.get();
    }

    public DoubleProperty sizeXProperty() {
        return sizeX;
    }

    public void setSizeX(double sizeX) {
        this.sizeX.set(sizeX);
    }

    public double getSizeY() {
        return sizeY.get();
    }

    public DoubleProperty sizeYProperty() {
        return sizeY;
    }

    public void setSizeY(double sizeY) {
        this.sizeY.set(sizeY);
    }

    public boolean isWindowMaximised() {
        return windowMaximised.get();
    }

    public BooleanProperty windowMaximisedProperty() {
        return windowMaximised;
    }

    public void setWindowMaximised(boolean windowMaximised) {
        this.windowMaximised.set(windowMaximised);
    }

    public ObservableList<String> getLastFilesOpened() {
        return lastFilesOpened;
    }

    public void setLastFilesOpened(List<String> files) {
        lastFilesOpened.setAll(files);
    }

    public Path getLastFocusedFile() {
        return lastFocusedFile.get();
    }

    public ObjectProperty<Path> lastFocusedFileProperty() {
        return lastFocusedFile;
    }

    public void setLastFocusedFile(Path lastFocusedFile) {
        this.lastFocusedFile.set(lastFocusedFile);
    }

    public FileHistory getFileHistory() {
        return fileHistory;
    }

    public String getLastSelectedIdBasedFetcher() {
        return lastSelectedIdBasedFetcher.get();
    }

    public StringProperty lastSelectedIdBasedFetcherProperty() {
        return lastSelectedIdBasedFetcher;
    }

    public void setLastSelectedIdBasedFetcher(String lastSelectedIdBasedFetcher) {
        this.lastSelectedIdBasedFetcher.set(lastSelectedIdBasedFetcher);
    }

    public DiffMode getMergeDiffMode() {
        return mergeDiffMode.get();
    }

    public ObjectProperty<DiffMode> mergeDiffModeProperty() {
        return mergeDiffMode;
    }

    public void setMergeDiffMode(DiffMode mergeDiffMode) {
        this.mergeDiffMode.set(mergeDiffMode);
    }
}
