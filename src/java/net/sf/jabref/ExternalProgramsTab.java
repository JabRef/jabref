package net.sf.jabref;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

class ExternalProgramsTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    private GridBagLayout gbl = new GridBagLayout();
    private GridBagConstraints con = new GridBagConstraints();
    JTextField
	pdf, ps, html, lyx;

    public ExternalProgramsTab (JabRefPreferences prefs) {
	_prefs = prefs;

	pdf = new JTextField(_prefs.get("pdfviewer"), 30);
	ps = new JTextField(_prefs.get("psviewer"), 30);
	html = new JTextField(_prefs.get("htmlviewer"), 30);
	lyx = new JTextField(_prefs.get("lyxpipe"), 30);

	/*setBorder(BorderFactory.createTitledBorder
		  (BorderFactory.createEtchedBorder(),
		  Globals.lang("Paths to external programs")));*/
	JLabel lab;
	setLayout(gbl);
	con.weightx = 0;
	con.insets = new Insets(10, 10, 10, 10);
	con.fill = GridBagConstraints.HORIZONTAL;
	lab = new JLabel(Globals.lang("Path to PDF viewer")+":");
	gbl.setConstraints(lab, con);
        add(lab);
	con.weightx = 1;
	gbl.setConstraints(pdf, con);
	add(pdf);
	con.weightx = 0;
        con.gridwidth = GridBagConstraints.REMAINDER;
        JButton browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(new BrowseAction(pdf));
        gbl.setConstraints(browse, con);
        add(browse);
        con.gridwidth = 1;
	con.fill = GridBagConstraints.HORIZONTAL;
	lab = new JLabel(Globals.lang("Path to PS viewer")+":");
	gbl.setConstraints(lab, con);
	add(lab);
	con.weightx = 1;
	gbl.setConstraints(ps, con);
	add(ps);
	con.weightx = 0;
        con.gridwidth = GridBagConstraints.REMAINDER;
        browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(new BrowseAction(ps));
        gbl.setConstraints(browse, con);
        add(browse);
	con.gridwidth = 1;
	lab = new JLabel(Globals.lang("Path to HTML viewer")+":");
	gbl.setConstraints(lab, con);
	add(lab);
	con.weightx = 1;
	gbl.setConstraints(html, con);
	add(html);
        con.gridwidth = GridBagConstraints.REMAINDER;
	con.weightx = 0;
        browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(new BrowseAction(html));
        gbl.setConstraints(browse, con);
        add(browse);
	con.gridwidth = 1;
	con.fill = GridBagConstraints.HORIZONTAL;
	lab = new JLabel(Globals.lang("Path to LyX pipe")+":");
	gbl.setConstraints(lab, con);
	add(lab);
        con.weightx = 1;
	gbl.setConstraints(lyx, con);
	add(lyx);
        con.weightx = 0;
        browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(new BrowseAction(lyx));
        gbl.setConstraints(browse, con);
        add(browse);

    }

  /**
   * Action used to produce a "Browse" button for one of the text fields.
   */
  class BrowseAction extends AbstractAction {
      JTextField comp;
      public BrowseAction(JTextField tc) {
        super(Globals.lang("Browse"));
        comp = tc;
      }
      public void actionPerformed(ActionEvent e) {
        JabRefFileChooser chooser = new JabRefFileChooser(new File(comp.getText()));
        //chooser.addChoosableFileFilter(new OpenFileFilter()); //nb nov2
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          File newFile = chooser.getSelectedFile();
          comp.setText(newFile.getPath());
        }
      }
    }

    public void setValues() {

    }

    /**
     * Store changes to table preferences. This method is called when
     * the user clicks Ok.
     *
     */
    public void storeSettings() {

	// We should maybe do some checking on the validity of the contents?

	_prefs.put("pdfviewer", pdf.getText());
	_prefs.put("psviewer", ps.getText());
	_prefs.put("htmlviewer", html.getText());
	_prefs.put("lyxpipe", lyx.getText());
    }

    public boolean readyToClose() {
	return true;
    }

}



