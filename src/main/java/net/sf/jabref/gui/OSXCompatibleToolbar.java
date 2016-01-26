package net.sf.jabref.gui;

import net.sf.jabref.logic.util.OS;

import javax.swing.*;
import java.awt.*;

public class OSXCompatibleToolbar extends JToolBar {

    public OSXCompatibleToolbar() {
    }

    public OSXCompatibleToolbar(int orientation) {
        super(orientation);
    }

    public OSXCompatibleToolbar(String name) {
        super(name);
    }

    public OSXCompatibleToolbar(String name, int orientation) {
        super(name, orientation);
    }

    @Override
    public Component add(Component a) {
        if (a instanceof JButton) {
            JButton button = (JButton) a;
            if (OS.OS_X) {
                button.putClientProperty("JButton.buttonType", "toolbar");
            }
        }

        return super.add(a);
    }

}
