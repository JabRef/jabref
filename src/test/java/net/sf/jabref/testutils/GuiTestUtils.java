/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.testutils;

import java.awt.*;

/**
 * Provides helper methods for making testing of GUIs easier.
 *
 * @author Dennis Hartrampf, Ines Moosdorf
 */
public class GuiTestUtils {

    /**
     * Get a Component by name.
     *
     * @param parent The parent Component, where to search in.
     * @param name   The name of the Component to find.
     * @return The Component with the given name or null if no
     * such Component.
     */
    public static Component getChildNamed(Component parent, String name) {
        if (name.equals(parent.getName())) {
            return parent;
        }

        if (parent instanceof Container) {
            Component[] children = ((Container) parent).getComponents();

            for (Component aChildren : children) {
                Component child = GuiTestUtils.getChildNamed(aChildren, name);
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }
}
