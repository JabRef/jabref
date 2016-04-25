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
package net.sf.jabref.external.push;

import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;

import net.sf.jabref.MetaData;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Class that defines interaction with an external application in the form of "pushing" selected entries to it.
 */
public interface PushToApplication {

    String getName();

    String getApplicationName();

    String getTooltip();

    Icon getIcon();

    /**
     * This method asks the implementing class to return a JPanel populated with the imlementation's options panel, if
     * necessary. If the JPanel is shown to the user, and the user indicates that settings should be stored, the
     * implementation's storeSettings() method will be called. This method must make sure all widgets in the panel are
     * in the correct selection states.
     *
     * @return a JPanel containing options, or null if options are not needed.
     */
    JPanel getSettingsPanel();

    /**
     * This method is called to indicate that the settings panel returned from the getSettingsPanel() method has been
     * shown to the user and that the user has indicated that the settings should be stored. This method must store the
     * state of the widgets in the settings panel to Globals.prefs.
     */
    void storeSettings();

    /**
     * The actual operation. This method will not be called on the event dispatch thread, so it should not do GUI
     * operations without utilizing invokeLater().
     *
     * @param database
     * @param entries
     * @param metaData
     */
    void pushEntries(BibDatabase database, List<BibEntry> entries, String keyString, MetaData metaData);

    /**
     * Reporting etc., this method is called on the event dispatch thread after pushEntries() returns.
     */
    void operationCompleted(BasePanel panel);

    /**
     * Check whether this operation requires BibTeX keys to be set for the entries. If true is returned an error message
     * will be displayed if keys are missing.
     *
     * @return true if BibTeX keys are required for this operation.
     */
    boolean requiresBibtexKeys();

}
