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
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Strings;

import net.sf.jabref.model.database.BibDatabase;

public class BibEntry {
    private static final Log LOGGER = LogFactory.getLog(BibEntry.class);

    public static final String TYPE_HEADER = "entrytype";
    public static final String KEY_FIELD = "bibtexkey";
    protected static final String ID_FIELD = "id";
    public static final String DEFAULT_TYPE = "misc";

    private String id;
    private String type;
    private Map<String, String> fields = new HashMap<>();

    private final VetoableChangeSupport changeSupport = new VetoableChangeSupport(this);

    // Search and grouping status is stored in boolean fields for quick reference:
    private boolean searchHit;
    private boolean groupHit;

    private String parsedSerialization;

    /*
    * marks if the complete serialization, which was read from file, should be used.
    * Is set to false, if parts of the entry change
     */
    private boolean changed;

    public BibEntry() {
        this(IdGenerator.next());
    }

    public BibEntry(String id) {
        this(id, DEFAULT_TYPE);
    }

    public BibEntry(String id, String type) {
        Objects.requireNonNull(id, "Every BibEntry must have an ID");

        this.id = id;
        changed = true;
        setType(type);
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
     * Returns this entry's type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets this entry's type.
     */
    public void setType(String type) {
        String newType;
        if ((type == null) || type.isEmpty()) {
            newType = DEFAULT_TYPE;
        } else {
            newType = type;
        }

        String oldType = this.type;

        try {
            // We set the type before throwing the changeEvent, to enable
            // the change listener to access the new value if the change
            // sets off a change in database sorting etc.
            this.type = newType.toLowerCase(Locale.ENGLISH);
            changed = true;
            firePropertyChangedEvent(TYPE_HEADER, oldType, newType);
        } catch (PropertyVetoException pve) {
            LOGGER.warn(pve);
        }
    }

    /**
     * Sets this entry's type.
     */
    public void setType(EntryType type) {
        this.setType(type.getName());
    }

    /**
     * Sets this entry's ID, provided the database containing it
     * doesn't veto the change.
     */
    public void setId(String id) {
        Objects.requireNonNull(id, "Every BibEntry must have an ID");

        try {
            firePropertyChangedEvent(BibEntry.ID_FIELD, this.id, id);
        } catch (PropertyVetoException pv) {
            throw new IllegalStateException("Couldn't change ID: " + pv);
        }

        this.id = id;
        changed = true;
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
        return fields.get(normalizeFieldName(name));
    }

    /**
     * Returns the contents of the given field as an Optional.
     */
    public Optional<String> getFieldOptional(String name) {
        return Optional.ofNullable(fields.get(normalizeFieldName(name)));
    }

    /**
     * Returns true if the entry has the given field, or false if it is not set.
     */
    public boolean hasField(String name) {
        return fields.containsKey(normalizeFieldName(name));
    }

    private String normalizeFieldName(String fieldName) {
        Objects.requireNonNull(fieldName, "field name must not be null");

        return fieldName.toLowerCase(Locale.ENGLISH);
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
        String fieldValue = getField(normalizeFieldName(name));

        if (!Strings.isNullOrEmpty(fieldValue)) {
            return fieldValue;
        }

        // No value of this field found, so look at the alias
        String aliasForField = EntryConverter.FIELD_ALIASES.get(name);

        if (aliasForField != null) {
            return getField(aliasForField);
        }

        // Finally, handle dates
        if ("date".equals(name)) {
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
        if ("year".equals(name) || "month".equals(name)) {
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
                if ("year".equals(name)) {
                    return Integer.toString(calendar.get(Calendar.YEAR));
                }
                if ("month".equals(name)) {
                    return Integer.toString(calendar.get(Calendar.MONTH) + 1); // Shift by 1 since in this calendar Jan = 0
                }
            } catch (ParseException e) {
                // So not a date with year and month, try just to parse years
                df = new SimpleDateFormat("yyyy");

                try {
                    Date parsedDate = df.parse(date);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(parsedDate);
                    if ("year".equals(name)) {
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

    public void setCiteKey(String newCiteKey) {
        setField(KEY_FIELD, newCiteKey);
    }

    public boolean hasCiteKey() {
        return !Strings.isNullOrEmpty(getCiteKey());
    }

    /**
     * Sets a number of fields simultaneously. The given HashMap contains field
     * names as keys, each mapped to the value to set.
     * WARNING: this method does not notify change listeners, so it should *NOT*
     * be used for entries that are being displayed in the GUI. Furthermore, it
     * does not check values for content, so e.g. empty strings will be set as such.
     */
    public void setField(Map<String, String> fields) {
        Objects.requireNonNull(fields, "fields must not be null");

        changed = true;
        this.fields.putAll(fields);
    }

    /**
     * Set a field, and notify listeners about the change.
     *
     * @param name  The field to set.
     * @param value The value to set.
     */
    public void setField(String name, String value) {
        Objects.requireNonNull(name, "field name must not be null");
        Objects.requireNonNull(value, "field value must not be null");

        String fieldName = normalizeFieldName(name);

        if (BibEntry.ID_FIELD.equals(fieldName)) {
            throw new IllegalArgumentException("The field name '" + name + "' is reserved");
        }

        changed = true;

        String oldValue = fields.get(fieldName);
        try {
            // We set the field before throwing the changeEvent, to enable
            // the change listener to access the new value if the change
            // sets off a change in database sorting etc.
            fields.put(fieldName, value);
            firePropertyChangedEvent(fieldName, oldValue, value);
        } catch (PropertyVetoException pve) {
            // Since we have already made the change, we must undo it since
            // the change was rejected:
            fields.put(fieldName, oldValue);
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
        String fieldName = normalizeFieldName(name);

        changed = true;

        if (BibEntry.ID_FIELD.equals(fieldName)) {
            throw new IllegalArgumentException("The field name '" + name + "' is reserved");
        }
        Object oldValue = fields.get(fieldName);
        fields.remove(fieldName);
        try {
            firePropertyChangedEvent(fieldName, oldValue, null);
        } catch (PropertyVetoException pve) {
            throw new IllegalArgumentException("Change rejected: " + pve);
        }

    }

    /**
     * Determines whether this entry has all the given fields present. If a non-null
     * database argument is given, this method will try to look up missing fields in
     * entries linked by the "crossref" field, if any.
     *
     * @param allFields An array of field names to be checked.
     * @param database  The database in which to look up crossref'd entries, if any. This
     *                  argument can be null, meaning that no attempt will be made to follow crossrefs.
     * @return true if all fields are set or could be resolved, false otherwise.
     */
    public boolean allFieldsPresent(List<String> allFields, BibDatabase database) {
        final String orSeparator = "/";

        for (String field : allFields) {
            String fieldName = normalizeFieldName(field);
            // OR fields
            if (fieldName.contains(orSeparator)) {
                String[] altFields = field.split(orSeparator);

                if (!atLeastOnePresent(altFields, database)) {
                    return false;
                }
            } else {
                if (BibDatabase.getResolvedField(fieldName, this, database) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean atLeastOnePresent(String[] fieldsToCheck, BibDatabase database) {
        for (String field : fieldsToCheck) {
            String fieldName = normalizeFieldName(field);

            String value = BibDatabase.getResolvedField(fieldName, this, database);
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
        BibEntry clone = new BibEntry(id, type);
        clone.fields = new HashMap<>(fields);
        return clone;
    }

    /**
     * This returns a canonical BibTeX serialization. Special characters such as "{" or "&" are NOT escaped, but written
     * as is
     * <p>
     * Serializes all fields, even the JabRef internal ones. Does NOT serialize "KEY_FIELD" as field, but as key
     */
    @Override
    public String toString() {
        return CanonicalBibtexEntry.getCanonicalRepresentation(this);
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
        String[] s = new String[] {getFieldOptional("author").orElse("N/A"), getFieldOptional("title").orElse("N/A"),
                getFieldOptional("year").orElse("N/A")};

        String text = s[0] + ": \"" + s[1] + "\" (" + s[2] + ')';
        if ((maxCharacters <= 0) || (text.length() <= maxCharacters)) {
            return text;
        }
        return text.substring(0, maxCharacters + 1) + "...";
    }

    /**
     * Will return the publication date of the given bibtex entry conforming to ISO 8601, i.e. either YYYY or YYYY-MM.
     *
     * @return will return the publication date of the entry or null if no year was found.
     */
    public String getPublicationDate() {
        if (!hasField("year")) {
            return null;
        }

        String year = getField("year");

        if (hasField("month")) {
            MonthUtil.Month month = MonthUtil.getMonth(getField("month"));
            if (month.isValid()) {
                return year + "-" + month.twoDigitNumber;
            }
        }
        return year;
    }


    public void setParsedSerialization(String parsedSerialization) {
        changed = false;
        this.parsedSerialization = parsedSerialization;
    }

    public String getParsedSerialization() {
        return parsedSerialization;
    }

    public boolean hasChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public void putKeywords(List<String> keywords) {
        Objects.requireNonNull(keywords);
        // Set Keyword Field
        String oldValue = this.getField("keywords");
        String newValue;
        if (keywords.isEmpty()) {
            newValue = null;
        } else {
            newValue = String.join(", ", keywords);
        }
        if (newValue == null) {
            if (oldValue != null) {
                this.clearField("keywords");
                changed = true;
            }
            return;
        }
        if ((oldValue == null) || !oldValue.equals(newValue)) {
            this.setField("keywords", newValue);
            changed = true;
        }
    }

    /**
     * Check if a keyword already exists (case insensitive), if not: add it
     *
     * @param keyword Keyword to add
     */
    public void addKeyword(String keyword) {
        Objects.requireNonNull(keyword, "keyword must not be empty");

        if (keyword.isEmpty()) {
            return;
        }

        List<String> keywords = this.getSeparatedKeywords();
        Boolean duplicate = false;

        for (String key : keywords) {
            if (keyword.equalsIgnoreCase(key)) {
                duplicate = true;
                break;
            }
        }

        if (!duplicate) {
            keywords.add(keyword);
            this.putKeywords(keywords);
        }
    }

    /**
     * Add multiple keywords to entry
     *
     * @param keywords Keywords to add
     */
    public void addKeywords(List<String> keywords) {
        Objects.requireNonNull(keywords);

        for (String keyword : keywords) {
            this.addKeyword(keyword);
        }
    }

    public List<String> getSeparatedKeywords() {
        return net.sf.jabref.model.entry.EntryUtil.getSeparatedKeywords(this.getField("keywords"));
    }

    public Collection<String> getFieldValues() {
        return fields.values();
    }
}
