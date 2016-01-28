/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui.actions;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;


/**
 * Action used to produce a "Browse" button for one of the text fields.
 */
public final class BrowseAction extends AbstractAction {
    private final JFrame frame;
    private final JTextField comp;
    private final boolean dir;
    private final JComponent focusTarget;

    public static BrowseAction buildForDir(JFrame frame, JTextField tc) {
        return new BrowseAction(frame, tc, true, null);
    }

    public static BrowseAction buildForDir(JTextField tc) {
        return new BrowseAction(null, tc, true, null);
    }

    public static BrowseAction buildForFile(JTextField tc) {
        return new BrowseAction(null, tc, false, null);
    }

    public static BrowseAction buildForFile(JTextField tc, JComponent focusTarget) {
        return new BrowseAction(null, tc, false, focusTarget);
}

    public static BrowseAction buildForDir(JTextField tc, JComponent focusTarget) {
        return new BrowseAction(null, tc, true, focusTarget);
    }

    private BrowseAction(JFrame frame, JTextField tc, boolean dir, JComponent focusTarget) {
        super(Localization.lang("Browse"));
        this.frame = frame;
        this.dir = dir;
        this.comp = tc;
        this.focusTarget = focusTarget;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String chosen = askUser();

        if (chosen != null) {
            File newFile = new File(chosen);
            comp.setText(newFile.getPath());
            if (focusTarget != null) {
                new FocusRequester(focusTarget);
            }
        }
    }

    private String askUser() {
        if (dir) {
            return FileDialogs.getNewDir(frame, new File(comp.getText()), Globals.NONE,
                    JFileChooser.OPEN_DIALOG, false);
        } else {
            return FileDialogs.getNewFile(frame, new File(comp.getText()), Globals.NONE,
                    JFileChooser.OPEN_DIALOG, false);
        }
    }

}
