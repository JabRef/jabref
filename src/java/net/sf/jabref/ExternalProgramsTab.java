package net.sf.jabref;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

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
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(pdf, con);
	add(pdf);
	con.weightx = 0;
	con.gridwidth = 1;
	con.fill = GridBagConstraints.HORIZONTAL;
	lab = new JLabel(Globals.lang("Path to PS viewer")+":");
	gbl.setConstraints(lab, con);
	add(lab);
	con.weightx = 1;
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(ps, con);
	add(ps);
	con.weightx = 0;
	con.gridwidth = 1;
	lab = new JLabel(Globals.lang("Path to HTML viewer")+":");
	gbl.setConstraints(lab, con);
	add(lab);
	con.weightx = 1;
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(html, con);
	add(html);
	con.weightx = 0;
	con.gridwidth = 1;
	con.fill = GridBagConstraints.HORIZONTAL;
	lab = new JLabel(Globals.lang("Path to LyX pipe")+":");
	gbl.setConstraints(lab, con);
	add(lab);
	con.weightx = 1;
	con.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(lyx, con);
	add(lyx);

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
}



