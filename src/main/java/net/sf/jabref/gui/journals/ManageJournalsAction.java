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
package net.sf.jabref.gui.journals;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import javafx.application.Platform;

import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.logic.l10n.Localization;

public class ManageJournalsAction extends MnemonicAwareAction {

    public ManageJournalsAction() {
        super();
        putValue(Action.NAME, Localization.menuTitle("Manage journal abbreviations"));
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Platform.runLater(() -> new ManageJournalAbbreviationsView().showAndWait());
    }
}
