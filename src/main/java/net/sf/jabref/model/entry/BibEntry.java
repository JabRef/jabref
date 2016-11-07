package net.sf.jabref.model.entry;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.event.EntryEventSource;
import net.sf.jabref.model.entry.event.FieldChangedEvent;
import net.sf.jabref.model.strings.LatexToUnicode;
import net.sf.jabref.model.strings.StringUtil;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BibEntry implements Cloneable {

    private static final Log LOGGER = LogFactory.getLog(BibEntry.class);

    public static final String TYPE_HEADER = "entrytype";
    public static final String OBSOLETE_TYPE_HEADER = "bibtextype";
    public static final String KEY_FIELD = "bibtexkey";
    protected static final String ID_FIELD = "id";
    public static final String DEFAULT_TYPE = "misc";

    private static final Pattern REMOVE_TRAILING_WHITESPACE = Pattern.compile("\\s+$");

    private String id;

    private final SharedBibEntryData sharedBibEntryData;

    private String type;
    private Map<String, String> fields = new ConcurrentHashMap<>();

    /**
     * Map to store the words in every field
     */
    private final Map<String, Set<String>> fieldsAsWords = new HashMap<>();

    /**
     * Cache that stores latex free versions of fields.
     */
    private final Map<String, String> latexFreeFields = new ConcurrentHashMap<>();

    /**
     * Used to cleanse field values for internal LaTeX-free storage
     */
    private LatexToUnicode unicodeConverter = new LatexToUnicode();

    // Search and grouping status is stored in boolean fields for quick reference:
    private boolean searchHit;
    private boolean groupHit;

    private String parsedSerialization;

    private String commentsBeforeEntry = "";

    /**
     * Marks whether the complete serialization, which was read from file, should be used.
     *
     * Is set to false, if parts of the entry change. This causes the entry to be serialized based on the internal state (and not based on the old serialization)
     */
    private boolean changed;

    private final EventBus eventBus = new EventBus();


    /**
     * Constructs a new BibEntry. The internal ID is set to IdGenerator.next()
     */

    public BibEntry() {
        this(IdGenerator.next());
    }

    /**
     * Constructs a new BibEntry with the given ID and DEFAULT_TYPE
     *
     * @param id The ID to be used
     */
    public BibEntry(String id) {
        this(id, DEFAULT_TYPE);
    }

    /**
     * Constructs a new BibEntry with the given ID and given type
     *
     * @param id   The ID to be used
     * @param type The type to set. May be null or empty. In that case, DEFAULT_TYPE is used.
     */
    public BibEntry(String id, String type) {
        Objects.requireNonNull(id, "Every BibEntry must have an ID");

        this.id = id;
        setType(type);
        this.sharedBibEntryData = new SharedBibEntryData();
    }

    public Optional<FieldChange> replaceKeywords(KeywordList keywordsToReplace, Optional<Keyword> newValue,
                                                 Character keywordDelimiter) {
        KeywordList keywordList = getKeywords(keywordDelimiter);
        keywordList.replaceKeywords(keywordsToReplace, newValue);

        return putKeywords(keywordList, keywordDelimiter);
    }

    /**
     * Returns the text stored in the given field of the given bibtex entry
     * which belongs to the given database.
     * <p>
     * If a database is given, this function will try to resolve any string
     * references in the field-value.
     * Also, if a database is given, this function will try to find values for
     * unset fields in the entry linked by the "crossref" field, if any.
     *
     * @param field    The field to return the value of.
     * @param database maybenull
     *                 The database of the bibtex entry.
     * @return The resolved field value or null if not found.
     */
    public Optional<String> getResolvedFieldOrAlias(String field, BibDatabase database) {
        Objects.requireNonNull(this, "entry cannot be null");

        if (TYPE_HEADER.equals(field) || OBSOLETE_TYPE_HEADER.equals(field)) {
            Optional<EntryType> entryType = EntryTypes.getType(getType(), BibDatabaseMode.BIBLATEX);
            if (entryType.isPresent()) {
                return Optional.of(entryType.get().getName());
            } else {
                return Optional.of(StringUtil.capitalizeFirst(getType()));
            }
        }

        if (KEY_FIELD.equals(field)) {
            return getCiteKeyOptional();
        }

        Optional<String> result = getFieldOrAlias(field);

        // If this field is not set, and the entry has a crossref, try to look up the
        // field in the referred entry: Do not do this for the bibtex key.
        if (!result.isPresent() && (database != null)) {
            Optional<BibEntry> referred = database.getReferencedEntry(this);
            result = referred.flatMap(entry -> entry.getFieldOrAlias(field));
        }
        return result.map(resultText -> BibDatabase.getText(resultText, database));
    }

    /**
     * Sets this entry's ID, provided the database containing it
     * doesn't veto the change.
     *
     * @param id The ID to be used
     */
    public void setId(String id) {
        Objects.requireNonNull(id, "Every BibEntry must have an ID");

        String oldId = this.id;

        eventBus.post(new FieldChangedEvent(this, BibEntry.ID_FIELD, id, oldId));
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
     * Sets the cite key AKA citation key AKA BibTeX key.
     * Note: This is <emph>not</emph> the internal Id of this entry. The internal Id is always present, whereas the BibTeX key might not be present.
     *
     * @param newCiteKey The cite key to set. Must not be null; use {@link #clearCiteKey()} to remove the cite key.
     */
    public void setCiteKey(String newCiteKey) {
        setField(KEY_FIELD, newCiteKey);
    }

    /**
     * Returns the cite key AKA citation key AKA BibTeX key, or null if it is not set.
     * Note: this is <emph>not</emph> the internal Id of this entry. The internal Id is always present, whereas the BibTeX key might not be present.
     */
    @Deprecated
    public String getCiteKey() {
        return fields.get(KEY_FIELD);
    }

    public Optional<String> getCiteKeyOptional() {
        return Optional.ofNullable(fields.get(KEY_FIELD));
    }

    public boolean hasCiteKey() {
        return !Strings.isNullOrEmpty(getCiteKey());
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
    public void setType(String type, EntryEventSource eventSource) {
        String newType;
        if (Strings.isNullOrEmpty(type)) {
            newType = DEFAULT_TYPE;
        } else {
            newType = type;
        }
        String oldType = getField(TYPE_HEADER).orElse(null);

        // We set the type before throwing the changeEvent, to enable
        // the change listener to access the new value if the change
        // sets off a change in database sorting etc.
        this.type = newType.toLowerCase(Locale.ENGLISH);
        changed = true;
        eventBus.post(new FieldChangedEvent(this, TYPE_HEADER, newType, oldType, eventSource));
    }

    /**
     * Sets this entry's type.
     */
    public void setType(String type) {
        setType(type, EntryEventSource.LOCAL);
    }

    /**
     * Sets this entry's type.
     */
    public void setType(EntryType type) {
        this.setType(type.getName());
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
     * Returns the contents of the given field as an Optional.
     */
    public Optional<String> getField(String name) {
        return Optional.ofNullable(fields.get(toLowerCase(name)));
    }

    /**
     * Returns true if the entry has the given field, or false if it is not set.
     */
    public boolean hasField(String name) {
        return fields.containsKey(toLowerCase(name));
    }

    private String toLowerCase(String fieldName) {
        Objects.requireNonNull(fieldName, "field name must not be null");

        return fieldName.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Internal method used to get the content of a field (or its alias)
     *
     * Used by {@link #getFieldOrAlias(String)} and {@link #getFieldOrAliasLatexFree(String)}
     *
     * @param name name of the field
     * @param getFieldInterface
     *
     * @return determined field value
     */
    private Optional<String> genericGetFieldOrAlias(String name, GetFieldInterface getFieldInterface) {
        Optional<String> fieldValue = getFieldInterface.getValueForField(toLowerCase(name));

        if (fieldValue.isPresent() && !fieldValue.get().isEmpty()) {
            return fieldValue;
        }

        // No value of this field found, so look at the alias
        String aliasForField = EntryConverter.FIELD_ALIASES.get(name);

        if (aliasForField != null) {
            return getFieldInterface.getValueForField(aliasForField);
        }

        // Finally, handle dates
        if (FieldName.DATE.equals(name)) {
            Optional<String> year = getFieldInterface.getValueForField(FieldName.YEAR);
            if (year.isPresent()) {
                MonthUtil.Month month = MonthUtil.getMonth(getFieldInterface.getValueForField(FieldName.MONTH).orElse(""));
                if (month.isValid()) {
                    return Optional.of(year.get() + '-' + month.twoDigitNumber);
                } else {
                    return year;
                }
            }
        }
        if (FieldName.YEAR.equals(name) || FieldName.MONTH.equals(name)) {
            Optional<String> date = getFieldInterface.getValueForField(FieldName.DATE);
            if (!date.isPresent()) {
                return Optional.empty();
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
                Date parsedDate = df.parse(date.get());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(parsedDate);
                if (FieldName.YEAR.equals(name)) {
                    return Optional.of(Integer.toString(calendar.get(Calendar.YEAR)));
                }
                if (FieldName.MONTH.equals(name)) {
                    return Optional.of(Integer.toString(calendar.get(Calendar.MONTH) + 1)); // Shift by 1 since in this calendar Jan = 0
                }
            } catch (ParseException e) {
                // So not a date with year and month, try just to parse years
                df = new SimpleDateFormat("yyyy");

                try {
                    Date parsedDate = df.parse(date.get());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(parsedDate);
                    if (FieldName.YEAR.equals(name)) {
                        return Optional.of(Integer.toString(calendar.get(Calendar.YEAR)));
                    }
                } catch (ParseException e2) {
                    LOGGER.warn("Could not parse entry " + name, e2);
                    return Optional.empty(); // Date field not in valid format
                }
            }
        }
        return Optional.empty();
    }

    private interface GetFieldInterface {
        Optional<String> getValueForField(String fieldName);
    }

    /**
     * Return the LaTeX-free contents of the given field or its alias an an Optional
     *
     * For details see also {@link #getFieldOrAlias(String)}
     *
     * @param name the name of the field
     * @return  the stored latex-free content of the field (or its alias)
     */
    public Optional<String> getFieldOrAliasLatexFree(String name) {
        return genericGetFieldOrAlias(name, this::getLatexFreeField);
    }

    /**
     * Returns the contents of the given field or its alias as an Optional
     * <p>
     * The following aliases are considered (old bibtex <-> new biblatex) based
     * on the BibLatex documentation, chapter 2.2.5:<br>
     * address      <-> location <br>
     * annote           <-> annotation <br>
     * archiveprefix    <-> eprinttype <br>
     * journal      <-> journaltitle <br>
     * key              <-> sortkey <br>
     * pdf          <-> file <br
     * primaryclass     <-> eprintclass <br>
     * school           <-> institution <br>
     * These work bidirectional. <br>
     * <p>
     * Special attention is paid to dates: (see the BibLatex documentation,
     * chapter 2.3.8)
     * The fields 'year' and 'month' are used if the 'date'
     * field is empty. Conversely, getFieldOrAlias("year") also tries to
     * extract the year from the 'date' field (analogously for 'month').
     */
    public Optional<String> getFieldOrAlias(String name) {
        return genericGetFieldOrAlias(name, this::getField);
    }

    /**
     * Sets a number of fields simultaneously. The given HashMap contains field
     * names as keys, each mapped to the value to set.
     */
    public void setField(Map<String, String> fields) {
        Objects.requireNonNull(fields, "fields must not be null");

        fields.forEach(this::setField);
    }

    /**
     * Set a field, and notify listeners about the change.
     *
     * @param name        The field to set
     * @param value       The value to set
     * @param eventSource Source the event is sent from
     */
    public Optional<FieldChange> setField(String name, String value, EntryEventSource eventSource) {
        Objects.requireNonNull(name, "field name must not be null");
        Objects.requireNonNull(value, "field value must not be null");

        String fieldName = toLowerCase(name);

        if (value.isEmpty()) {
            return clearField(fieldName);
        }

        String oldValue = getField(fieldName).orElse(null);
        if (value.equals(oldValue)) {
            return Optional.empty();
        }

        if (BibEntry.ID_FIELD.equals(fieldName)) {
            throw new IllegalArgumentException("The field name '" + name + "' is reserved");
        }

        changed = true;

        fields.put(fieldName, value.intern());
        invalidateFieldCache(fieldName);

        FieldChange change = new FieldChange(this, fieldName, oldValue, value);
        eventBus.post(new FieldChangedEvent(change, eventSource));
        return Optional.of(change);
    }

    public Optional<FieldChange> setField(String name, Optional<String> value, EntryEventSource eventSource) {
        if (value.isPresent()) {
            return setField(name, value.get(), eventSource);
        }
        return Optional.empty();
    }

    /**
     * Set a field, and notify listeners about the change.
     *
     * @param name  The field to set.
     * @param value The value to set.
     */
    public Optional<FieldChange> setField(String name, String value) {
        return setField(name, value, EntryEventSource.LOCAL);
    }

    /**
     * Remove the mapping for the field name, and notify listeners about
     * the change.
     *
     * @param name The field to clear.
     */
    public Optional<FieldChange> clearField(String name) {
        return clearField(name, EntryEventSource.LOCAL);
    }

    /**
     * Remove the mapping for the field name, and notify listeners about
     * the change including the {@link EntryEventSource}.
     *
     * @param name        The field to clear.
     * @param eventSource the source a new {@link FieldChangedEvent} should be posten from.
     */
    public Optional<FieldChange> clearField(String name, EntryEventSource eventSource) {
        String fieldName = toLowerCase(name);

        if (BibEntry.ID_FIELD.equals(fieldName)) {
            throw new IllegalArgumentException("The field name '" + name + "' is reserved");
        }

        Optional<String> oldValue = getField(fieldName);
        if (!oldValue.isPresent()) {
            return Optional.empty();
        }

        changed = true;

        fields.remove(fieldName);
        invalidateFieldCache(fieldName);

        FieldChange change = new FieldChange(this, fieldName, oldValue.get(), null);
        eventBus.post(new FieldChangedEvent(change, eventSource));
        return Optional.of(change);
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

        for (String field : allFields) {
            String fieldName = toLowerCase(field);
            // OR fields
            if (fieldName.contains(FieldName.FIELD_SEPARATOR)) {
                String[] altFields = field.split(FieldName.FIELD_SEPARATOR);

                if (!atLeastOnePresent(altFields, database)) {
                    return false;
                }
            } else {
                if (!this.getResolvedFieldOrAlias(fieldName, database).isPresent()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean atLeastOnePresent(String[] fieldsToCheck, BibDatabase database) {
        for (String field : fieldsToCheck) {
            String fieldName = toLowerCase(field);

            Optional<String> value = this.getResolvedFieldOrAlias(fieldName, database);
            if ((value.isPresent()) && !value.get().isEmpty()) {
                return true;
            }
        }
        return false;
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
        String[] s = new String[]{getField(FieldName.AUTHOR).orElse("N/A"), getField(FieldName.TITLE).orElse("N/A"),
                getField(FieldName.YEAR).orElse("N/A")};

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
    public Optional<String> getPublicationDate() {
        if (!hasField(FieldName.YEAR)) {
            return Optional.empty();
        }

        Optional<String> year = getField(FieldName.YEAR);

        Optional<String> monthString = getField(FieldName.MONTH);
        if (monthString.isPresent()) {
            MonthUtil.Month month = MonthUtil.getMonth(monthString.get());
            if (month.isValid()) {
                return Optional.of(year.orElse("") + "-" + month.twoDigitNumber);
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

    public void setCommentsBeforeEntry(String parsedComments) {
        this.commentsBeforeEntry = parsedComments;
    }

    public boolean hasChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public Optional<FieldChange> putKeywords(List<String> keywords, Character delimiter) {
        Objects.requireNonNull(delimiter);
        return putKeywords(new KeywordList(keywords), delimiter);
    }

    public Optional<FieldChange> putKeywords(KeywordList keywords, Character delimiter) {
        Objects.requireNonNull(keywords);
        Optional<String> oldValue = this.getField(FieldName.KEYWORDS);

        if (keywords.isEmpty()) {
            // Clear keyword field
            if (oldValue.isPresent()) {
                return this.clearField(FieldName.KEYWORDS);
            } else {
                return Optional.empty();
            }
        }

        // Set new keyword field
        String newValue = keywords.getAsString(delimiter);
        return this.setField(FieldName.KEYWORDS, newValue);
    }

    /**
     * Check if a keyword already exists (case insensitive), if not: add it
     *
     * @param keyword Keyword to add
     */
    public void addKeyword(String keyword, Character delimiter) {
        Objects.requireNonNull(keyword, "keyword must not be null");

        if (keyword.isEmpty()) {
            return;
        }

        addKeyword(new Keyword(keyword), delimiter);
    }

    public void addKeyword(Keyword keyword, Character delimiter) {
        KeywordList keywords = this.getKeywords(delimiter);
        keywords.add(keyword);
        this.putKeywords(keywords, delimiter);
    }

    /**
     * Add multiple keywords to entry
     *
     * @param keywords Keywords to add
     */
    public void addKeywords(Collection<String> keywords, Character delimiter) {
        Objects.requireNonNull(keywords);
        keywords.forEach(keyword -> addKeyword(keyword, delimiter));
    }

    public KeywordList getKeywords(Character delimiter) {
        Optional<String> keywordsContent = getField(FieldName.KEYWORDS);
        if (keywordsContent.isPresent()) {
            return KeywordList.parse(keywordsContent.get(), delimiter);
        } else {
            return new KeywordList();
        }
    }

    public Collection<String> getFieldValues() {
        return fields.values();
    }

    public Map<String, String> getFieldMap() {
        return fields;
    }

    public SharedBibEntryData getSharedBibEntryData() {
        return sharedBibEntryData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        BibEntry entry = (BibEntry) o;
        return Objects.equals(type, entry.type) && Objects.equals(fields, entry.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, fields);
    }

    public void registerListener(Object object) {
        this.eventBus.register(object);
    }

    public void unregisterListener(Object object) {
        this.eventBus.unregister(object);
    }

    public BibEntry withField(String field, String value) {
        setField(field, value);
        return this;
    }

    /*
    * Returns user comments (arbitrary text before the entry), if they exist. If not, returns the empty String
     */
    public String getUserComments() {
        // delete trailing whitespaces (between entry and text) from stored serialization
        return REMOVE_TRAILING_WHITESPACE.matcher(commentsBeforeEntry).replaceFirst("");
    }

    public List<ParsedEntryLink> getEntryLinkList(String fieldName, BibDatabase database) {
        return getField(fieldName).map(fieldValue -> EntryLinkList.parse(fieldValue, database))
                .orElse(Collections.emptyList());
    }

    public Optional<FieldChange> setEntryLinkList(String fieldName, List<ParsedEntryLink> list) {
        return setField(fieldName, EntryLinkList.serialize(list));
    }

    public Set<String> getFieldAsWords(String field) {
        String fieldName = toLowerCase(field);
        Set<String> storedList = fieldsAsWords.get(fieldName);
        if (storedList != null) {
            return storedList;
        } else {
            String fieldValue = fields.get(fieldName);
            if (fieldValue == null) {
                return Collections.emptySet();
            } else {
                HashSet<String> words = new HashSet<>(StringUtil.getStringAsWords(fieldValue));
                fieldsAsWords.put(fieldName, words);
                return words;
            }
        }
    }

    public Optional<FieldChange> clearCiteKey() {
        return clearField(KEY_FIELD);
    }

    private void invalidateFieldCache(String fieldName) {
        latexFreeFields.remove(fieldName);
        fieldsAsWords.remove(fieldName);
    }

    public Optional<String> getLatexFreeField(String name) {
        if (!hasField(name)) {
            return Optional.empty();
        } else if (latexFreeFields.containsKey(name)) {
            return Optional.ofNullable(latexFreeFields.get(toLowerCase(name)));
        } else {
            String latexFreeField = unicodeConverter.format(getField(name).get()).intern();
            latexFreeFields.put(name, latexFreeField);
            return Optional.of(latexFreeField);
        }
    }
}
