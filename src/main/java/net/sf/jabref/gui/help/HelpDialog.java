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
package net.sf.jabref.gui.help;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.JabRef;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;

/**
 * 
 * This is a non-modal help Dialog. The contents of the help is specified by
 * calling showPage().
 */
public class HelpDialog extends JDialog implements HyperlinkListener {

    private final JabRefFrame frame;

    private final HelpContent content;

    private final BackAction back = new BackAction();

    private final ForwardAction forward = new ForwardAction();


    // Initializes, but does not show the help dialog.
    public HelpDialog(JabRefFrame bf) {
        super(bf, Localization.lang("JabRef help"), false);
        frame = bf;
        content = new HelpContent(bf.prefs());
        content.addHyperlinkListener(this);
        setSize(GUIGlobals.helpSize);

        JToolBar tlb = new JToolBar();
        tlb.add(back);
        tlb.add(forward);
        tlb.addSeparator();
        ContentsAction contents = new ContentsAction();
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

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            content.setPage(e.getURL());
            back.setEnabled(true);
            forward.setEnabled(false);
        }
    }


    class CloseAction extends AbstractAction {

        public CloseAction() {
            super(Localization.lang("Close"));
            // , new ImageIcon(GUIGlobals.closeIconFile));
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Close the help window"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    }

    class BackAction extends AbstractAction {

        public BackAction() {
            super("Back", IconTheme.JabRefIcon.LEFT.getIcon());
            // putValue(SHORT_DESCRIPTION, "Show the previous page");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setEnabled(content.back());
            forward.setEnabled(true);
        }
    }

    class ForwardAction extends AbstractAction {

        public ForwardAction() {
            super("Forward", IconTheme.JabRefIcon.RIGHT.getIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setEnabled(content.forward());
            back.setEnabled(true);
        }
    }

    class ContentsAction extends AbstractAction {

        public ContentsAction() {
            super("Contents", IconTheme.JabRefIcon.HELP_CONTENTS.getIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            content.setPage(GUIGlobals.helpContents, JabRef.class);
            back.setEnabled(true);
        }
    }
}
