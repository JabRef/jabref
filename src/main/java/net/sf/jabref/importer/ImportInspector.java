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

import net.sf.jabref.model.entry.BibEntry;

/**
 * An ImportInspector can be passed to a EntryFetcher and will receive entries
 * as they are fetched from somewhere.
 * 
 * Currently there are two implementations: ImportInspectionDialog and
 * ImportInspectionCommandLine
 * 
 */
public interface ImportInspector {

    /**
     * Notify the ImportInspector about the progress of the operation.
     * 
     * The Inspector for instance could display a progress bar with the given
     * values.
     * 
     * @param current
     *            A number that is related to the work already done.
     * 
     * @param max
     *            A current estimate for the total amount of work to be done.
     */
    void setProgress(int current, int max);

    /**
     * Add the given entry to the list of entries managed by the inspector.
     * 
     * @param entry
     *            The entry to add.
     */
    void addEntry(BibEntry entry);
}
