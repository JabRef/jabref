package org.jabref.gui.util;

/**
 * This class is used to store the size and position of dialog windows so that
 * these properties stay consistent when they are closed and re-opened
 */
public class DialogWindowState {
    private final double x;
    private final double y;
    private final double height;
    private final double width;

    public DialogWindowState(double x, double y, double height, double width) {
        this.x = x;
        this.y = y;
        this.height = height;
        this.width = width;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }
}
