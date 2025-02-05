package org.jabref.gui;

import javafx.scene.control.Tab;

import org.jabref.gui.util.WelcomePage;

public class WelcomeTab extends Tab {

    public WelcomeTab(WelcomePage welcomePage) {
        setText("Welcome");
        setContent(welcomePage.getWelcomeMainContainer());

        setClosable(true);
    }
}
