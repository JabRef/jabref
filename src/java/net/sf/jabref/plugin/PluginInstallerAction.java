package net.sf.jabref.plugin;

import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MnemonicAwareAction;
import net.sf.jabref.Globals;

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
        this.frame = frame;
        putValue(NAME, Globals.menuTitle("Install plugin"));
    }

    public void actionPerformed(ActionEvent actionEvent) {
        File f = new File(Globals.getNewFile(frame, new File(System.getProperty("user.home")),
            ".jar", JFileChooser.OPEN_DIALOG, false));
        if (f != null) {
            if (!f.exists()) {
                JOptionPane.showMessageDialog(frame, Globals.lang("File not found")+".",
                        Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
            } else {
                try {
                    PluginInstaller.installPlugin(frame, new URL("file://"+f.getPath()));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
