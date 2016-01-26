package net.sf.jabref.gui;

import net.sf.jabref.logic.util.OS;

import javax.swing.*;

public class OSXCompatibleToolbar extends JToolBar {

    @Override
    public JButton add(Action a) {
        JButton button = super.add(a);
        if (OS.OS_X) {
            button.putClientProperty("JButton.buttonType", "toolbar");
        }
        return button;
    }

}
