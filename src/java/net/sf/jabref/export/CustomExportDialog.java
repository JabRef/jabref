/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/
package net.sf.jabref.export;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Vector;
import java.io.File;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Util;
import net.sf.jabref.Globals;
import net.sf.jabref.GUIGlobals;

/**
 * Dialog for creating or modifying custom exports.
 */
class CustomExportDialog extends JDialog {

    JTextField
	name = new JTextField(60),
	layoutFile = new JTextField(60),
	extension = new JTextField(60);
    JLabel
	nl = new JLabel(Globals.lang("Export name")+":"),
	nr = new JLabel(Globals.lang("Main layout file")+":"),
	nf = new JLabel(Globals.lang("File extension")+":");
    JButton
        ok = new JButton(Globals.lang("Ok")),
        cancel = new JButton(Globals.lang("Cancel")),
        browse = new JButton(Globals.lang("Browse"));
    JPanel
	main = new JPanel(),
	opt = new JPanel();
    private boolean ok_pressed = false;
    private Vector groups;
    private int index;
    private JabRefFrame parent;

    private String /*name, regexp, field,*/ oldName, oldRegexp, oldField;

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();


    public CustomExportDialog(JabRefFrame parent_, String name_, String layoutFile_, String extension_) {
      this(parent_);
      name.setText(name_);
      layoutFile.setText(layoutFile_);
      extension.setText(extension_);
    }


    public CustomExportDialog(JabRefFrame parent_) {
	super(parent_, Globals.lang("Edit custom export"), true);
	parent = parent_;
	//groups = groups_;
	//index = index_;
	/*if (index >= 0) {
	    // Group entry already exists.
	    try {
		oldField = (String)groups.elementAt(index);
		field.setText(oldField);
		oldName = (String)groups.elementAt(index+1);
		name.setText(oldName);
		oldRegexp = (String)groups.elementAt(index+2);
		regexp.setText(oldRegexp);

		// We disable these text fields, since changing field
		// or regexp would leave the entries added to the
		// group hanging.
		field.setEnabled(false);
		regexp.setEnabled(false);
	    } catch (ArrayIndexOutOfBoundsException ex) {
	    }
	} else
	    field.setText(defaultField);
*/
	ActionListener okListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {

		    // Check that there are no empty strings.
		    if ((layoutFile.getText().equals("")) ||
			(name.getText().equals("")) ||
			(extension.getText().equals("")) ||
                        (!layoutFile.getText().endsWith(".layout"))) {
			//JOptionPane.showMessageDialog
			//    (parent, Globals.lang("You must provide a name, a search "
			//			  +"string and a field name for this group."),
			//			  Globals.lang("Create group"),
			//     JOptionPane.ERROR_MESSAGE);
			return;
		    }

		    // Handling of : and ; must also be done.

		    ok_pressed = true;
		    dispose();
		}
	    };
	ok.addActionListener(okListener);
	name.addActionListener(okListener);
	layoutFile.addActionListener(okListener);
	extension.addActionListener(okListener);

	AbstractAction cancelAction = new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
              dispose();
		}
	    };

	cancel.addActionListener(cancelAction);

        browse.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            File directory = new File(Globals.prefs.get("exportWorkingDirectory"));
            String chosenStr = Globals.getNewFile(parent, Globals.prefs, directory, ".layout",
                                             JFileChooser.OPEN_DIALOG, false);
            if (chosenStr == null) return;
            File chosen = new File(chosenStr);

            // Update working directory for layout files.
            Globals.prefs.put("exportWorkingDirectory", chosen.getParent());

            layoutFile.setText(chosen.getPath());
          }
        });

        // Key bindings:
        ActionMap am = main.getActionMap();
        InputMap im = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(parent.prefs().getKey("Close dialog"), "close");
	am.put("close", cancelAction);


	// Layout starts here.
	main.setLayout(gbl);
	//opt.setLayout(gbl);
	main.setBorder(BorderFactory.createTitledBorder
		       (BorderFactory.createEtchedBorder(),
			Globals.lang("Export properties")));

	// Main panel:
	con.weightx = 0;
	con.gridwidth = 1;
	con.insets = new Insets(3, 5, 3, 5);
	con.anchor = GridBagConstraints.EAST;
	con.fill = GridBagConstraints.NONE;
	con.gridx = 0;
	con.gridy = 0;
	gbl.setConstraints(nl, con);
	main.add(nl);
	con.gridy = 1;
	gbl.setConstraints(nr, con);
	main.add(nr);
	con.gridy = 2;
	gbl.setConstraints(nf, con);
	main.add(nf);

        con.gridwidth = 2;
        con.weightx = 1;
	con.anchor = GridBagConstraints.WEST;
	con.fill = GridBagConstraints.HORIZONTAL;
	con.gridy = 0;
	con.gridx = 1;
	gbl.setConstraints(name, con);
	main.add(name);
	con.gridy = 1;
        con.gridwidth = 1;
        gbl.setConstraints(layoutFile, con);
	main.add(layoutFile);
        con.gridx = 2;
        con.weightx = 0;
        gbl.setConstraints(browse, con);
        main.add(browse);
        con.weightx = 1;
        con.gridwidth = 2;
        con.gridx = 1;
	con.gridy = 2;
	gbl.setConstraints(extension, con);
	main.add(extension);

       	// Option buttons:
	con.gridx = GridBagConstraints.RELATIVE;
	con.gridy = GridBagConstraints.RELATIVE;
	con.weightx = 1;
	con.gridwidth = 1;
	con.anchor = GridBagConstraints.EAST;
	con.fill = GridBagConstraints.NONE;
	//gbl.setConstraints(ok, con);
	opt.add(ok);
	con.anchor = GridBagConstraints.WEST;
	con.gridwidth = GridBagConstraints.REMAINDER;
	//gbl.setConstraints(cancel, con);
	opt.add(cancel);

	getContentPane().add(main, BorderLayout.CENTER);
	getContentPane().add(opt, BorderLayout.SOUTH);

	//pack();
	setSize(600, 170);

        Util.placeDialog(this, parent);
    }

    public boolean okPressed() {
	return ok_pressed;
    }

    public int index() { return index; }
    public String oldField() { return oldField; }
    public String oldName() { return oldName; }
    public String oldRegexp() { return oldRegexp; }
    public String layoutFile() { return layoutFile.getText(); }
    public String name() { return name.getText(); }
    public String extension() {
      String ext = extension.getText();
      if (ext.startsWith("."))
        return ext;
      else if (ext.startsWith("*."))
        return ext.substring(1);
      else
        return "."+ext;
    }

}
