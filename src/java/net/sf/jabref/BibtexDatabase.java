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


// created by : ?
//
// modified : r.nagel 23.08.2004
//                - insert getEntryByKey() methode needed by AuxSubGenerator

package net.sf.jabref;

import java.beans.*;
import java.util.*;

import javax.swing.JOptionPane;

import net.sf.jabref.groups.GroupSelector;

public class BibtexDatabase
{
    Map _entries = new Hashtable();
    String _preamble = null;
    HashMap _strings = new HashMap();
    Vector _strings_ = new Vector();
    Hashtable _autoCompleters = null;
    Set changeListeners = new HashSet();
    private BibtexDatabase ths = this;

    private HashMap allKeys  = new HashMap();	// use a map instead of a set since i need to know how many of each key is inthere

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
		if (pce.getPropertyName() == null)
		    fireDatabaseChanged (new DatabaseChangeEvent(ths, DatabaseChangeEvent.CHANGING_ENTRY, (BibtexEntry)pce.getSource()));
                else if ("id".equals(pce.getPropertyName()))
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
                } else {
		    fireDatabaseChanged (new DatabaseChangeEvent(ths, DatabaseChangeEvent.CHANGED_ENTRY, (BibtexEntry)pce.getSource()));
		    //Util.pr(pce.getSource().toString()+"\n"+pce.getPropertyName()
		    //    +"\n"+pce.getNewValue());
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
	EntrySorter sorter = new EntrySorter(_entries, comp);
	addDatabaseChangeListener(sorter);
        return sorter;
    }

    /**
     * Just temporary, for testing purposes....
     * @return
     */
    public Map getEntryMap() { return _entries; }

    /**
     * Returns the entry with the given ID (-> entry_type + hashcode).
     */
    public synchronized BibtexEntry getEntryById(String id)
    {
        return (BibtexEntry) _entries.get(id);
    }
    
    public synchronized Collection getEntries() {
    	return _entries.values();
    }

    /**
     * Returns the entry with the given bibtex key.
     */
    public synchronized BibtexEntry getEntryByKey(String key)
    {
      BibtexEntry back = null ;

      int keyHash = key.hashCode() ; // key hash for better performance

      Set keySet = _entries.keySet();
      if (keySet != null)
      {
          Iterator it = keySet.iterator();
          boolean loop = it.hasNext() ;
          while(loop)
          {
            String entrieID = (String) it.next() ;
            BibtexEntry entry = getEntryById(entrieID) ;
            if ((entry != null) && (entry.getCiteKey() != null))
            {
              String citeKey = entry.getCiteKey() ;
              if (citeKey != null)
              {
                if (keyHash == citeKey.hashCode() )
                {
                  loop = false ;
                  back = entry ;
                }
                else loop = it.hasNext() ;
              } else loop = it.hasNext() ;
            }
          }
      }
      return back ;
    }
    
    public synchronized BibtexEntry[] getEntriesByKey(String key) {
        Vector entries = new Vector();
        BibtexEntry entry;
        for (Iterator it = _entries.entrySet().iterator(); it.hasNext(); ) {
            entry = (BibtexEntry)((Map.Entry)it.next()).getValue();
            if (key.equals(entry.getCiteKey()))
                entries.add(entry);
        }
        BibtexEntry[] entryArray = new BibtexEntry[entries.size()];
        return (BibtexEntry[]) entries.toArray(entryArray);
    }    

    /**
     * Inserts the entry, given that its ID is not already in use.
     * use Util.createId(...) to make up a unique ID for an entry.
     */
    public synchronized boolean insertEntry(BibtexEntry entry)
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

	fireDatabaseChanged(new DatabaseChangeEvent(this, DatabaseChangeEvent.ADDED_ENTRY, entry));

        return checkForDuplicateKeyAndAdd(null, entry.getCiteKey(), false);
    }

    /**
     * Removes the entry with the given string.
     */
    public synchronized BibtexEntry removeEntry(String id)
    {
        BibtexEntry oldValue = (BibtexEntry) _entries.remove(id);
        removeKeyFromSet(oldValue.getCiteKey());

        if (oldValue != null)
        {
            oldValue.removePropertyChangeListener(listener);
        }

	fireDatabaseChanged(new DatabaseChangeEvent(this, DatabaseChangeEvent.REMOVED_ENTRY, oldValue));

        return oldValue;
    }

    public synchronized boolean setCiteKeyForEntry(String id, String key) {
        if (!_entries.containsKey(id)) return false; // Entry doesn't exist!
        BibtexEntry entry = getEntryById(id);
        String oldKey = entry.getCiteKey();
        if (key != null)
          entry.setField(Globals.KEY_FIELD, key);
        else
          entry.clearField(Globals.KEY_FIELD);
        return checkForDuplicateKeyAndAdd(oldKey, entry.getCiteKey(), false);
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
    public synchronized void addString(BibtexString string)
        throws KeyCollisionException
    {
        for (java.util.Iterator i=_strings.keySet().iterator(); i.hasNext();) {
            if (((BibtexString)_strings.get(i.next())).getName().equals(string.getName()))
                throw new KeyCollisionException("A string with this label already exists,");
        }
	
	if (_strings.containsKey(string.getId()))
	    throw new KeyCollisionException("Duplicate BibtexString id.");
	
        _strings.put(string.getId(), string);
    }

    /**
     * Removes the string at the given index.
     */
    public synchronized void removeString(String id) {
        _strings.remove(id);
    }

    /**
     * Returns a Set of keys to all BibtexString objects in the database.
     * These are in no sorted order.
     */
    public Set getStringKeySet() {
	return _strings.keySet();
    }

    /**
     * Returns the string at the given index.
     */
    public synchronized BibtexString getString(Object o) {
        return (BibtexString)(_strings.get(o));
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
        for (java.util.Iterator i=_strings.keySet().iterator(); i.hasNext();) {
            if (((BibtexString)_strings.get(i.next())).getName().equals(label))
                return true;
        }
        return false;
    }

   /**
    * If the label represents a string contained in this database, returns
    * that string's content. Resolves references to other strings, taking
    * care not to follow a circular reference pattern.
    * If the string is undefined, returns the label itself.
    */
    public String resolveString(String label) {
	return resolveString(label, new HashSet());
    }
   
    /**
     * Resolves any references to strings contained in this database,
     * if possible.
     */
    public String resolveForStrings(String content) {
	return resolveContent(content, new HashSet());
    }

    private String resolveString(String label, HashSet usedIds) {

        for (java.util.Iterator i=_strings.keySet().iterator(); i.hasNext();) {
            BibtexString string = (BibtexString)_strings.get(i.next());

	        //Util.pr(label+" : "+string.getName());
            if (string.getName().toLowerCase().equals(label.toLowerCase())) {

		// First check if this string label has been resolved
		// earlier in this recursion. If so, we have a
		// circular reference, and have to stop to avoid
		// infinite recursion.
		if (usedIds.contains(string.getId())) {
		    Util.pr("Stopped due to circular reference in strings: "+label);
		    return label;
		}
		// If not, log this string's ID now.
		usedIds.add(string.getId());

		// Ok, we found the string. Now we must make sure we
		// resolve any references to other strings in this one.
		String res = string.getContent();
		res = resolveContent(res, usedIds);

		// Finished with recursing this branch, so we remove our
		// ID again:
		usedIds.remove(string.getId());

		return res;
	    }
        }
        
        // If we get to this point, the string has obviously not been defined locally.
        // Check if one of the standard BibTeX month strings has been used:
        Object o;
        if ((o = Globals.MONTH_STRINGS.get(label.toLowerCase())) != null) {
            return (String)o;
        }
        
        return label;
    }

    private String resolveContent(String res, HashSet usedIds) {
	//if (res.matches(".*#[-\\^\\:\\w]+#.*")) {
    if (res.matches(".*#[^#]+#.*")) {
	    StringBuffer newRes = new StringBuffer();
	    int piv = 0, next = 0;
	    while ((next=res.indexOf("#", piv)) >= 0) {
		// We found the next string ref. Append the text
		// up to it.
		if (next > 0)
		    newRes.append(res.substring(piv, next));
		int stringEnd = res.indexOf("#", next+1);
		if (stringEnd >= 0) {
		    // We found the boundaries of the string ref,
		    // now resolve that one.
		    String refLabel = res.substring(next+1, stringEnd);
		    newRes.append(resolveString(refLabel, usedIds));
		}
		piv = stringEnd+1;
	    }
	    if (piv < res.length()-1)
		newRes.append(res.substring(piv));
	    res = newRes.toString();
	}
	return res;
    }

    //##########################################
    //  usage:
    //  isDuplicate=checkForDuplicateKeyAndAdd( null, b.getKey() , issueDuplicateWarning);
    //############################################
        // if the newkey already exists and is not the same as oldkey it will give a warning
    // else it will add the newkey to the to set and remove the oldkey
    public boolean checkForDuplicateKeyAndAdd(String oldKey, String newKey, boolean issueWarning){
                // Globals.logger(" checkForDuplicateKeyAndAdd [oldKey = " + oldKey + "] [newKey = " + newKey + "]");

        boolean duplicate=false;
        if(oldKey==null){// this is a new entry so don't bother removing oldKey
            duplicate= addKeyToSet( newKey);
        }else{
            if(oldKey.equals(newKey)){// were OK because the user did not change keys
                duplicate=false;
            }else{// user changed the key

                // removed the oldkey
                // But what if more than two have the same key?
                // this means that user can add another key and would not get a warning!
                // consider this: i add a key xxx, then i add another key xxx . I get a warning. I delete the key xxx. JBM
                // removes this key from the allKey. then I add another key xxx. I don't get a warning!
                // i need a way to count the number of keys of each type
                // hashmap=>int (increment each time)

                removeKeyFromSet( oldKey);
                duplicate = addKeyToSet( newKey );
            }
        }
        if(duplicate==true && issueWarning==true){
            JOptionPane.showMessageDialog(null,  Globals.lang("Warning there is a duplicate key")+":" + newKey ,
                                          Globals.lang("Duplicate Key Warning"),
                                          JOptionPane.WARNING_MESSAGE);//, options);

        }
        return duplicate;
    }

    /**
     * Returns the number of occurences of the given key in this database.
     */
    public int getNumberOfKeyOccurences(String key) {
        Object o = allKeys.get(key);
        if (o == null)
            return 0;
        else
            return ((Integer)o).intValue();
            
    }
    
    //========================================================
    // keep track of all the keys to warn if there are duplicates
    //========================================================
    private boolean addKeyToSet(String key){
                boolean exists=false;
                if((key == null) || key.equals(""))
                        return false;//don't put empty key
                if(allKeys.containsKey(key)){
                        // warning
                        exists=true;
                        allKeys.put( key, new Integer( ((Integer)allKeys.get(key)).intValue() + 1));// incrementInteger( allKeys.get(key)));
                }else
                        allKeys.put( key, new Integer(1));
                return exists;
    }
    //========================================================
    // reduce the number of keys by 1. if this number goes to zero then remove from the set
    // note: there is a good reason why we should not use a hashset but use hashmap instead
    //========================================================
    private void removeKeyFromSet(String key){
                if((key == null) || key.equals("")) return;
                if(allKeys.containsKey(key)){
                        Integer tI = (Integer)allKeys.get(key); // if(allKeys.get(key) instanceof Integer)
                        if(tI.intValue()==1)
                                allKeys.remove( key);
                        else
                                allKeys.put( key, new Integer( ((Integer)tI).intValue() - 1));//decrementInteger( tI ));
                }
    }


    
    public void fireDatabaseChanged(DatabaseChangeEvent e) {
	for (Iterator i=changeListeners.iterator(); i.hasNext();) {
	    ((DatabaseChangeListener)i.next()).databaseChanged(e);
	}
    }

    public void addDatabaseChangeListener(DatabaseChangeListener l) {
	changeListeners.add(l);
    }

    public void removeDatabaseChangeListener(DatabaseChangeListener l) {
	changeListeners.remove(l);
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
