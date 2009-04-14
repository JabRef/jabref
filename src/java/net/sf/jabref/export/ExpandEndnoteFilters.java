/*
 * ExpandEndnoteFilters.java
 *
 * Created on January 22, 2005, 6:31 PM
 */

package net.sf.jabref.export;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MnemonicAwareAction;
import net.sf.jabref.Worker;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.util.ResourceExtractor;
import spin.Spin;

/**
 *
 * @author alver
 */
public class ExpandEndnoteFilters extends MnemonicAwareAction implements Worker {
    
    JabRefFrame frame;
    File file = null;
    final String FILENAME = "/EndNote.zip";
    
    /** Creates a new instance of ExpandEndnoteFilters */
    public ExpandEndnoteFilters(JabRefFrame frame) {
        this.frame = frame;
        putValue(NAME, "Unpack EndNote filter set");
        putValue(SHORT_DESCRIPTION, Globals.lang("<HTML>Unpack the zip file containing import/export filters for Endnote,<BR>"
                +"for optimal interoperability with JabRef</HTML>"));
    }
    
    public void actionPerformed(ActionEvent e) {
        
        String filename = FileDialogs.getNewFile(frame, new File(System.getProperty("user.home")), ".zip",
                JFileChooser.SAVE_DIALOG, false); 
        
        if (filename == null)
            return;
        
        //if (!filename.substring(4).equalsIgnoreCase(".zip"))
        //    filename += ".zip";
        file = new File(filename);
        if (file.exists()) {
            int confirm = JOptionPane.showConfirmDialog(frame, "'"+file.getName()+"' "+
                          Globals.lang("exists. Overwrite file?"),
                          Globals.lang("Unpack EndNote filter set"), JOptionPane.OK_CANCEL_OPTION);
            if (confirm != JOptionPane.OK_OPTION)
                return;
        }
        
        // Spin off the GUI thread, and run the run() method.
       ((Worker)Spin.off(this)).run(); 
       
       file = null;
    }
    
    /**
     * Worker method.
     */
    public void run() {
        ResourceExtractor re = new ResourceExtractor(frame, FILENAME, file);
        re.run();
        frame.output(Globals.lang("Unpacked file."));
    }
}
