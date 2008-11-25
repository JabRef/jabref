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
Modified for use in JabRef.

*/

package net.sf.jabref;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

import net.sf.jabref.export.FieldFormatter;


public class BibtexEntry
{
    public final static String ID_FIELD = "id";
    private String _id;
    private BibtexEntryType _type;
    private Map<String, String> _fields = new HashMap<String, String>();
    VetoableChangeSupport _changeSupport = new VetoableChangeSupport(this);

    // Search and grouping status is stored in boolean fields for quick reference:
    private boolean searchHit, groupHit;

    public BibtexEntry(){
    	this(Util.createNeutralId());
    }
    
    public BibtexEntry(String id)
    {
        this(id, BibtexEntryType.OTHER);
    }

    public BibtexEntry(String id, BibtexEntryType type)
    {
        if (id == null)
        {
            throw new NullPointerException("Every BibtexEntry must have an ID");
        }

        _id = id;
        setType(type);
    }

    /**
     * Returns an array describing the optional fields for this entry.
     */
    public String[] getOptionalFields()
    {
        return _type.getOptionalFields();
    }

    /**
     * Returns an array describing the required fields for this entry.
     */
    public String[] getRequiredFields()
    {
        return _type.getRequiredFields();
    }

    /**
     * Returns an array describing general fields.
     */
    public String[] getGeneralFields() {
        return _type.getGeneralFields();
    }

    /**
     * Returns an set containing the names of all fields that are
     * set for this particular entry.
     */
    public Set<String> getAllFields() {
        return new TreeSet<String>(_fields.keySet());
    }

    /**
     * Returns a string describing the required fields for this entry.
     */
    public String describeRequiredFields()
    {
        return _type.describeRequiredFields();
    }

    /**
     * Returns true if this entry contains the fields it needs to be
     * complete.
     */
    public boolean hasAllRequiredFields(BibtexDatabase database)
    {
        return _type.hasAllRequiredFields(this, database);
    }

    /**
     * Returns this entry's type.
     */
    public BibtexEntryType getType()
    {
        return _type;
    }

    /**
     * Sets this entry's type.
     */
    public void setType(BibtexEntryType type)
    {
        if (type == null)
        {
            throw new NullPointerException(
                "Every BibtexEntry must have a type.  Instead of null, use type OTHER");
        }

        BibtexEntryType oldType = _type;

        try {
            // We set the type before throwing the changeEvent, to enable
            // the change listener to access the new value if the change
            // sets off a change in database sorting etc.
            _type = type;
            firePropertyChangedEvent(GUIGlobals.TYPE_HEADER,
                    oldType != null ? oldType.getName() : null,
                    type.getName());
        } catch (PropertyVetoException pve) {
            pve.printStackTrace();
        }


    }

    /**
     * Prompts the entry to call BibtexEntryType.getType(String) with
     * its current type name as argument, and sets its type according
     * to what is returned. This method is called when a user changes
     * the type customization, to make sure all entries are set with
     * current types.
     * @return true if the entry could find a type, false if not (in
     * this case the type will have been set to
     * BibtexEntryType.TYPELESS).
     */
    public boolean updateType() {
        BibtexEntryType newType = BibtexEntryType.getType(_type.getName());
        if (newType != null) {
            _type = newType;
            return true;
        }
        _type = BibtexEntryType.TYPELESS;
        return false;
    }

    /**
     * Sets this entry's ID, provided the database containing it
     * doesn't veto the change.
     */
    public void setId(String id) throws KeyCollisionException {

        if (id == null) {
            throw new
                NullPointerException("Every BibtexEntry must have an ID");
        }

        try
        {
            firePropertyChangedEvent(ID_FIELD, _id, id);
        }
        catch (PropertyVetoException pv)
        {
            throw new KeyCollisionException("Couldn't change ID: " + pv);
        }

        _id = id;
    }

    /**
     * Returns this entry's ID.
     */
    public String getId()
    {
        return _id;
    }

    /**
     * Returns the contents of the given field, or null if it is not set.
     */
    public String getField(String name) {
        return _fields.get(name);
    }

    public String getCiteKey() {
        return (_fields.containsKey(BibtexFields.KEY_FIELD) ?
                _fields.get(BibtexFields.KEY_FIELD) : null);
    }

    /**
     * Sets a number of fields simultaneously. The given HashMap contains field
     * names as keys, each mapped to the value to set.
     * WARNING: this method does not notify change listeners, so it should *NOT*
     * be used for entries that are being displayed in the GUI. Furthermore, it
     * does not check values for content, so e.g. empty strings will be set as such.
     */
    public void setField(Map<String, String> fields){
        _fields.putAll(fields);
    }

    /**
     * Set a field, and notify listeners about the change.
     *
     * @param name The field to set.
     * @param value The value to set.
     */
    public void setField(String name, String value) {

        if (ID_FIELD.equals(name)) {
            throw new IllegalArgumentException("The field name '" + name +
                                               "' is reserved");
        }

        String oldValue = _fields.get(name);
        try {
            // We set the field before throwing the changeEvent, to enable
            // the change listener to access the new value if the change
            // sets off a change in database sorting etc.
            _fields.put(name, value);
            firePropertyChangedEvent(name, oldValue, value);
        } catch (PropertyVetoException pve) {
            // Since we have already made the change, we must undo it since
            // the change was rejected:
            _fields.put(name, oldValue);
            throw new IllegalArgumentException("Change rejected: " + pve);
        }

    }

    /**
     * Remove the mapping for the field name, and notify listeners about
     * the change.
     *
     * @param name The field to clear.
     */
    public void clearField(String name) {

      if (ID_FIELD.equals(name)) {
           throw new IllegalArgumentException("The field name '" + name +
                                              "' is reserved");
       }
       Object oldValue = _fields.get(name);
       _fields.remove(name);
       try {
           firePropertyChangedEvent(name, oldValue, null);
       } catch (PropertyVetoException pve) {
           throw new IllegalArgumentException("Change rejected: " + pve);
       }


    }

    /**
     * Determines whether this entry has all the given fields present. If a non-null
     * database argument is given, this method will try to look up missing fields in
     * entries linked by the "crossref" field, if any.
     *
     * @param fields An array of field names to be checked.
     * @param database The database in which to look up crossref'd entries, if any. This
     *  argument can be null, meaning that no attempt will be made to follow crossrefs.
     * @return true if all fields are set or could be resolved, false otherwise.
     */
    protected boolean allFieldsPresent(String[] fields, BibtexDatabase database) {
        for (int i = 0; i < fields.length; i++) {
            if (BibtexDatabase.getResolvedField(fields[i], this, database) == null) {
                return false;
            }
        }

        return true;
    }

    private void firePropertyChangedEvent(String fieldName, Object oldValue,
        Object newValue) throws PropertyVetoException
    {
        _changeSupport.fireVetoableChange(new PropertyChangeEvent(this,
                fieldName, oldValue, newValue));
    }

    /**
     * Adds a VetoableChangeListener, which is notified of field
     * changes. This is useful for an object that needs to update
     * itself each time a field changes.
     */
    public void addPropertyChangeListener(VetoableChangeListener listener)
    {
        _changeSupport.addVetoableChangeListener(listener);
    }

    /**
     * Removes a property listener.
     */
    public void removePropertyChangeListener(VetoableChangeListener listener)
    {
        _changeSupport.removeVetoableChangeListener(listener);
    }

    /**
     * Write this entry to the given Writer, with the given FieldFormatter.
     * @param write True if this is a write, false if it is a display. The write will
     * not include non-writeable fields if it is a write, otherwise non-displayable fields
     * will be ignored. Refer to GUIGlobals for isWriteableField(String) and
     * isDisplayableField(String).
     */
    public void write(Writer out, FieldFormatter ff, boolean write) throws IOException {
        // Write header with type and bibtex-key.
        out.write("@"+_type.getName().toUpperCase(Locale.US)+"{");

        String str = Util.shaveString(getField(BibtexFields.KEY_FIELD));
        out.write(((str == null) ? "" : str)+","+Globals.NEWLINE);
        HashMap<String, String> written = new HashMap<String, String>();
        written.put(BibtexFields.KEY_FIELD, null);
        boolean hasWritten = false;
        // Write required fields first.
        String[] s = getRequiredFields();
        if (s != null) for (int i=0; i<s.length; i++) {
            hasWritten = hasWritten | writeField(s[i], out, ff, hasWritten);
            written.put(s[i], null);
        }
        // Then optional fields.
        s = getOptionalFields();
        if (s != null) for (int i=0; i<s.length; i++) {
            if (!written.containsKey(s[i])) { // If field appears both in req. and opt. don't repeat.
                //writeField(s[i], out, ff);
                hasWritten = hasWritten | writeField(s[i], out, ff, hasWritten);
                written.put(s[i], null);
            }
        }
        // Then write remaining fields in alphabetic order.
        TreeSet<String> remainingFields = new TreeSet<String>();
        for (String key : _fields.keySet()){
            boolean writeIt = (write ? BibtexFields.isWriteableField(key) :
                               BibtexFields.isDisplayableField(key));
            if (!written.containsKey(key) && writeIt)
                       remainingFields.add(key);
        }
        for (String field: remainingFields)
            hasWritten = hasWritten | writeField(field, out, ff, hasWritten);

        // Finally, end the entry.
        out.write((hasWritten ? Globals.NEWLINE : "")+"}"+Globals.NEWLINE);
    }

    /**
     * Write a single field, if it has any content.
     * @param name The field name
     * @param out The Writer to send it to
     * @param ff A formatter to filter field contents before writing
     * @param isFirst Indicates whether this is the first field written for
     *    this entry - if not, start by writing a comma and newline
     * @return true if this field was written, false if it was skipped because
     *    it was not set
     * @throws IOException In case of an IO error
     */
    private boolean writeField(String name, Writer out,
                            FieldFormatter ff, boolean isFirst) throws IOException {
        String o = getField(name);
        if (o != null) {
            if (isFirst)
                out.write(","+Globals.NEWLINE);
            out.write("  "+name+" = ");

            try {
                out.write(ff.format(o.toString(), name));
            } catch (Throwable ex) {
                throw new IOException
                    (Globals.lang("Error in field")+" '"+name+"': "+ex.getMessage());
            }
            return true;
        } else
            return false;
    }

    /**
     * Returns a clone of this entry. Useful for copying.
     */
    public Object clone() {
        BibtexEntry clone = new BibtexEntry(_id, _type);
        clone._fields = new HashMap<String, String>(_fields); 
        return clone;
    }

    public String toString() {
        return getType().getName()+":"+getField(BibtexFields.KEY_FIELD);
    }

    public boolean isSearchHit() {
        return searchHit;
    }

    public void setSearchHit(boolean searchHit) {
        this.searchHit = searchHit;
    }

    public boolean isGroupHit() {
        return groupHit;
    }

    public void setGroupHit(boolean groupHit) {
        this.groupHit = groupHit;
    }

    /**
     * @param maxCharacters The maximum number of characters (additional
     * characters are replaced with "..."). Set to 0 to disable truncation.
     * @return A short textual description of the entry in the format:
     * Author1, Author2: Title (Year)
     */
    public String getAuthorTitleYear(int maxCharacters) {
        String[] s = new String[] {
                getField("author"),
                getField("title"),
                getField("year")};
        for (int i = 0; i < s.length; ++i)
            if (s[i] == null)
                s[i] = "N/A";
        String text = s[0] + ": \"" + s[1] + "\" (" + s[2] + ")";
        if (maxCharacters <= 0 || text.length() <= maxCharacters)
            return text;
        return text.substring(0, maxCharacters + 1) + "...";
    }
    
}
