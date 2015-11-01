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

import net.sf.jabref.Globals;

public class PositionWindow {

    public static final String MERGEENTRIES = "mergeEntries";
    public static final String DUPLICATES = "duplicates";
    public static final String STRINGS = "strings";
    public static final String MAINWINDOW = "mainWindow";


    public static void setWindowPosition(Window window, String name) {

        String prefSizeX = name + "SizeX";
        String prefSizeY = name + "SizeY";
        String prefPosX = name + "PosX";
        String prefPosY = name + "PosY";

        int sizeX = Globals.prefs.getInt(prefSizeX);
        int sizeY = Globals.prefs.getInt(prefSizeY);
        int posX = Globals.prefs.getInt(prefPosX);
        int posY = Globals.prefs.getInt(prefPosY);

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
                    posX = Globals.prefs.getIntDefault(prefPosX);
                    sizeX = Globals.prefs.getIntDefault(prefSizeX);
                }
            }

            if ((posY + sizeY) > height) {
                if (sizeY <= height) {
                    posY = height - sizeY;
                } else {
                    posY = Globals.prefs.getIntDefault(prefPosY);
                    sizeY = Globals.prefs.getIntDefault(prefSizeY);
                }
            }
        }
        window.setLocation(posX, posY);
        window.setSize(sizeX, sizeY);

    }

    public static void storeWindowPosition(Window window, String name) {
        Point p = window.getLocation();
        Dimension d = window.getSize();
        Globals.prefs.putInt(name + "PosX", p.x);
        Globals.prefs.putInt(name + "PosY", p.y);
        Globals.prefs.putInt(name + "SizeX", d.width);
        Globals.prefs.putInt(name + "SizeY", d.height);
    }
}
