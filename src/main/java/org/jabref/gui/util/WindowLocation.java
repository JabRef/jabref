package org.jabref.gui.util;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;

import org.jabref.Globals;

/**
 * Restores and saves the position of non-modal windows inside the JabRef preferences.
 *
 * Includes multi-monitor support.
 * If a windows is placed on another monitor than the main one, it tries to restore that position afterwards.
 * If the stored position in a multi-monitor setup is not available anymore, it places the window on an equivalent position on the main monitor.
 */
public class WindowLocation {
    private final String posXKey;
    private final String posYKey;
    private final String sizeXKey;
    private final String sizeYKey;
    private final Window window;

    public WindowLocation(Window window, String posXKey, String posYKey, String sizeXKey, String sizeYKey) {
        this.window = window;
        this.posXKey = posXKey;
        this.posYKey = posYKey;
        this.sizeXKey = sizeXKey;
        this.sizeYKey = sizeYKey;

        // set up a ComponentListener that saves the last size and position of the dialog
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                storeCurrentWindowLocation();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                storeCurrentWindowLocation();
            }
        });
    }

    public void displayWindowAtStoredLocation() {
        WindowPosition storedPosition = getStoredLocation();

        // preference values are wrong/not in multi-monitor setup anymore
        if (!isDisplayable(storedPosition)) {
            // adapt position to be inside available boundaries
            storedPosition = adaptPosition(storedPosition);
        }

        setWindowLocation(storedPosition);
    }

    public void storeCurrentWindowLocation() {
        // maximizing is handled explicitely
        if (window instanceof Frame) {
            Frame frame = (Frame) window;
            if (frame.getExtendedState() == Frame.MAXIMIZED_BOTH) {
                return;
            }
        }
        Point location = window.getLocation();
        Dimension dimensions = window.getSize();

        Globals.prefs.putInt(posXKey, location.x);
        Globals.prefs.putInt(posYKey, location.y);
        Globals.prefs.putInt(sizeXKey, dimensions.width);
        Globals.prefs.putInt(sizeYKey, dimensions.height);
    }

    private WindowPosition getStoredLocation() {
        int sizeX = Globals.prefs.getInt(sizeXKey);
        int sizeY = Globals.prefs.getInt(sizeYKey);
        int posX = Globals.prefs.getInt(posXKey);
        int posY = Globals.prefs.getInt(posYKey);

        return new WindowPosition(posX, posY, sizeX, sizeY);
    }

    private void setWindowLocation(WindowPosition storedPosition) {
        window.setLocation(storedPosition.posX, storedPosition.posY);
        window.setSize(storedPosition.sizeX, storedPosition.sizeY);
    }

    private boolean isDisplayable(WindowPosition position) {
        JFrame frame = new JFrame();
        frame.setBounds(position.posX, position.posY, position.sizeX, position.sizeY);

        return getVirtualBounds().contains(frame.getBounds());
    }

    private Rectangle getVirtualBounds() {
        Rectangle bounds = new Rectangle(0, 0, 0, 0);
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        for (GraphicsDevice device : devices) {
            bounds.add(device.getDefaultConfiguration().getBounds());
        }
        return bounds;
    }

    private WindowPosition adaptPosition(WindowPosition position) {
        if (isDisplayable(position)) {
            return position;
        }

        // current algorithm:
        // 1. try to move to main screen
        // 2. use default sizes on main monitor
        GraphicsDevice mainScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int mainScreenHeight = mainScreen.getDisplayMode().getHeight();
        int mainScreenWidth = mainScreen.getDisplayMode().getWidth();

        int newPosX = position.posX;
        int newPosY = position.posY;
        int newSizeX = position.sizeX;
        int newSizeY = position.sizeY;

        if ((position.posX + position.sizeX) > mainScreenWidth) {
            if (position.sizeX <= mainScreenWidth) {
                newPosX = mainScreenWidth - position.sizeX;
            } else {
                newPosX = Globals.prefs.getIntDefault(posXKey);
                newSizeX = Globals.prefs.getIntDefault(sizeXKey);
            }
        }

        if ((position.posY + position.sizeY) > mainScreenHeight) {
            if (position.sizeY <= mainScreenHeight) {
                newPosY = mainScreenHeight - position.sizeY;
            } else {
                newPosY = Globals.prefs.getIntDefault(posYKey);
                newSizeY = Globals.prefs.getIntDefault(sizeYKey);
            }
        }

        return new WindowPosition(newPosX, newPosY, newSizeX, newSizeY);
    }

    static class WindowPosition {
        public final int posX;
        public final int posY;
        public final int sizeX;
        public final int sizeY;

        public WindowPosition(int posX, int posY, int sizeX, int sizeY) {
            this.posX = posX;
            this.posY = posY;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
        }
    }
}
