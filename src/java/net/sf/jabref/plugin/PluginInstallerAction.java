package net.sf.jabref.plugin;

import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MnemonicAwareAction;
import net.sf.jabref.Globals;
import net.sf.jabref.GUIGlobals;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Mar 27, 2009
 * Time: 11:33:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class PluginInstallerAction extends MnemonicAwareAction {
    private JabRefFrame frame;

    public PluginInstallerAction(JabRefFrame frame) {
        super(GUIGlobals.getImage("plugin"));
        //System.out.println();
        this.frame = frame;
        putValue(NAME, Globals.menuTitle("Manage plugins"));

    }
    
    public void actionPerformed(ActionEvent actionEvent) {
        ManagePluginsDialog mpd = new ManagePluginsDialog(frame);
        mpd.setVisible(true);
    }

    
}
