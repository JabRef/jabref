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

import javax.swing.AbstractAction;
import javax.swing.Action;

import javafx.application.Platform;

import net.sf.jabref.gui.errorconsole.ErrorConsoleView;
import net.sf.jabref.logic.error.StreamEavesdropper;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.logging.Cache;

/**
 * Such an error console can be
 * useful in getting complete bug reports, especially from Windows users,
 * without asking users to run JabRef in a command window to catch the error info.
 * <p/>
 * It offers a separate tab for the log output.
 */
public class ErrorConsoleAction extends AbstractAction {

    public ErrorConsoleAction(StreamEavesdropper streamEavesdropper, Cache cache) {
        super(Localization.menuTitle("View event log"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Display all error messages"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Platform.runLater(() -> new ErrorConsoleView().show());
    }

}
