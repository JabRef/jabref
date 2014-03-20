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
package net.sf.jabref.help;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Util;

/**
 * 
 * This is a non-modal help Dialog. The contents of the help is specified by
 * calling showPage().
 * 
 * @version $Revision$ ($Date$)
 * 
 */
public class HelpDialog extends JDialog implements HyperlinkListener {

	private JabRefFrame frame;

	private HelpContent content;

	private BackAction back = new BackAction();

	private ForwardAction forward = new ForwardAction();

	private ContentsAction contents = new ContentsAction();

	// Initializes, but does not show the help dialog.
	public HelpDialog(JabRefFrame bf) {
		super(bf, Globals.lang("JabRef help"), false);
		frame = bf;
		content = new HelpContent(bf.prefs());
		content.addHyperlinkListener(this);
		setSize(GUIGlobals.helpSize);

		JToolBar tlb = new JToolBar();
		tlb.add(back);
		tlb.add(forward);
		tlb.addSeparator();
		tlb.add(contents);
		tlb.setFloatable(false);

		// Make ESC close dialog, and set shortkeys for back and forward.
		InputMap im = tlb.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = tlb.getActionMap();
		im.put(bf.prefs().getKey("Close dialog"), "close");
		am.put("close", new CloseAction());
		im.put(bf.prefs().getKey("Back, help dialog"), "left");
		am.put("left", back);
		im.put(bf.prefs().getKey("Forward, help dialog"), "right");
		am.put("right", forward);

		// Set shortkeys for back and forward specifically for the EditorPane.
		im = content.getInputMap(JComponent.WHEN_FOCUSED);
		am = content.getActionMap();
		im.put(bf.prefs().getKey("Back, help dialog"), "left");
		am.put("left", back);
		im.put(bf.prefs().getKey("Forward, help dialog"), "right");
		am.put("right", forward);

		getContentPane().add(tlb, BorderLayout.NORTH);
		getContentPane().add(content.getPane());
		forward.setEnabled(false);
		back.setEnabled(false);
	}

    public void showPage(String url) {
        showPage(url, JabRef.class);
    }

	public void showPage(String url, Class resourceOwner) {
		if (!isVisible()) {
			Util.placeDialog(this, frame);
			content.reset();
			back.setEnabled(false);
			setVisible(true);
		} else {
			back.setEnabled(true);
		}
		forward.setEnabled(false);
		content.setPage(url, resourceOwner);
		content.requestFocus();
	}

	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			content.setPage(e.getURL());
			back.setEnabled(true);
			forward.setEnabled(false);
		}
	}

	class CloseAction extends AbstractAction {
		public CloseAction() {
			super(Globals.lang("Close"));
			// , new ImageIcon(GUIGlobals.closeIconFile));
			putValue(SHORT_DESCRIPTION, Globals.lang("Close the help window"));
		}

		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	}

	class BackAction extends AbstractAction {
		public BackAction() {
			super("Back", GUIGlobals.getImage("left"));
			// putValue(SHORT_DESCRIPTION, "Show the previous page");
		}

		public void actionPerformed(ActionEvent e) {
			setEnabled(content.back());
			forward.setEnabled(true);
		}
	}

	class ForwardAction extends AbstractAction {
		public ForwardAction() {
			super("Forward", GUIGlobals.getImage("right"));
		}

		public void actionPerformed(ActionEvent e) {
			setEnabled(content.forward());
			back.setEnabled(true);
		}
	}

	class ContentsAction extends AbstractAction {
		public ContentsAction() {
			super("Contents", GUIGlobals.getImage("helpContents"));
		}

		public void actionPerformed(ActionEvent e) {
			content.setPage(GUIGlobals.helpContents, JabRef.class);
			back.setEnabled(true);
		}
	}
}
