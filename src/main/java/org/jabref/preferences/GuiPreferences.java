package org.jabref.preferences;

import java.nio.file.Path;
import java.util.List;

public class GuiPreferences {
    private final double positionX;
    private final double positionY;
    private final double sizeX;
    private final double sizeY;

    private final boolean windowMaximised;

    private final boolean shouldOpenLastEdited;
    private List<String> lastFilesOpened;
    private Path lastFocusedFile;

    public GuiPreferences(double positionX,
                          double positionY,
                          double sizeX,
                          double sizeY,
                          boolean windowMaximised,
                          boolean shouldOpenLastEdited,
                          List<String> lastFilesOpened,
                          Path lastFocusedFile) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.windowMaximised = windowMaximised;
        this.shouldOpenLastEdited = shouldOpenLastEdited;
        this.lastFilesOpened = lastFilesOpened;
        this.lastFocusedFile = lastFocusedFile;
    }

    public double getPositionX() {
        return positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public double getSizeX() {
        return sizeX;
    }

    public double getSizeY() {
        return sizeY;
    }

    public boolean isWindowMaximised() {
        return windowMaximised;
    }

    public boolean shouldOpenLastEdited() {
        return shouldOpenLastEdited;
    }

    public List<String> getLastFilesOpened() {
        return lastFilesOpened;
    }

    public GuiPreferences withLastFilesOpened(List<String> lastFilesOpened) {
        this.lastFilesOpened = lastFilesOpened;
        return this;
    }

    public Path getLastFocusedFile() {
        return lastFocusedFile;
    }

    public GuiPreferences withLastFocusedFile(Path lastFocusedFile) {
        this.lastFocusedFile = lastFocusedFile;
        return this;
    }
}
