package net.sf.jabref.testutils;

import java.awt.Component;
import java.awt.Container;

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
