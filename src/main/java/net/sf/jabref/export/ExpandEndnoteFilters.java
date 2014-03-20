/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
