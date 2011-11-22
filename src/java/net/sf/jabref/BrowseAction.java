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
package net.sf.jabref;

import net.sf.jabref.gui.FileDialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

/**
 * Action used to produce a "Browse" button for one of the text fields.
 */
public class BrowseAction extends AbstractAction implements ActionListener {

	private static final long serialVersionUID = 3007593430933681310L;

    JComponent focusTarget = null;
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

    public void setFocusTarget(JComponent focusTarget) {
        this.focusTarget = focusTarget;
    }

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
            if (focusTarget != null)
                new FocusRequester(focusTarget);
        }
    }

}
