/*
Copyright (C) 2003 David Weitzman, Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

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

Note:
Modified for use in JabRef

*/

package net.sf.jabref;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;

import java.util.*;

public class BibtexDatabase
{
    Map _entries = new HashMap();
    String _preamble = null;
    Vector _strings = new Vector();
    Hashtable _autoCompleters = null;
    
    /* Entries are stored in a HashMap with the ID as key.
     * What happens if someone changes a BibtexEntry's ID
     * after it has been added to this BibtexDatabase?
     * The key of that entry would be the old ID, not the new one.
     * Use a PropertyChangeListener to identify an ID change
     * and update the Map.
     */
    private final VetoableChangeListener listener =
        new VetoableChangeListener()
        {
            public void vetoableChange(PropertyChangeEvent pce)
                throws PropertyVetoException
            {
                if ("id".equals(pce.getPropertyName()))
                {
                    // locate the entry under its old key
                    Object oldEntry =
                        _entries.remove((String) pce.getOldValue());

                    if (oldEntry != pce.getSource())
                    {
                        // Something is very wrong!
                        // The entry under the old key isn't
                        // the one that sent this event.
                        // Restore the old state.
                        _entries.put(pce.getOldValue(), oldEntry);
                        throw new PropertyVetoException("Wrong old ID", pce);
                    }

                    if (_entries.get(pce.getNewValue()) != null)
                    {
                        _entries.put(pce.getOldValue(), oldEntry);
                        throw new PropertyVetoException
			    ("New ID already in use, please choose another",
                            pce);
                    }

                    // and re-file this entry
                    _entries.put((String) pce.getNewValue(),
                        (BibtexEntry) pce.getSource());
                }
            }
        };

    /**
     * Returns the number of entries.
     */
    public synchronized int getEntryCount()
    {
        return _entries.size();
    }

    /**
     * Returns a Set containing the keys to all entries.
     * Use getKeySet().iterator() to iterate over all entries.
     */
    public synchronized Set getKeySet()
    {
        return _entries.keySet();
    }

    /**
     * Returns an EntrySorter with the sorted entries from this base,
     * sorted by the given Comparator.
     */
    public synchronized EntrySorter getSorter(java.util.Comparator comp) {
	return new EntrySorter(_entries, comp);
    }

    /**
     * Returns the entry with the given ID.
     */
    public synchronized BibtexEntry getEntryById(String id)
    {
        return (BibtexEntry) _entries.get(id);
    }

    /**
     * Inserts the entry, given that its ID is not already in use.
     * use Util.createId(...) to make up a unique ID for an entry.
     */
    public synchronized void insertEntry(BibtexEntry entry)
        throws KeyCollisionException
    {
        String id = entry.getId();

        if (getEntryById(id) != null)
        {
            throw new KeyCollisionException(
                "ID is already in use, please choose another");
        }

        entry.addPropertyChangeListener(listener);

	// Possibly add a FieldChangeListener, which is there to add
	// new words to the autocompleter's dictionary. In case the
	// entry is non-empty (pasted), update completers.
	/*if (_autoCompleters != null) {
	    entry.addPropertyChangeListener(new FieldChangeListener
					    (_autoCompleters, entry));
	    Util.updateCompletersForEntry(_autoCompleters,
					  entry);
	}
	*/
        _entries.put(id, entry);

    }

    /**
     * Removes the entry with the given string.
     */
    public synchronized BibtexEntry removeEntry(String id)
    {
        BibtexEntry oldValue = (BibtexEntry) _entries.remove(id);

        if (oldValue != null)
        {
            oldValue.removePropertyChangeListener(listener);
        }

        return oldValue;
    }

    /**
     * Sets the database's preamble.
     */
    public synchronized void setPreamble(String preamble)
    {
	_preamble = preamble;
    }

    /**
     * Returns the database's preamble.
     */
    public synchronized String getPreamble() 
    {
	return _preamble;
    }

    /**
     * Inserts a Bibtex String at the given index.
     */
    public synchronized void addString(BibtexString string, int index) 
        throws KeyCollisionException
    {
	for (java.util.Iterator i=_strings.iterator(); i.hasNext();) {
	    if (((BibtexString)i.next()).getName().equals(string.getName()))
		throw new KeyCollisionException("A string with this label already exists,");					    
	}
	_strings.insertElementAt(string, index);
    }

    /**
     * Removes the string at the given index.
     */
    public synchronized void removeString(int index) {
	_strings.removeElementAt(index);
    }

    /**
     * Returns the string at the given index.
     */
    public synchronized BibtexString getString(int index) {
	return (BibtexString)(_strings.elementAt(index));
    }

    /**
     * Returns the number of strings.
     */
    public synchronized int getStringCount() {
	return _strings.size();
    }

    /**
     * Returns true if a string with the given label already exists.
     */
    public synchronized boolean hasStringLabel(String label) {
	for (java.util.Iterator i=_strings.iterator(); i.hasNext();) {
	    if (((BibtexString)i.next()).getName().equals(label))
		return true;
	}
	return false;
    }

    /*
    public void setCompleters(Hashtable autoCompleters) {
	_autoCompleters = autoCompleters;
       
	for (Iterator i=getKeySet().iterator(); i.hasNext();) {
	    BibtexEntry be = getEntryById((String)(i.next()));
	    be.addPropertyChangeListener(new FieldChangeListener
					 (autoCompleters, be));

	    Util.updateCompletersForEntry(autoCompleters, be);
	}
	}*/

}
