package org.jabref.gui.util;

/**
 * This class is used to store the size and position of dialog windows so that
 * these properties stay consistent when they are closed and re-opened
 */
public class DialogWindowState {
    private double x;
    private double y;
    private double height;
    private double width;

    public DialogWindowState() {
    }

    public boolean isNull() {
        return this.x == 0 && this.y == 0 && this.height == 0 && this.width == 0;
    }

    public void setAll(double x, double y, double height, double width) {
        this.x = x;
        this.y = y;
        this.height = height;
        this.width = width;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getHeight() {
        return this.height;
    }

    public double getWidth() {
        return this.width;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public void setWidth(double width) {
        this.width = width;
    }
}
