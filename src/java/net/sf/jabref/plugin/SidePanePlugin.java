package net.sf.jabref.plugin;

import net.sf.jabref.JabRefFrame;
import net.sf.jabref.SidePaneComponent;
import net.sf.jabref.SidePaneManager;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Nov 26, 2007
 * Time: 5:44:16 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SidePanePlugin {

    public void init(JabRefFrame frame, SidePaneManager manager);
    
    public SidePaneComponent getSidePaneComponent();

    public JMenuItem getMenuItem();

    public String getShortcutKey();
}
