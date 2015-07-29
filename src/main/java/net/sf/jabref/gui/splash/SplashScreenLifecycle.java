package net.sf.jabref.gui.splash;

import java.awt.Frame;

public class SplashScreenLifecycle {

    private Frame splashScreen;

    public void show() {
        splashScreen = SplashScreen.splash();
    }

    public void hide() {
        if (splashScreen != null) {
            splashScreen.dispose();
            splashScreen = null;
        }
    }
}
