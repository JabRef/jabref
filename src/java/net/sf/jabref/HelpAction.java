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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.KeyStroke;

/**
 * This Action keeps a reference to a URL. When activated, it shows the help
 * Dialog unless it is already visible, and shows the URL in it.
 */
public class HelpAction extends MnemonicAwareAction {

	protected HelpDialog diag;

    protected Class resourceOwner = null;

	protected URL helpfile;

	protected String helpFile;

	public HelpAction(HelpDialog diag, String helpFile) {
		super(GUIGlobals.getImage("help"));
		putValue(NAME, "Help");
		this.diag = diag;
		this.helpFile = helpFile;
	}

	public HelpAction(HelpDialog diag, String helpFile, String tooltip) {
		super(GUIGlobals.getImage("help"));
		putValue(NAME, "Help");
		putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
		this.diag = diag;
		this.helpFile = helpFile;
	}

	public HelpAction(HelpDialog diag, String helpFile, String tooltip, URL iconFile) {
		super(new ImageIcon(iconFile));
		putValue(NAME, "Help");
		putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
		this.diag = diag;
		this.helpFile = helpFile;
	}

	public HelpAction(String title, HelpDialog diag, String helpFile, String tooltip) {
		super(GUIGlobals.getImage("help"));
		putValue(NAME, title);
		putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
		this.diag = diag;
		this.helpFile = helpFile;
	}

	public HelpAction(String title, HelpDialog diag, String helpFile, String tooltip, KeyStroke key) {
		super(GUIGlobals.getImage("help"));
		putValue(NAME, title);
		putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
		putValue(ACCELERATOR_KEY, key);
		this.diag = diag;
		this.helpFile = helpFile;
	}

	public HelpAction(String title, HelpDialog diag, String helpFile, String tooltip, URL iconFile) {
		super(new ImageIcon(iconFile));
		putValue(NAME, title);
		putValue(SHORT_DESCRIPTION, Globals.lang(tooltip));
		this.diag = diag;
		this.helpFile = helpFile;
	}

    public void setResourceOwner(Class resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

	public JButton getIconButton() {
		JButton hlp = new JButton(this);
		hlp.setText(null);
		hlp.setPreferredSize(new Dimension(24, 24));
		return hlp;
	}

	public void actionPerformed(ActionEvent e) {
        if (resourceOwner == null)
		    diag.showPage(helpFile);
        else
            diag.showPage(helpFile, resourceOwner);
	}
}
