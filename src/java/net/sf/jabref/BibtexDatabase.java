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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JOptionPane;

public class BibtexDatabase {
    
	Map<String, BibtexEntry> _entries = new Hashtable<String, BibtexEntry>();

	String _preamble = null;

	HashMap<String, BibtexString> _strings = new HashMap<String, BibtexString>();

	Vector<String> _strings_ = new Vector<String>();

	Set<DatabaseChangeListener> changeListeners = new HashSet<DatabaseChangeListener>();

	/**
	 * use a map instead of a set since i need to know how many of each key is
	 * inthere
	 */
	private HashMap<String, Integer> allKeys = new HashMap<String, Integer>();

    /*
	 * Entries are stored in a HashMap with the ID as key. What happens if
	 * someone changes a BibtexEntry's ID after it has been added to this
	 * BibtexDatabase? The key of that entry would be the old ID, not the new
	 * one. Use a PropertyChangeListener to identify an ID change and update the
	 * Map.
	 */
    private final VetoableChangeListener listener =
        new VetoableChangeListener()
        {
            public void vetoableChange(PropertyChangeEvent pce)
                throws PropertyVetoException
            {
                if (pce.getPropertyName() == null)
                    fireDatabaseChanged (new DatabaseChangeEvent(BibtexDatabase.this, DatabaseChangeEvent.CHANGING_ENTRY, (BibtexEntry)pce.getSource()));
                else if ("id".equals(pce.getPropertyName()))
                {
                    // locate the entry under its old key
                    BibtexEntry oldEntry =
                        _entries.remove(pce.getOldValue());

                    if (oldEntry != pce.getSource())
                    {
                        // Something is very wrong!
                        // The entry under the old key isn't
                        // the one that sent this event.
                        // Restore the old state.
                        _entries.put((String)pce.getOldValue(), oldEntry);
                        throw new PropertyVetoException("Wrong old ID", pce);
                    }

                    if (_entries.get(pce.getNewValue()) != null)
                    {
                        _entries.put((String)pce.getOldValue(), oldEntry);
                        throw new PropertyVetoException
                            ("New ID already in use, please choose another",
                            pce);
                    }

                    // and re-file this entry
                    _entries.put((String) pce.getNewValue(),
                        (BibtexEntry) pce.getSource());
                } else {
                    fireDatabaseChanged (new DatabaseChangeEvent(BibtexDatabase.this, DatabaseChangeEvent.CHANGED_ENTRY, (BibtexEntry)pce.getSource()));
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
    public synchronized Set<String> getKeySet()
    {
        return _entries.keySet();
    }

    /**
     * Returns an EntrySorter with the sorted entries from this base,
     * sorted by the given Comparator.
     */
    public synchronized EntrySorter getSorter(Comparator<BibtexEntry> comp) {
        EntrySorter sorter = new EntrySorter(_entries, comp);
        addDatabaseChangeListener(sorter);
        return sorter;
    }

    /**
     * Just temporary, for testing purposes....
     * @return
     */
    public Map<String, BibtexEntry> getEntryMap() { return _entries; }

    /**
     * Returns the entry with the given ID (-> entry_type + hashcode).
     */
    public synchronized BibtexEntry getEntryById(String id)
    {
        return _entries.get(id);
    }

    public synchronized Collection<BibtexEntry> getEntries() {
            return _entries.values();
    }

    /**
     * Returns the entry with the given bibtex key.
     */
    public synchronized BibtexEntry getEntryByKey(String key)
    {
      BibtexEntry back = null ;

      int keyHash = key.hashCode() ; // key hash for better performance

      Set<String> keySet = _entries.keySet();
      if (keySet != null)
      {
          Iterator<String> it = keySet.iterator();
          while(it.hasNext())
          {
            String entrieID = it.next() ;
            BibtexEntry entry = getEntryById(entrieID) ;
            if ((entry != null) && (entry.getCiteKey() != null))
            {
              String citeKey = entry.getCiteKey() ;
              if (citeKey != null)
              {
                if (keyHash == citeKey.hashCode() )
                {
                  back = entry ;
                }
              }
            }
          }
      }
      return back ;
    }

    public synchronized BibtexEntry[] getEntriesByKey(String key) {
        
    	ArrayList<BibtexEntry> entries = new ArrayList<BibtexEntry>();
        
        for (BibtexEntry entry : _entries.values()){
            if (key.equals(entry.getCiteKey()))
                entries.add(entry);
        }
        
        return entries.toArray(new BibtexEntry[entries.size()]);
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

        _entries.put(id, entry);

        fireDatabaseChanged(new DatabaseChangeEvent(this, DatabaseChangeEvent.ADDED_ENTRY, entry));

        return checkForDuplicateKeyAndAdd(null, entry.getCiteKey(), false);
    }

    /**
     * Removes the entry with the given string.
     * 
     * Returns null if not found.
     */
    public synchronized BibtexEntry removeEntry(String id)
    {
        BibtexEntry oldValue = _entries.remove(id);
        
        if (oldValue == null)
            return null;
        
        removeKeyFromSet(oldValue.getCiteKey());
        oldValue.removePropertyChangeListener(listener);
        fireDatabaseChanged(new DatabaseChangeEvent(this, DatabaseChangeEvent.REMOVED_ENTRY, oldValue));

        return oldValue;
    }

    public synchronized boolean setCiteKeyForEntry(String id, String key) {
        if (!_entries.containsKey(id)) return false; // Entry doesn't exist!
        BibtexEntry entry = getEntryById(id);
        String oldKey = entry.getCiteKey();
        if (key != null)
          entry.setField(BibtexFields.KEY_FIELD, key);
        else
          entry.clearField(BibtexFields.KEY_FIELD);
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
    	if (hasStringLabel(string.getName())){
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
    public Set<String> getStringKeySet() {
        return _strings.keySet();
    }
    
    /**
     * Returns a Collection of all BibtexString objects in the database.
     * These are in no particular order.
     */
    public Collection<BibtexString> getStringValues() {
        return _strings.values();
    }

    /**
     * Returns the string at the given index.
     */
    public synchronized BibtexString getString(String o) {
        return _strings.get(o);
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
    	for (BibtexString value : _strings.values()){
             if (value.getName().equals(label))
                return true;
        }
        return false;
    }

    /**
     * Resolves any references to strings contained in this field content,
     * if possible.
     */
    public String resolveForStrings(String content) {
    	if (content == null){
    		throw new IllegalArgumentException("Content for resolveForStrings must not be null.");
    	}
        return resolveContent(content, new HashSet<String>());
    }
    
    /**
	 * Take the given collection of BibtexEntry and resolve any string
	 * references.
	 * 
	 * @param entries
	 *            A collection of BibtexEntries in which all strings of the form
	 *            #xxx# will be resolved against the hash map of string
	 *            references stored in the databasee.
	 *            
	 * @param inPlace If inPlace is true then the given BibtexEntries will be modified, if false then copies of the BibtexEntries are made before resolving the strings.
	 * 
	 * @return a list of bibtexentries, with all strings resolved. It is dependent on the value of inPlace whether copies are made or the given BibtexEntries are modified. 
	 */
    public List<BibtexEntry> resolveForStrings(Collection<BibtexEntry> entries, boolean inPlace){
    	
    	if (entries == null)
    		throw new NullPointerException();
    	
    	List<BibtexEntry> results = new ArrayList<BibtexEntry>(entries.size());
    	
    	for (BibtexEntry entry : entries){
    		results.add(this.resolveForStrings(entry, inPlace));
    	}
    	return results;
    }
    
    /**
	 * Take the given BibtexEntry and resolve any string references.
	 * 
	 * @param entry
	 *            A BibtexEntry in which all strings of the form #xxx# will be
	 *            resolved against the hash map of string references stored in
	 *            the databasee.
	 * 
	 * @param inPlace
	 *            If inPlace is true then the given BibtexEntry will be
	 *            modified, if false then a copy is made using close made before
	 *            resolving the strings.
	 * 
	 * @return a BibtexEntry with all string references resolved. It is
	 *         dependent on the value of inPlace whether a copy is made or the
	 *         given BibtexEntries is modified.
	 */
    public BibtexEntry resolveForStrings(BibtexEntry entry, boolean inPlace) {
		
    	if (!inPlace){
    		entry = (BibtexEntry)entry.clone();
    	}
    	
    	for (Object field : entry.getAllFields()){
    		entry.setField(field.toString(), this.resolveForStrings(entry.getField(field.toString()).toString()));
    	}
    	
    	return entry;
	}

	/**
    * If the label represents a string contained in this database, returns
    * that string's content. Resolves references to other strings, taking
    * care not to follow a circular reference pattern.
    * If the string is undefined, returns null.
    */
    private String resolveString(String label, HashSet<String> usedIds) {
    	for (BibtexString string : _strings.values()){

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

        return null;
    }

    private String resolveContent(String res, HashSet<String> usedIds) {
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
                    String resolved = resolveString(refLabel, usedIds);
                    
                    if (resolved == null) {
                        // Could not resolve string. Display the #
                        // characters rather than removing them:
                        newRes.append(res.substring(next, stringEnd+1));
                    } else
                        // The string was resolved, so we display its meaning only,
                        // stripping the # characters signifying the string label:
                        newRes.append(resolved);
                    piv = stringEnd+1;
                } else {
                    // We didn't find the boundaries of the string ref. This
                    // makes it impossible to interpret it as a string label.
                    // So we should just append the rest of the text and finish.
                    newRes.append(res.substring(next));
                    piv = res.length();
                    break;
                }

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
                        allKeys.put( key, new Integer( allKeys.get(key).intValue() + 1));// incrementInteger( allKeys.get(key)));
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
                        Integer tI = allKeys.get(key); // if(allKeys.get(key) instanceof Integer)
                        if(tI.intValue()==1)
                                allKeys.remove( key);
                        else
                                allKeys.put( key, new Integer( (tI).intValue() - 1));//decrementInteger( tI ));
                }
    }



    public void fireDatabaseChanged(DatabaseChangeEvent e) {
    	for (DatabaseChangeListener listener : changeListeners){
    		listener.databaseChanged(e);
        }
    }

    public void addDatabaseChangeListener(DatabaseChangeListener l) {
        changeListeners.add(l);
    }

    public void removeDatabaseChangeListener(DatabaseChangeListener l) {
        changeListeners.remove(l);
    }

	/**
	 * Returns the text stored in the given field of the given bibtex entry
	 * which belongs to the given database.
	 * 
	 * If a database is given, this function will try to resolve any string
	 * references in the field-value.
     * Also, if a database is given, this function will try to find values for
     * unset fields in the entry linked by the "crossref" field, if any.
	 * 
	 * @param field
	 *            The field to return the value of.
	 * @param bibtex maybenull
	 *            The bibtex entry which contains the field.
	 * @param database maybenull
	 *            The database of the bibtex entry.
	 * @return The resolved field value or null if not found.
	 */
	public static String getResolvedField(String field, BibtexEntry bibtex,
			BibtexDatabase database) {
	
		if (field.equals("bibtextype"))
			return bibtex.getType().getName();

        Object o = bibtex.getField(field);

        // If this field is not set, and the entry has a crossref, try to look up the
        // field in the referred entry:
        if ((o == null) && (database != null)) {
            Object crossRef = bibtex.getField("crossref");
            if (crossRef != null) {
                BibtexEntry referred = database.getEntryByKey((String)crossRef);
                if (referred != null) {
                    // Ok, we found the referred entry. Get the field value from that
                    // entry. If it is unset there, too, stop looking:
                    o = referred.getField(field);
                }
            }
        }

        return getText((String)o, database);
	}

	/**
	 * Returns a text with references resolved according to an optionally given
	 * database.
	
	 * @param toResolve maybenull The text to resolve.
	 * @param database maybenull The database to use for resolving the text.
	 * @return The resolved text or the original text if either the text or the database are null
	 */
	public static String getText(String toResolve, BibtexDatabase database) {
		if (toResolve != null && database != null)
			return database.resolveForStrings(toResolve);
		
		return toResolve;
	}
}
