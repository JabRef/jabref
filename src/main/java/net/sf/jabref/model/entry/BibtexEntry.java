/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.model.entry;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.model.database.BibtexDatabase;

public class BibtexEntry {

    private static final Log LOGGER = LogFactory.getLog(BibtexEntry.class);

    public static final String TYPE_HEADER = "entrytype";
    public static final String KEY_FIELD = "bibtexkey";
    private static final String ID_FIELD = "id";

    private String id;

    private EntryType type;

    private Map<String, String> fields = new HashMap<>();

    private final VetoableChangeSupport changeSupport = new VetoableChangeSupport(this);

    // Search and grouping status is stored in boolean fields for quick reference:
    private boolean searchHit;
    private boolean groupHit;


    public BibtexEntry() {
        this(IdGenerator.next());
    }

    public BibtexEntry(String id) {
        this(id, BibtexEntryTypes.MISC);
    }

    public BibtexEntry(String id, EntryType type) {
        Objects.requireNonNull(id, "Every BibtexEntry must have an ID");

        this.id = id;
        setType(type);
    }

    /**
     * @return An array describing the optional fields for this entry. "null" if no fields are required
     */
    public List<String> getOptionalFields() {
        return type.getOptionalFields();
    }

    /**
     * Returns all required field names.
     * If fields have an OR relationship the name includes both field names divided by /, e.g. author/editor.
     * If you need all required fields as sole entities use @see{getRequiredFieldsFlat} .
     *
     * @return a List of required field name Strings
     */
    public List<String> getRequiredFields() {
        return type.getRequiredFields();
    }

    /**
     * Returns all required field names.
     * No OR relationships are captured here.
     *
     * @return a List of required field name Strings
     */
    public List<String> getRequiredFieldsFlat() {
        return type.getRequiredFieldsFlat();
    }

    /**
     * Returns an set containing the names of all fields that are
     * set for this particular entry.
     *
     * @return a set of existing field names
     */
    public Set<String> getFieldNames() {
        return new TreeSet<>(fields.keySet());
    }

    /**
     * Returns true if this entry contains the fields it needs to be
     * complete.
     */
    public boolean hasAllRequiredFields(BibtexDatabase database) {
        return allFieldsPresent(type.getRequiredFields(), database);
    }

    /**
     * Returns this entry's type.
     */
    public EntryType getType() {
        return type;
    }

    /**
     * Sets this entry's type.
     */
    public void setType(EntryType type) {
        Objects.requireNonNull(type, "Every BibtexEntry must have a type.");

        EntryType oldType = this.type;

        try {
            // We set the type before throwing the changeEvent, to enable
            // the change listener to access the new value if the change
            // sets off a change in database sorting etc.
            this.type = type;
            firePropertyChangedEvent(TYPE_HEADER, oldType != null ? oldType.getName() : null, type.getName());
        } catch (PropertyVetoException pve) {
            pve.printStackTrace();
        }

    }

    /**
     * Sets this entry's ID, provided the database containing it
     * doesn't veto the change.
     */
    public void setId(String id) {
        Objects.requireNonNull(id, "Every BibtexEntry must have an ID");

        try {
            firePropertyChangedEvent(BibtexEntry.ID_FIELD, this.id, id);
        } catch (PropertyVetoException pv) {
            throw new IllegalStateException("Couldn't change ID: " + pv);
        }

        this.id = id;
    }

    /**
     * Returns this entry's ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the contents of the given field, or null if it is not set.
     */
    public String getField(String name) {
        return fields.get(name);
    }

    /**
     * Returns the contents of the given field, its alias or null if both are
     * not set.
     * <p>
     * The following aliases are considered (old bibtex <-> new biblatex) based
     * on the BibLatex documentation, chapter 2.2.5:
     * address 		<-> location
     * annote			<-> annotation
     * archiveprefix 	<-> eprinttype
     * journal 		<-> journaltitle
     * key				<-> sortkey
     * pdf 			<-> file
     * primaryclass 	<-> eprintclass
     * school 			<-> institution
     * These work bidirectional.
     * <p>
     * Special attention is paid to dates: (see the BibLatex documentation,
     * chapter 2.3.8)
     * The fields 'year' and 'month' are used if the 'date'
     * field is empty. Conversely, getFieldOrAlias("year") also tries to
     * extract the year from the 'date' field (analogously for 'month').
     */
    public String getFieldOrAlias(String name) {
        String fieldValue = getField(name);

        if ((fieldValue != null) && !fieldValue.isEmpty()) {
            return fieldValue;
        }

        // No value of this field found, so look at the alias
        String aliasForField = EntryConverter.FIELD_ALIASES.get(name);

        if (aliasForField != null) {
            return getField(aliasForField);
        }

        // Finally, handle dates
        if (name.equals("date")) {
            String year = getField("year");
            MonthUtil.Month month = MonthUtil.getMonth(getField("month"));
            if (year != null) {
                if (month.isValid()) {
                    return year + '-' + month.twoDigitNumber;
                } else {
                    return year;
                }
            }
        }
        if (name.equals("year") || name.equals("month")) {
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
                public StringBuffer format(Date dDate, StringBuffer toAppendTo, FieldPosition fieldPosition) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Date parse(String source, ParsePosition pos) {
                    if ((source.length() - pos.getIndex()) == FORMAT1.length()) {
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
                if (name.equals("month")) {
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
                    LOGGER.warn("Could not parse entry " + name, e2);
                    return null; // Date field not in valid format
                }
            }
        }
        return null;
    }

    /**
     * Returns the bibtex key, or null if it is not set.
     */
    public String getCiteKey() {
        return fields.get(KEY_FIELD);
    }

    public boolean hasCiteKey() {
        if ((getCiteKey() == null) || getCiteKey().isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Sets a number of fields simultaneously. The given HashMap contains field
     * names as keys, each mapped to the value to set.
     * WARNING: this method does not notify change listeners, so it should *NOT*
     * be used for entries that are being displayed in the GUI. Furthermore, it
     * does not check values for content, so e.g. empty strings will be set as such.
     */
    public void setField(Map<String, String> fields) {
        this.fields.putAll(fields);
    }

    /**
     * Set a field, and notify listeners about the change.
     *
     * @param name  The field to set.
     * @param value The value to set.
     */
    public void setField(String name, String value) {

        if (BibtexEntry.ID_FIELD.equals(name)) {
            throw new IllegalArgumentException("The field name '" + name + "' is reserved");
        }

        String oldValue = fields.get(name);
        try {
            // We set the field before throwing the changeEvent, to enable
            // the change listener to access the new value if the change
            // sets off a change in database sorting etc.
            fields.put(name, value);
            firePropertyChangedEvent(name, oldValue, value);
        } catch (PropertyVetoException pve) {
            // Since we have already made the change, we must undo it since
            // the change was rejected:
            fields.put(name, oldValue);
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
            throw new IllegalArgumentException("The field name '" + name + "' is reserved");
        }
        Object oldValue = fields.get(name);
        fields.remove(name);
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
     * @param allFields   An array of field names to be checked.
     * @param database The database in which to look up crossref'd entries, if any. This
     *                 argument can be null, meaning that no attempt will be made to follow crossrefs.
     * @return true if all fields are set or could be resolved, false otherwise.
     */
    boolean allFieldsPresent(String[] allFields, BibtexDatabase database) {
        final String orSeparator = "/";

        for (String field : allFields) {
            // OR fields
            if (field.contains(orSeparator)) {
                String[] altFields = field.split(orSeparator);

                if (!atLeastOnePresent(altFields, database)) {
                    return false;
                }
            } else {
                if (BibtexDatabase.getResolvedField(field, this, database) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean allFieldsPresent(List<String> allFields, BibtexDatabase database) {
        return allFieldsPresent(allFields.toArray(new String[allFields.size()]), database);
    }

    private boolean atLeastOnePresent(String[] fieldsToCheck, BibtexDatabase database) {
        for (String field : fieldsToCheck) {
            String value = BibtexDatabase.getResolvedField(field, this, database);
            if ((value != null) && !value.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void firePropertyChangedEvent(String fieldName, Object oldValue, Object newValue)
            throws PropertyVetoException {
        changeSupport.fireVetoableChange(new PropertyChangeEvent(this, fieldName, oldValue, newValue));
    }

    /**
     * Adds a VetoableChangeListener, which is notified of field
     * changes. This is useful for an object that needs to update
     * itself each time a field changes.
     */
    public void addPropertyChangeListener(VetoableChangeListener listener) {
        changeSupport.addVetoableChangeListener(listener);
    }

    /**
     * Removes a property listener.
     */
    public void removePropertyChangeListener(VetoableChangeListener listener) {
        changeSupport.removeVetoableChangeListener(listener);
    }

    /**
     * Returns a clone of this entry. Useful for copying.
     */
    @Override
    public Object clone() {
        BibtexEntry clone = new BibtexEntry(id, type);
        clone.fields = new HashMap<>(fields);
        return clone;
    }

    @Override
    public String toString() {
        return getType().getName() + ':' + getCiteKey();
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
     *                      characters are replaced with "..."). Set to 0 to disable truncation.
     * @return A short textual description of the entry in the format:
     * Author1, Author2: Title (Year)
     */
    public String getAuthorTitleYear(int maxCharacters) {
        String[] s = new String[] {getField("author"), getField("title"), getField("year")};

        for (int i = 0; i < s.length; ++i) {
            if (s[i] == null) {
                s[i] = "N/A";
            }
        }
        String text = s[0] + ": \"" + s[1] + "\" (" + s[2] + ')';
        if ((maxCharacters <= 0) || (text.length() <= maxCharacters)) {
            return text;
        }
        return text.substring(0, maxCharacters + 1) + "...";
    }

    /**
     * Will return the publication date of the given bibtex entry conforming to ISO 8601, i.e. either YYYY or YYYY-MM.
     *
     * @param entry
     * @return will return the publication date of the entry or null if no year was found.
     */
    public String getPublicationDate() {

        Object o = getField("year");
        if (o == null) {
            return null;
        }

        String year = YearUtil.toFourDigitYear(o.toString());

        o = getField("month");
        if (o != null) {
            MonthUtil.Month month = MonthUtil.getMonth(o.toString());
            if (month.isValid()) {
                return year + "-" + month.twoDigitNumber;
            }
        }
        return year;
    }
}
