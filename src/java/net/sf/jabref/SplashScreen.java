package net.sf.jabref;

//import javax.swing.*;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.net.URL;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class SplashScreen extends Window {
    private Image splashImage;
    private boolean paintCalled = false;
    
    public SplashScreen(Frame owner) {
        super(owner);
        URL imageURL = SplashScreen.class.getResource("/images/splash.png");
        splashImage = Toolkit.getDefaultToolkit().createImage(imageURL);

        // Load the image
        MediaTracker mt = new MediaTracker(this);
        mt.addImage(splashImage,0);
        try {
            mt.waitForID(0);
        } catch(InterruptedException ie) {}

                                                                                
        // Center the window on the screen.
        int imgWidth = splashImage.getWidth(this);
        int imgHeight = splashImage.getHeight(this);  

        setSize(imgWidth, imgHeight);
        setLocationRelativeTo(null);
        
        /* Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(
            (screenDim.width - imgWidth) / 2,
            (screenDim.height - imgHeight) / 2
        );
        */

    }
    
    
    /**
     * Updates the display area of the window.
     */
    public void update(Graphics g) {
        // Note: Since the paint method is going to draw an
        // image that covers the complete area of the component we
        // do not fill the component with its background color
        // here. This avoids flickering.

        g.setColor(getForeground());
        paint(g);
    }
    /**
     * Paints the image on the window.
     */

    public void paint(Graphics g) {
        g.drawImage(splashImage, 0, 0, this);

        // Notify method splash that the window
        // has been painted.
        if (! paintCalled) {
            paintCalled = true;
            synchronized (this) { notifyAll(); }
        }
    }
    
      /**
     * Constructs and displays a SplashWindow.<p>
     * This method is useful for startup splashs.
     * Dispose the returned frame to get rid of the splash window.<p>
     *
     * @param splashImage The image to be displayed.
     * @return Returns the frame that owns the SplashWindow.
     */

    public static Frame splash() {
        Frame f = new Frame();
        SplashScreen w = new SplashScreen(f);

        // Show the window.
        w.setVisible(true);
        w.toFront();

        // Note: To make sure the user gets a chance to see the
        // splash window we wait until its paint method has been
        // called at least once by the AWT event dispatcher thread.

        // sebwills adds: However, just in case the paint method never gets called
        // (e.g. if the splashscreen is completely obscured by an 'always on top'
        // window of some other application), we time-out after 5 seconds.
        if (! EventQueue.isDispatchThread()) {
            synchronized (w) {
                if (! w.paintCalled) {
                    try { 
                        w.wait(5000);
                    } catch (InterruptedException e) {}
                }
            }
        }
        return f;
    }
}
