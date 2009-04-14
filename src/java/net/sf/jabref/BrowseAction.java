package net.sf.jabref;

import net.sf.jabref.gui.FileDialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextField;

/**
 * Action used to produce a "Browse" button for one of the text fields.
 */
public class BrowseAction extends AbstractAction implements ActionListener {

	private static final long serialVersionUID = 3007593430933681310L;

	JFrame frame=null;
    //JDialog dialog=null;
    JTextField comp;
    boolean dir;

    public BrowseAction(JFrame frame, JTextField tc, boolean dir) {
        super(Globals.lang("Browse"));
        this.frame = frame;
        this.dir = dir;
        comp = tc;

    }

    /*public BrowseAction(JDialog dialog, JTextField tc, boolean dir) {
        super(Globals.lang("Browse"));
        this.dialog = dialog;
        this.dir = dir;
        comp = tc;

    } */

    public void actionPerformed(ActionEvent e) {
        String chosen = null;
        if (dir)
            chosen = FileDialogs.getNewDir(frame, new File(comp.getText()), Globals.NONE,
                    JFileChooser.OPEN_DIALOG, false);
        else
            chosen = FileDialogs.getNewFile(frame, new File(comp.getText()), Globals.NONE,
                    JFileChooser.OPEN_DIALOG, false);
        if (chosen != null) {
            File newFile = new File(chosen);
            comp.setText(newFile.getPath());
        }
    }

}
