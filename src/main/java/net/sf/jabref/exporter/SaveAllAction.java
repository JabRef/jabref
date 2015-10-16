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
package net.sf.jabref.exporter;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import net.sf.jabref.*;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.worker.Worker;
import net.sf.jabref.logic.l10n.Localization;
import spin.Spin;

/**
 *
 * @author alver
 */
public class SaveAllAction extends MnemonicAwareAction implements Worker {

    private final JabRefFrame frame;
    private int databases;
    private int saved;


    /** Creates a new instance of SaveAllAction */
    public SaveAllAction(JabRefFrame frame) {
        super(IconTheme.JabRefIcon.SAVE_ALL.getIcon());
        this.frame = frame;
        putValue(Action.ACCELERATOR_KEY, Globals.prefs.getKey("Save all"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Save all open databases"));
        putValue(Action.NAME, Localization.menuTitle("Save all"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        databases = frame.getTabbedPane().getTabCount();
        saved = 0;
        frame.output(Localization.lang("Saving all databases..."));
        Spin.off(this);
        run();
        frame.output(Localization.lang("Save all finished."));
    }

    @Override
    public void run() {
        for (int i = 0; i < databases; i++) {
            if (i < frame.getTabbedPane().getTabCount()) {
                //System.out.println("Base "+i);
                BasePanel panel = frame.baseAt(i);
                if (panel.getFile() == null) {
                    frame.showBaseAt(i);
                }
                panel.runCommand("save");
                // TODO: can we find out whether the save was actually done or not?
                saved++;
            }
        }
    }

}
