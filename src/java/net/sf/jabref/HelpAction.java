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
package net.sf.jabref;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import java.net.URL;

/**
 * This Action keeps a reference to a URL. When activated, it shows the help
 * Dialog unless it is already visible, and shows the URL in it.
 */
public class HelpAction extends MnemonicAwareAction {

    protected HelpDialog diag;
    protected URL helpfile;
    protected String helpFile;

    public HelpAction(HelpDialog diag, String helpFile) {
	super(new ImageIcon(GUIGlobals.helpIconFile));
	putValue(NAME, "Help");
	this.diag = diag;
	this.helpFile = helpFile;
    }

    public HelpAction(HelpDialog diag, String helpFile, String tooltip) {
	super(new ImageIcon(GUIGlobals.helpIconFile));
	putValue(NAME, "Help");
	putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
	this.diag = diag;
	this.helpFile = helpFile;
    }

    public HelpAction(HelpDialog diag, String helpFile, String tooltip,
		      URL iconFile) {
	super(new ImageIcon(iconFile));
	putValue(NAME, "Help");
	putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
	this.diag = diag;
	this.helpFile = helpFile;
    }

    public HelpAction(String title, HelpDialog diag, String helpFile, String tooltip) {
	super(new ImageIcon(GUIGlobals.helpIconFile));
	putValue(NAME, title);
	putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
	this.diag = diag;
	this.helpFile = helpFile;
    }

    public HelpAction(String title, HelpDialog diag, String helpFile, String tooltip,
		      KeyStroke key) {
	super(new ImageIcon(GUIGlobals.helpIconFile));
	putValue(NAME, title);
	putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
	putValue(ACCELERATOR_KEY, key);
	this.diag = diag;
	this.helpFile = helpFile;
    }

    public HelpAction(String title, HelpDialog diag, String helpFile, String tooltip,
		      URL iconFile) {
	super(new ImageIcon(iconFile));
	putValue(NAME, title);
	putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
	this.diag = diag;
	this.helpFile = helpFile;
    }
/*
    public HelpAction(HelpDialog diag, URL helpfile, String tooltip) {
	super(Globals.lang("Help"), new ImageIcon(GUIGlobals.helpIconFile));
	putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
	this.diag = diag;
	this.helpfile = helpfile;
              Util.pr("6. HelpAction: "+helpFile+" "+helpfile);
    }

    public HelpAction(HelpDialog diag, URL helpfile, String tooltip,
		      URL iconFile) {
	super(Globals.lang("Help"), new ImageIcon(iconFile));
	putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
	this.diag = diag;
	this.helpfile = helpfile;
              Util.pr("7. HelpAction: "+helpFile+" "+helpfile);
    }

    public HelpAction(String title, HelpDialog diag, URL helpfile, String tooltip) {
	super(Globals.lang(title), new ImageIcon(GUIGlobals.helpIconFile));
	putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
	this.diag = diag;
	this.helpfile = helpfile;
              Util.pr("8. HelpAction: "+helpFile+" "+helpfile);
    }

    public HelpAction(String title, HelpDialog diag, URL helpfile, String tooltip,
		      KeyStroke key) {
	super(Globals.lang(title), new ImageIcon(GUIGlobals.helpIconFile));
	putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
	putValue(ACCELERATOR_KEY, key);
	this.diag = diag;
	this.helpfile = helpfile;
              Util.pr("9. HelpAction: "+helpFile+" "+helpfile);
    }
*/

    public JButton getIconButton() {
        JButton hlp = new JButton(this);
        hlp.setText(null);
        hlp.setPreferredSize(new Dimension(24, 24));
        return hlp;
    }

    public void actionPerformed(ActionEvent e) {
	diag.showPage(helpFile);
    }

}
