package net.sf.jabref;

import javax.swing.*;
import java.awt.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class SplashScreen extends JWindow {
    public SplashScreen() {
        ImageIcon image = null;
        JLabel label = new JLabel(image);
        try {
            image = new ImageIcon(GUIGlobals.splashScreenImage);
            label = new JLabel(image);
        } catch (Exception ex) {
            ex.printStackTrace();
            label = new JLabel("unable to load splash image");
        }
        getContentPane().add(label);
        pack();
        Dimension dim =
            Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int)(dim.getWidth() - getWidth())/2;
        int y = (int)(dim.getHeight() - getHeight())/2;
        setLocation(x,y);
    }
}
