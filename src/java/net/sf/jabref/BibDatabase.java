/*
Copyright (C) 2003 Nizar N. Batada nbatada@stanford.edu
Morten O. Alver alver@boblefisk.org

All programs in this directory and subdirectories are published under
the GNU General Public License as described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

package net.sf.jabref;

import java.beans.*;
import java.util.HashMap;
import java.util.Set;

public class BibDatabase {

    private HashMap _entries = new HashMap();
    PropertyChangeSupport _changeSupport = new PropertyChangeSupport(this);

    /**
     * Insert an entry.
     */
    public void addEntry(String id, BibEntry entry)
	throws KeyCollisionException {
	// Inserts entry into _entries, but only if there is no entry
	// there already with the same id.
	if (_entries.containsKey(id))
	    throw new KeyCollisionException("ID already used in database");
	_entries.put(id, entry);
    }

    /**
     * Get an entry by its ID. Returns null if id doesn't exist.
     */
    public BibEntry getEntry(String id) {
	return (BibEntry)_entries.get(id);
    }

    /**
     * Remove an entry by its ID.
     */
    public void removeEntry(String id) {
	_entries.remove(id);
    }

    /**
     * Remove an entry.
     */
    /*    public void removeEntry(BibEntry entry) {

    }*/

    /**
     * To get keys for all entries.
     */
    public Set getKeySet() {
	return _entries.keySet();
    }

    /**
     * Add a property listener, that gets notification any time a string
     * or and entry is added or removed, or the preamble is changed.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        _changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove a property listener.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
	_changeSupport.removePropertyChangeListener(listener);
    }


    private void firePropertyChangedEvent(String fieldName, Object oldValue,
					  Object newValue) {
        _changeSupport.firePropertyChange(new PropertyChangeEvent
					  (this, fieldName, oldValue, 
					   newValue));
    }

}
