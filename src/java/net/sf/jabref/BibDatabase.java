package net.sf.jabref;

import java.beans.*
import java.util.HashMap;
import java.util.Set;

public class BibDatabase {

    private HashMap _entries = new HashMap();
    PropertyChangeSupport _changeSupport = new PropertyChangeSupport(this);

    /**
     * Insert an entry.
     */
    public void addEntry(String id, BibEntry entry) {
	// Inserts entry into _entries, but only if there is no entry
	// there already with the same id.
    }

    /**
     * Get an entry by its ID.
     */
    public BibEntry getEntry(String id) {
	return (BibEntry)_entries.get(id);
    }

    /**
     * Remove an entry by its ID.
     */
    public void removeEntry(String id) {

    }

    /**
     * Remove an entry.
     */
    public void removeEntry(BibEntry entry) {

    }

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
