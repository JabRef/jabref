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

import net.sf.jabref.export.FieldFormatter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;

import java.util.HashMap;
import java.util.Map;
import java.io.*;


public class BibtexEntry
{
    private String _id;
    private BibtexEntryType _type;
    private Map _fields = new HashMap();
    VetoableChangeSupport _changeSupport = new VetoableChangeSupport(this);

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
        return new String[] 
	    {"crossref", "url", "abstract", "keywords", "comment"}; // May change...
    }

    /**
     * Returns an array containing the names of all fields that are
     * set for this particular entry.
     */
    public Object[] getAllFields() {
	return _fields.keySet().toArray();
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
    public boolean hasAllRequiredFields()
    {
        return _type.hasAllRequiredFields(this);
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

        _type = type;
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
            firePropertyChangedEvent("id", _id, id);
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
    public Object getField(String name) {
        return _fields.get(name);
    }

    /**
     * Sets the given field to the given value.
     */
    public void setField(String name, Object value) {
        if ("id".equals(name)) {
            throw new IllegalArgumentException("The field name '" + name +
					       "' is reserved");
        }

	// This mechanism is probably not really necessary.
        //Object normalValue = FieldTypes.normalize(name, value);

	try {
	    firePropertyChangedEvent(name, _fields.get(name), value);
	} catch (PropertyVetoException pve) {
            throw new IllegalArgumentException("Change rejected: " + pve);
	}

        Object oldValue = _fields.put(name, value);
    }

    protected boolean allFieldsPresent(String[] fields) {
        for (int i = 0; i < fields.length; i++) {
	    if (getField(fields[i]) == null) {
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
     */
    public void write(Writer out, FieldFormatter ff) throws IOException {
	// Write header with type and bibtex-key.
	out.write("@"+_type.getName().toUpperCase()+"{");
	
	String str = Util.shaveString((String)getField(GUIGlobals.KEY_FIELD));
	out.write(((str == null) ? "" : str.toString())+",\n");
	HashMap written = new HashMap();
	written.put(GUIGlobals.KEY_FIELD, null);
	// Write required fields first.
	String[] s = getRequiredFields();
	if (s != null) for (int i=0; i<s.length; i++) {
	    writeField(s[i], out, ff);
	    written.put(s[i], null);
	}
	// Then optional fields.
	s = getOptionalFields();
	if (s != null) for (int i=0; i<s.length; i++) {
	    writeField(s[i], out, ff);
	    written.put(s[i], null);
	}
	// Then write remaining fields, if any.
	/*for (int i=0; i<GUIGlobals.ALL_FIELDS.length; i++) {
	    if (!written.containsKey(GUIGlobals.ALL_FIELDS[i]) 
            &&
              GUIGlobals.isWriteableField(GUIGlobals.ALL_FIELDS[i])
              )  
        {
            writeField(GUIGlobals.ALL_FIELDS[i], out);
        }
	}//*/

	for (java.util.Iterator i=_fields.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();
            if (!written.containsKey(key)
                && GUIGlobals.isWriteableField(key))
		
                {
                    writeField(key, out, ff);
                }
        }

	// Finally, end the entry.
	out.write("}\n");
    }

    private void writeField(String name, Writer out,
			    FieldFormatter ff) throws IOException {
	Object o = getField(name);
	if (o != null) {
	    out.write("  "+name+" = ");

	    try {
		out.write(ff.format(o.toString()));
	    } catch (Throwable ex) {
		throw new IOException
		    ("Error in field '"+name+"': "+ex.getMessage()); 
	    }
	    //Util.writeField(name, o, out);
	    out.write(",\n");
	}
    }

    /**
     * Returns a clone of this entry. Useful for copying.
     */
    public Object clone() {
	BibtexEntry clone = new BibtexEntry(_id, _type);
	clone._fields = (Map)((HashMap)_fields).clone();	
	return clone;
    }

}
