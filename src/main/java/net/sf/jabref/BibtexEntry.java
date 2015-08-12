/*
 * Copyright (C) 2003-2003 David Weitzman, Morten O. Alver
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

import net.sf.jabref.export.FieldFormatter;
import net.sf.jabref.logic.util.MonthUtil;

public class BibtexEntry
{

    private static final String ID_FIELD = "id";
    public static final Map<String, String> FieldAliasesOldToNew = new HashMap<String, String>(); // Bibtex to BibLatex
    public static final Map<String, String> FieldAliasesNewToOld = new HashMap<String, String>(); // BibLatex to Bibtex

    static {
        BibtexEntry.FieldAliasesOldToNew.put("address", "location");
        BibtexEntry.FieldAliasesNewToOld.put("location", "address");

        BibtexEntry.FieldAliasesOldToNew.put("annote", "annotation");
        BibtexEntry.FieldAliasesNewToOld.put("annotation", "annote");

        BibtexEntry.FieldAliasesOldToNew.put("archiveprefix", "eprinttype");
        BibtexEntry.FieldAliasesNewToOld.put("eprinttype", "archiveprefix");

        BibtexEntry.FieldAliasesOldToNew.put("journal", "journaltitle");
        BibtexEntry.FieldAliasesNewToOld.put("journaltitle", "journal");

        BibtexEntry.FieldAliasesOldToNew.put("key", "sortkey");
        BibtexEntry.FieldAliasesNewToOld.put("sortkey", "key");

        BibtexEntry.FieldAliasesOldToNew.put("pdf", "file");
        BibtexEntry.FieldAliasesNewToOld.put("file", "pdf");

        BibtexEntry.FieldAliasesOldToNew.put("primaryclass", "eprintclass");
        BibtexEntry.FieldAliasesNewToOld.put("eprintclass", "primaryclass");

        BibtexEntry.FieldAliasesOldToNew.put("school", "institution");
        BibtexEntry.FieldAliasesNewToOld.put("institution", "school");
    }

    private String _id;
    private BibtexEntryType _type;
    private Map<String, String> _fields = new HashMap<String, String>();
    private final VetoableChangeSupport _changeSupport = new VetoableChangeSupport(this);

    // Search and grouping status is stored in boolean fields for quick reference:
    private boolean searchHit;
    private boolean groupHit;


    public BibtexEntry() {
        this(IdGenerator.next());
    }

    public BibtexEntry(String id)
    {
        this(id, BibtexEntryTypes.OTHER);
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
        return _type.getOptionalFields().clone();
    }

    /**
     * Returns an array describing the required fields for this entry.
     */
    public String[] getRequiredFields()
    {
        return _type.getRequiredFields().clone();
    }

    public String[] getUserDefinedFields()
    {

        return Globals.prefs.getStringArray(JabRefPreferences.WRITEFIELD_USERDEFINEDORDER);
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
     * BibtexEntryTypes.TYPELESS).
     */
    public boolean updateType() {
        BibtexEntryType newType = BibtexEntryType.getType(_type.getName());
        if (newType != null) {
            _type = newType;
            return true;
        }
        _type = BibtexEntryTypes.TYPELESS;
        return false;
    }

    /**
     * Sets this entry's ID, provided the database containing it
     * doesn't veto the change.
     */
    public void setId(String id) throws KeyCollisionException {

        if (id == null) {
            throw new NullPointerException("Every BibtexEntry must have an ID");
        }

        try
        {
            firePropertyChangedEvent(BibtexEntry.ID_FIELD, _id, id);
        } catch (PropertyVetoException pv)
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

    /**
     * Returns the contents of the given field, its alias or null if both are
     * not set.
     * 
     * The following aliases are considered (old bibtex <-> new biblatex) based
     * on the BibLatex documentation, chapter 2.2.5:
     *  address 		<-> location
     *  annote			<-> annotation 
     *  archiveprefix 	<-> eprinttype 
     *  journal 		<-> journaltitle 
     *  key				<-> sortkey 
     * 	pdf 			<-> file 
     * 	primaryclass 	<-> eprintclass 
     * 	school 			<-> institution 
     * These work bidirectional.
     * 
     * Special attention is paid to dates: (see the BibLatex documentation,
     * chapter 2.3.8) 
     * 	The fields 'year' and 'month' are used if the 'date'
     * 	field is empty. Conversely, getFieldOrAlias("year") also tries to
     * 	extract the year from the 'date' field (analogously for 'month').
     */
    public String getFieldOrAlias(String name) {
        String fieldValue = getField(name);
        if (fieldValue != null && !fieldValue.isEmpty()) {
            return fieldValue;
        }

        // No value of this field found, so look at the alias

        // Create bidirectional dictionary between field names and their aliases
        Map<String, String> aliases = new HashMap<String, String>();
        aliases.putAll(BibtexEntry.FieldAliasesOldToNew);
        aliases.putAll(BibtexEntry.FieldAliasesNewToOld);

        String aliasForField = aliases.get(name);
        if (aliasForField != null) {
            return getField(aliasForField);
        }

        // So we did not found the field itself or its alias...
        // Finally, handle dates
        if (name.equals("date"))
        {
            String year = getField("year");
            MonthUtil.Month month = MonthUtil.getMonth(getField("month"));
            if (year != null)
            {
                if (month.isValid()) {
                    return year + '-' + month.twoDigitNumber;
                } else {
                    return year;
                }
            }
        }
        if (name.equals("year") || name.equals("month"))
        {
            String date = getField("date");
            if (date == null) {
                return null;
            }

            // Create date format matching dates with year and month
            DateFormat df = new DateFormat() {

                static final String FORMAT1 = "yyyy-MM-dd";
                static final String FORMAT2 = "yyyy-MM";
                final SimpleDateFormat sdf1 = new SimpleDateFormat(FORMAT1);
                final SimpleDateFormat sdf2 = new SimpleDateFormat(FORMAT2);


                @Override
                public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Date parse(String source, ParsePosition pos) {
                    if (source.length() - pos.getIndex() == FORMAT1.length()) {
                        return sdf1.parse(source, pos);
                    }
                    return sdf2.parse(source, pos);
                }
            };

            try {
                Date parsedDate = df.parse(date);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(parsedDate);
                if (name.equals("year")) {
                    return Integer.toString(calendar.get(Calendar.YEAR));
                }
                if (name.equals("month"))
                 {
                    return Integer.toString(calendar.get(Calendar.MONTH) + 1); // Shift by 1 since in this calendar Jan = 0			
                }
            } catch (ParseException e) {
                // So not a date with year and month, try just to parse years
                df = new SimpleDateFormat("yyyy");

                try {
                    Date parsedDate = df.parse(date);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(parsedDate);
                    if (name.equals("year")) {
                        return Integer.toString(calendar.get(Calendar.YEAR));
                    }
                } catch (ParseException e2) {
                    return null; // Date field not in valid format
                }
            }
        }

        return null;
    }

    public String getCiteKey() {
        return _fields.containsKey(BibtexFields.KEY_FIELD) ?
                _fields.get(BibtexFields.KEY_FIELD) : null;
    }

    /**
     * Sets a number of fields simultaneously. The given HashMap contains field
     * names as keys, each mapped to the value to set.
     * WARNING: this method does not notify change listeners, so it should *NOT*
     * be used for entries that are being displayed in the GUI. Furthermore, it
     * does not check values for content, so e.g. empty strings will be set as such.
     */
    public void setField(Map<String, String> fields) {
        _fields.putAll(fields);
    }

    /**
     * Set a field, and notify listeners about the change.
     *
     * @param name The field to set.
     * @param value The value to set.
     */
    public void setField(String name, String value) {

        if (BibtexEntry.ID_FIELD.equals(name)) {
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

        if (BibtexEntry.ID_FIELD.equals(name)) {
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
    boolean allFieldsPresent(String[] fields, BibtexDatabase database) {
        for (String field : fields) {
            if (BibtexDatabase.getResolvedField(field, this, database) == null) {
                return false;
            }
        }

        return true;
    }

    boolean atLeastOnePresent(String[] fields, BibtexDatabase database) {
        for (String field : fields) {
            String value = BibtexDatabase.getResolvedField(field, this, database);
            if (value != null && !value.isEmpty()) {
                return true;
            }
        }
        return false;
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
        new BibtexEntryWriter(ff, write).write(this, out);
    }

    /**
     * Returns a clone of this entry. Useful for copying.
     */
    @Override
    public Object clone() {
        BibtexEntry clone = new BibtexEntry(_id, _type);
        clone._fields = new HashMap<String, String>(_fields);
        return clone;
    }

    @Override
    public String toString() {
        return getType().getName() + ':' + getField(BibtexFields.KEY_FIELD);
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
        for (int i = 0; i < s.length; ++i) {
            if (s[i] == null) {
                s[i] = "N/A";
            }
        }
        String text = s[0] + ": \"" + s[1] + "\" (" + s[2] + ')';
        if (maxCharacters <= 0 || text.length() <= maxCharacters) {
            return text;
        }
        return text.substring(0, maxCharacters + 1) + "...";
    }
}
