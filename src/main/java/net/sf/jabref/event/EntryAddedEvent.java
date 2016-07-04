/*  Copyright (C) 2016 JabRef contributors.
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
package net.sf.jabref.event;

import net.sf.jabref.event.scope.EntryEventScope;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

/**
 * {@link EntryAddedEvent} is fired when a new {@link BibEntry} was added to the {@link BibDatabase}.
 */
public class EntryAddedEvent extends EntryEvent {

    /**
     * flag if the addition is the undo of a deletion/cut
     */
    private final boolean isUndo;

    /**
     * @param bibEntry the entry which has been added
     */
    public EntryAddedEvent(BibEntry bibEntry) {
        super(bibEntry);
        this.isUndo = false;
    }

    /**
     * @param bibEntry the entry which has been added
     * @param isUndo   flag if the addition is the undo of a deletion/cut
     */
    public EntryAddedEvent(BibEntry bibEntry, boolean isUndo) {
        super(bibEntry);
        this.isUndo = isUndo;
    }

    /**
     * @param bibEntry <code>BibEntry</code> object which has been added.
     * @param location Location affected by this event
     */
    public EntryAddedEvent(BibEntry bibEntry, EntryEventScope location) {
        super(bibEntry, location);
        this.isUndo = false;
    }

    public boolean isUndo() {
        return isUndo;
    }

}
