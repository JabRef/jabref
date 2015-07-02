package net.sf.jabref.splash;

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
