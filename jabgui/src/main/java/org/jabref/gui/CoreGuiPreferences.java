package org.jabref.gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CoreGuiPreferences {
    private final DoubleProperty positionX;
    private final DoubleProperty positionY;
    private final DoubleProperty sizeX;
    private final DoubleProperty sizeY;

    private final BooleanProperty windowMaximised;

    private final DoubleProperty horizontalDividerPosition;
    private final DoubleProperty verticalDividerPosition;

    private final StringProperty lastSelectedIdBasedFetcher;

    public CoreGuiPreferences(double positionX,
                              double positionY,
                              double sizeX,
                              double sizeY,
                              boolean windowMaximised,
                              String lastSelectedIdBasedFetcher,
                              double horizontalDividerPosition,
                              double verticalDividerPosition) {
        this.positionX = new SimpleDoubleProperty(positionX);
        this.positionY = new SimpleDoubleProperty(positionY);
        this.sizeX = new SimpleDoubleProperty(sizeX);
        this.sizeY = new SimpleDoubleProperty(sizeY);
        this.windowMaximised = new SimpleBooleanProperty(windowMaximised);
        this.lastSelectedIdBasedFetcher = new SimpleStringProperty(lastSelectedIdBasedFetcher);
        this.horizontalDividerPosition = new SimpleDoubleProperty(horizontalDividerPosition);
        this.verticalDividerPosition = new SimpleDoubleProperty(verticalDividerPosition);
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

    public String getLastSelectedIdBasedFetcher() {
        return lastSelectedIdBasedFetcher.get();
    }

    public StringProperty lastSelectedIdBasedFetcherProperty() {
        return lastSelectedIdBasedFetcher;
    }

    public void setLastSelectedIdBasedFetcher(String lastSelectedIdBasedFetcher) {
        this.lastSelectedIdBasedFetcher.set(lastSelectedIdBasedFetcher);
    }

    public double getHorizontalDividerPosition() {
        return horizontalDividerPosition.get();
    }

    public DoubleProperty horizontalDividerPositionProperty() {
        return horizontalDividerPosition;
    }

    public void setHorizontalDividerPosition(double horizontalDividerPosition) {
        this.horizontalDividerPosition.set(horizontalDividerPosition);
    }

    public double getVerticalDividerPosition() {
        return verticalDividerPosition.get();
    }

    public DoubleProperty getVerticalDividerPositionProperty() {
        return verticalDividerPosition;
    }

    public void setVerticalDividerPosition(double dividerPosition) {
        this.verticalDividerPosition.set(dividerPosition);
    }
}
