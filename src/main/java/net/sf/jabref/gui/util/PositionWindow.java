/*  Copyright (C) 2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.util;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import net.sf.jabref.Globals;

public class PositionWindow {

    private final String posXKey;
    private final String posYKey;
    private final String sizeXKey;
    private final String sizeYKey;
    private final Window window;


    public PositionWindow(Window window, String posXKey, String posYKey, String sizeXKey, String sizeYKey) {
        this.posXKey = posXKey;
        this.posYKey = posYKey;
        this.sizeXKey = sizeXKey;
        this.sizeYKey = sizeYKey;
        this.window = window;
        // Set up a ComponentListener that saves the last size and position of the dialog
        window.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                // Save dialog position
                storeWindowPosition();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                // Save dialog position
                storeWindowPosition();
            }
        });

    }


    public void setWindowPosition() {

        int sizeX = Globals.prefs.getInt(sizeXKey);
        int sizeY = Globals.prefs.getInt(sizeYKey);
        int posX = Globals.prefs.getInt(posXKey);
        int posY = Globals.prefs.getInt(posYKey);

        //
        // Fix for [ 1738920 ] Windows Position in Multi-Monitor environment
        //
        // Do not put a window outside the screen if the preference values are wrong.
        //
        // Useful reference: http://www.exampledepot.com/egs/java.awt/screen_ScreenSize.html?l=rel
        // googled on forums.java.sun.com graphicsenvironment second screen java
        //
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length >= 1) {
            Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0]
                    .getDefaultConfiguration().getBounds();
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

            // Make sure we are not above or to the left of the screen bounds:
            if (posX < bounds.x) {
                posX = bounds.x;
            }
            if (posY < bounds.y) {
                posY = bounds.y;
            }

            int height = (int) dim.getHeight();
            int width = (int) dim.getWidth();

            if ((posX + sizeX) > width) {
                if (sizeX <= width) {
                    posX = width - sizeX;
                } else {
                    posX = Globals.prefs.getIntDefault(posXKey);
                    sizeX = Globals.prefs.getIntDefault(sizeXKey);
                }
            }

            if ((posY + sizeY) > height) {
                if (sizeY <= height) {
                    posY = height - sizeY;
                } else {
                    posY = Globals.prefs.getIntDefault(posYKey);
                    sizeY = Globals.prefs.getIntDefault(sizeYKey);
                }
            }
        }
        window.setLocation(posX, posY);
        window.setSize(sizeX, sizeY);

    }

    public void storeWindowPosition() {
        Point p = window.getLocation();
        Dimension d = window.getSize();
        Globals.prefs.putInt(posXKey, p.x);
        Globals.prefs.putInt(posYKey, p.y);
        Globals.prefs.putInt(sizeXKey, d.width);
        Globals.prefs.putInt(sizeYKey, d.height);
    }

}
