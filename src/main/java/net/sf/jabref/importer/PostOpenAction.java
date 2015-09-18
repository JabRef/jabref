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
package net.sf.jabref.importer;

import net.sf.jabref.gui.BasePanel;

/**
 * This interface defines potential actions that may need to be taken after
 * opening a bib file into JabRef. This can for instance be file upgrade actions
 * that should be offered due to new features in JabRef, and may depend on e.g.
 * which JabRef version the file was last written by.
 *
 * This interface is introduced in an attempt to add such functionality in a
 * flexible manner.
 */
public interface PostOpenAction {

    /**
     * This method is queried in order to find out whether the action needs to be
     * performed or not.
     * @param pr The result of the bib parse operation.
     * @return true if the action should be called, false otherwise.
     */
    boolean isActionNecessary(ParserResult pr);

    /**
     * This method is called after the new database has been added to the GUI, if
     * the isActionNecessary() method returned true.
     *
     * Note: if several such methods need to be called sequentially, it is
     *       important that all implementations of this method do not return
     *       until the operation is finished. If work needs to be off-loaded
     *       into a worker thread, use Spin to do this synchronously.
     *
     * @param panel The BasePanel where the database is shown.
     * @param pr The result of the bib parse operation.
     */
    void performAction(BasePanel panel, ParserResult pr);
}
