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

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JTextField;

import net.sf.jabref.gui.FileDialog;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.FileExtensions;

/**
 * Action used to produce a "Browse" button for one of the text fields.
 * TODO: Replace by FileDialog usage and remove afterwards
 */
public final class BrowseAction extends AbstractAction {

    private final JFrame frame;
    private final JTextField comp;
    private final boolean dirsOnly;
    private final Set<FileExtensions> extensions;


    public static BrowseAction buildForDir(JFrame frame, JTextField tc) {
        return new BrowseAction(frame, tc, true, Collections.emptySet());
    }

    public static BrowseAction buildForDir(JTextField tc) {
        return new BrowseAction(null, tc, true, Collections.emptySet());
    }

    private BrowseAction(JFrame frame, JTextField tc, boolean dirsOnly, Set<FileExtensions> extensions) {
        super(Localization.lang("Browse"));
        this.frame = frame;
        this.dirsOnly = dirsOnly;
        this.comp = tc;
        this.extensions = extensions;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String chosen = askUser();

        if (chosen != null) {
            File newFile = new File(chosen);
            comp.setText(newFile.getPath());

        }
    }

    private String askUser() {
        if (dirsOnly) {
            Path path  = new FileDialog(frame, comp.getText()).withExtensions(extensions)
                    .showDialogAndGetSelectedDirectory().orElse(Paths.get(""));
            String file = path.toString();

            return file;
        } else {
            Path path = new FileDialog(frame, comp.getText()).withExtensions(extensions)
                    .showDialogAndGetSelectedFile().orElse(Paths.get(""));
            String file = path.toString();

            return file;
        }
    }
}
