package org.jabref.model.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.IEEEField;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;

/**
 * Handling of bibtex fields.
 * All bibtex-field related stuff should be placed here!
 * Because we can export this information into additional
 * config files -> simple extension and definition of new fields
 *
 * TODO:
 * - handling of identically fields with different names (https://github.com/JabRef/jabref/issues/521)
 * e.g. LCCN = lib-congress, journaltitle = journal
 * - group id for each fields, e.g. standard, jurabib, bio, ...
 * - add a additional properties functionality into the BibtexSingleField class
 */
public class InternalBibtexFields {
    private static final List<String> DEFAULT_GENERAL_FIELDS = Arrays.asList(
            StandardField.CROSSREF, StandardField.KEYWORDS, StandardField.FILE, InternalField.GROUPS, InternalField.OWNER, InternalField.TIMESTAMP
    );

    // Lists of fields with special properties
    private static final List<String> INTEGER_FIELDS = Arrays.asList(
            IEEEField.CTLMAX_NAMES_FORCED_ETAL, IEEEField.CTLNAMES_SHOW_ETAL, IEEEField.CTLALT_STRETCH_FACTOR, StandardField.VOLUMES, StandardField.PMID
    );

    private static final List<String> YES_NO_FIELDS = Arrays.asList(
            IEEEField.CTLUSE_ARTICLE_NUMBER, IEEEField.CTLUSE_PAPER, IEEEField.CTLUSE_URL, IEEEField.CTLUSE_FORCED_ETAL, IEEEField.CTLUSE_ALT_SPACING, IEEEField.CTLDASH_REPEATED_NAMES
    );

    private static final List<String> DATE_FIELDS = Arrays.asList(
            StandardField.DATE, StandardField.EVENTDATE, StandardField.ORIGDATE, StandardField.URLDATE
    );

    private static final List<String> PERSON_NAME_FIELDS = Arrays.asList(
            StandardField.AUTHOR, StandardField.EDITOR, StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC, StandardField.TRANSLATOR, StandardField.ANNOTATOR,
            StandardField.COMMENTATOR, StandardField.INTRODUCTION, StandardField.FOREWORD, StandardField.AFTERWORD,
            StandardField.BOOKAUTHOR, StandardField.HOLDER, StandardField.SHORTAUTHOR, StandardField.SHORTEDITOR, StandardField.SORTNAME,
            StandardField.NAMEADDON, StandardField.ASSIGNEE
    );

    private static final List<String> EDITOR_TYPE_FIELDS = Arrays.asList(
            StandardField.EDITORTYPE, StandardField.EDITORATYPE, StandardField.EDITORBTYPE, StandardField.EDITORCTYPE
    );

    private static final List<String> PAGINATION_FIELDS = Arrays.asList(
            StandardField.PAGINATION, StandardField.BOOKPAGINATION
    );

    private static final List<String> JOURNAL_NAME_FIELDS = Arrays.asList(
            StandardField.JOURNAL, StandardField.JOURNALTITLE, StandardField.JOURNALSUBTITLE
    );

    private static final List<String> BOOK_NAME_FIELDS = Arrays.asList(
            StandardField.BOOKTITLE, StandardField.MAINTITLE, StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.BOOKSUBTITLE, StandardField.BOOKTITLEADDON
    );

    private static final List<String> LANGUAGE_FIELDS = Arrays.asList(
            StandardField.LANGUAGE, StandardField.ORIGLANGUAGE
    );

    private static final List<String> MULTI_KEY_FIELDS = Arrays.asList(
            StandardField.RELATED, StandardField.ENTRYSET
    );

    private static final List<String> VERBATIM_FIELDS = Arrays.asList(
            StandardField.URL, StandardField.FILE, IEEEField.CTLNAME_FORMAT_STRING, IEEEField.CTLNAME_LATEX_CMD, IEEEField.CTLNAME_URL_PREFIX
    );

    private static final List<String> SPECIAL_FIELDS = Arrays.asList(
            SpecialField.PRINTED.getName(),
            SpecialField.PRIORITY.getName(),
            SpecialField.QUALITY.getName(),
            SpecialField.RANKING.getName(),
            SpecialField.READ_STATUS.getName(),
            SpecialField.RELEVANCE.getName()
    );

    private static final Set<String> MULTILINE_FIELDS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(StandardField.NOTE, StandardField.ABSTRACT, StandardField.COMMENT)
    ));

    // singleton instance
    private static InternalBibtexFields RUNTIME = new InternalBibtexFields();

    // contains all bibtex-field objects (BibtexSingleField)
    private final Map<String, BibtexSingleField> fieldSet;

    // the name with the current time stamp field, needed in case we want to change it
    private String timeStampField;

    private InternalBibtexFields() {
        fieldSet = new HashMap<>();

        // FIRST: all standard fields
        // These are the fields that BibTeX might want to treat, so these must conform to BibTeX rules.
        add(new BibtexSingleField(StandardField.ADDRESS));
        // An annotation. It is not used by the standard bibliography styles,
        // but may be used by others that produce an annotated bibliography.
        // http://www.ecst.csuchico.edu/~jacobsd/bib/formats/bibtex.html
        add(new BibtexSingleField(StandardField.ANNOTE));
        add(new BibtexSingleField(StandardField.AUTHOR, true, 280));
        add(new BibtexSingleField(StandardField.BOOKTITLE, true, 175));
        add(new BibtexSingleField(StandardField.CHAPTER));
        add(new BibtexSingleField(StandardField.CROSSREF).withProperties(FieldProperty.SINGLE_ENTRY_LINK));
        add(new BibtexSingleField(StandardField.EDITION));
        add(new BibtexSingleField(StandardField.EDITOR, true, 280));
        add(new BibtexSingleField(StandardField.EPRINT).withProperties(FieldProperty.EPRINT));
        add(new BibtexSingleField(StandardField.HOWPUBLISHED));
        add(new BibtexSingleField(StandardField.INSTITUTION));
        add(new BibtexSingleField(StandardField.ISBN).withProperties(FieldProperty.ISBN));
        add(new BibtexSingleField(StandardField.ISSN));
        add(new BibtexSingleField(StandardField.JOURNAL).withProperties(FieldProperty.JOURNAL_NAME));
        add(new BibtexSingleField(StandardField.JOURNALTITLE).withProperties(FieldProperty.JOURNAL_NAME));
        add(new BibtexSingleField(StandardField.KEY));
        add(new BibtexSingleField(StandardField.MONTH).withProperties(FieldProperty.MONTH));
        add(new BibtexSingleField(StandardField.MONTHFILED).withProperties(FieldProperty.MONTH));
        add(new BibtexSingleField(StandardField.NOTE));
        add(new BibtexSingleField(StandardField.NUMBER, true, 60).setNumeric());
        add(new BibtexSingleField(StandardField.ORGANIZATION));
        add(new BibtexSingleField(StandardField.PAGES));
        add(new BibtexSingleField(StandardField.PUBLISHER));
        add(new BibtexSingleField(StandardField.SCHOOL));
        add(new BibtexSingleField(StandardField.SERIES));
        add(new BibtexSingleField(StandardField.TITLE, true, 400));
        add(new BibtexSingleField(StandardField.TYPE).withProperties(FieldProperty.TYPE));
        add(new BibtexSingleField(StandardField.LANGUAGE));
        add(new BibtexSingleField(StandardField.VOLUME, true, 60).setNumeric());
        add(new BibtexSingleField(StandardField.YEAR, true, 60).setNumeric());

        // custom fields not displayed at editor, but as columns in the UI
        for (String fieldName : SPECIAL_FIELDS) {
            BibtexSingleField field = new BibtexSingleField(fieldName, false);
            field.setPrivate();
            field.setWriteable(false);
            field.setDisplayable(false);

            add(field);
        }

        // some semi-standard fields
        BibtexSingleField tempField = new BibtexSingleField(InternalField.KEY_FIELD).withProperties(FieldProperty.KEY);
        tempField.setPrivate();
        add(tempField);

        add(new BibtexSingleField(StandardField.DOI).withProperties(FieldProperty.DOI));
        add(new BibtexSingleField(StandardField.EID));
        add(new BibtexSingleField(StandardField.DATE).withProperties(FieldProperty.DATE));
        add(new BibtexSingleField(StandardField.PMID, false, 60).setNumeric());

        // additional fields
        add(new BibtexSingleField(StandardField.LOCATION, false));
        add(new BibtexSingleField(StandardField.ABSTRACT, false, 400).withProperties(FieldProperty.MULTILINE_TEXT));
        add(new BibtexSingleField(StandardField.URL, false).withProperties(FieldProperty.EXTERNAL, FieldProperty.VERBATIM));
        add(new BibtexSingleField(StandardField.COMMENT, false));
        add(new BibtexSingleField(StandardField.KEYWORDS, false));
        add(new BibtexSingleField(StandardField.FILE, false).withProperties(FieldProperty.FILE_EDITOR, FieldProperty.VERBATIM));
        add(new BibtexSingleField(StandardField.RELATED, false).withProperties(FieldProperty.MULTIPLE_ENTRY_LINK));

        // some biblatex fields
        add(new BibtexSingleField(StandardField.GENDER).withProperties(FieldProperty.GENDER));
        add(new BibtexSingleField(StandardField.PUBSTATE).withProperties(FieldProperty.PUBLICATION_STATE));

        // some internal fields
        tempField = new BibtexSingleField(InternalField.OWNER, false).withProperties(FieldProperty.OWNER);
        tempField.setPrivate();
        add(tempField);

        timeStampField = InternalField.TIMESTAMP;
        tempField = new BibtexSingleField(InternalField.TIMESTAMP, false).withProperties(FieldProperty.DATE);
        tempField.setPrivate();
        add(tempField);

        tempField = new BibtexSingleField(InternalField.TYPE_HEADER, false, 75);
        tempField.setPrivate();
        add(tempField);

        tempField = new BibtexSingleField(InternalField.SEARCH_INTERNAL, false);
        tempField.setPrivate();
        tempField.setWriteable(false);
        tempField.setDisplayable(false);
        add(tempField);

        tempField = new BibtexSingleField(InternalField.GROUPSEARCH_INTERNAL, false);
        tempField.setPrivate();
        tempField.setWriteable(false);
        tempField.setDisplayable(false);
        add(tempField);

        tempField = new BibtexSingleField(InternalField.MARKED_INTERNAL, false);
        tempField.setPrivate();
        tempField.setWriteable(true); // This field must be written to file!
        tempField.setDisplayable(false);
        add(tempField);

        // IEEEtranBSTCTL fields that should be "yes" or "no"
        for (String yesNoField : YES_NO_FIELDS) {
            BibtexSingleField field = new BibtexSingleField(yesNoField, false).withProperties(FieldProperty.YES_NO);
            add(field);
        }

        // Fields that should be an integer value
        for (String numericField : INTEGER_FIELDS) {
            BibtexSingleField field = fieldSet.get(numericField);
            if (field == null) {
                field = new BibtexSingleField(numericField, true).setNumeric();
            }
            field.getProperties().add(FieldProperty.INTEGER);
            add(field);
        }

        // Fields that should be treated as verbatim, so no formatting requirements
        for (String fieldText : VERBATIM_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true);
            }
            field.getProperties().add(FieldProperty.VERBATIM);
            add(field);
        }

        // Set all fields with person names
        for (String fieldText : PERSON_NAME_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true);
            }
            field.getProperties().add(FieldProperty.PERSON_NAMES);
            add(field);
        }

        // Set all fields which should contain editor types
        for (String fieldText : EDITOR_TYPE_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true);
            }
            field.getProperties().add(FieldProperty.EDITOR_TYPE);
            add(field);
        }

        // Set all fields which are pagination fields
        for (String fieldText : PAGINATION_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true);
            }
            field.getProperties().add(FieldProperty.PAGINATION);
            add(field);
        }

        // Set all fields with dates
        for (String fieldText : DATE_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText);
            }
            field.getProperties().add(FieldProperty.DATE);
            field.getProperties().add(FieldProperty.ISO_DATE);
            add(field);
        }

        // Set all fields with journal names
        for (String fieldText : JOURNAL_NAME_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText);
            }
            field.getProperties().add(FieldProperty.JOURNAL_NAME);
            add(field);
        }

        // Set all fields with book names
        for (String fieldText : BOOK_NAME_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText);
            }
            field.getProperties().add(FieldProperty.BOOK_NAME);
            add(field);
        }

        // Set all fields containing a language
        for (String fieldText : LANGUAGE_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText);
            }
            field.getProperties().add(FieldProperty.LANGUAGE);
            add(field);
        }

        // Set all fields with multiple key links
        for (String fieldText : MULTI_KEY_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText);
            }
            field.getProperties().add(FieldProperty.MULTIPLE_ENTRY_LINK);
            add(field);
        }
    }

    public static void updateTimeStampField(String timeStampFieldName) {
        getField(RUNTIME.timeStampField).ifPresent(field -> {
            field.setName(timeStampFieldName);
            RUNTIME.timeStampField = timeStampFieldName;
        });
    }

    public static void updateSpecialFields(boolean serializeSpecialFields) {
        for (String fieldName : SPECIAL_FIELDS) {
            getField(fieldName).ifPresent(field -> {
                if (serializeSpecialFields) {
                    field.setPublic();
                } else {
                    field.setPrivate();
                }
                field.setWriteable(serializeSpecialFields);
                field.setDisplayable(serializeSpecialFields);
            });
        }
    }

    /**
     * Read the "numericFields" string array from preferences, and activate numeric
     * sorting for all fields listed in the array. If an unknown field name is included,
     * add a field descriptor for the new field.
     */
    public static void setNumericFields(List<String> numFields) {
        if (numFields.isEmpty()) {
            return;
        }
        // Build a Set of field names for the fields that should be sorted numerically
        Set<String> names = new HashSet<>();
        names.addAll(numFields);
        // Look through all registered fields, and activate numeric sorting if necessary
        for (String fieldName : InternalBibtexFields.RUNTIME.fieldSet.keySet()) {
            BibtexSingleField field = InternalBibtexFields.RUNTIME.fieldSet.get(fieldName);
            if (!field.isNumeric() && names.contains(fieldName)) {
                field.setNumeric();
            }
            names.remove(fieldName); // remove, so we clear the set of all standard fields.
        }
        // If there are fields left in names, these must be non-standard fields. Add descriptors for them
        for (String fieldName : names) {
            BibtexSingleField field = new BibtexSingleField(fieldName, false);
            field.setNumeric();
            InternalBibtexFields.RUNTIME.fieldSet.put(fieldName, field);
        }
    }

    public static Set<FieldProperty> getFieldProperties(Field name) {
        return getField(name)
                .map(BibtexSingleField::getProperties)
                .orElse(EnumSet.noneOf(FieldProperty.class));
    }

    private static Optional<BibtexSingleField> getField(String name) {
        if (name != null) {
            return Optional.ofNullable(InternalBibtexFields.RUNTIME.fieldSet.get(name.toLowerCase(Locale.ENGLISH)));
        }
        return Optional.empty();
    }

    public static Double getFieldLength(String name) {
        return InternalBibtexFields.getField(name)
                .map(BibtexSingleField::getLength)
                .orElse(BibtexSingleField.DEFAULT_FIELD_LENGTH);
    }

    /**
     * Returns a sorted list with all field names
     */
    public static List<Field> getAllPublicFieldNames() {
        List<String> publicFields = new ArrayList<>();
        for (BibtexSingleField field : InternalBibtexFields.RUNTIME.fieldSet.values()) {
            if (!field.isPrivate()) {
                publicFields.add(field.getName());
            }
        }
        // sort the entries
        Collections.sort(publicFields);

        return publicFields;
    }

    public static boolean isWriteableField(String field) {
        Optional<BibtexSingleField> sField = InternalBibtexFields.getField(field);
        return !sField.isPresent() || sField.get().isWriteable();
    }

    public static boolean isDisplayableField(Field field) {
        Optional<BibtexSingleField> sField = InternalBibtexFields.getField(field);
        return !sField.isPresent() || sField.get().isDisplayable();
    }

    /**
     * Returns true if the given field is a standard BibTeX field.
     *
     * @param field a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isStandardField(Field field) {
        Optional<BibtexSingleField> sField = InternalBibtexFields.getField(field);
        return sField.isPresent() && sField.get().isStandard();
    }

    public static boolean isNumeric(String field) {
        Optional<BibtexSingleField> sField = InternalBibtexFields.getField(field);
        return sField.isPresent() && sField.get().isNumeric();
    }

    public static boolean isInternalField(Field field) {
        return field.getName().startsWith("__");
    }

    public static List<Field> getJournalNameFields() {
        return getFieldsWithProperty(FieldProperty.JOURNAL_NAME);
    }

    /**
     * Returns a sorted List with all field names including internal field names
     */
    public static List<Field> getAllPublicAndInternalFieldNames() {
        //add the internal field names to public fields
        List<String> publicAndInternalFields = new ArrayList<>();
        publicAndInternalFields.addAll(InternalBibtexFields.getAllPublicFieldNames());
        publicAndInternalFields.add(InternalField.INTERNAL_ALL_FIELD);
        publicAndInternalFields.add(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD);

        // sort the entries
        Collections.sort(publicAndInternalFields);

        return publicAndInternalFields;
    }

    public static List<Field> getBookNameFields() {
        return getFieldsWithProperty(FieldProperty.BOOK_NAME);
    }

    public static List<Field> getPersonNameFields() {
        return getFieldsWithProperty(FieldProperty.PERSON_NAMES);
    }

    public static List<String> getFieldsWithProperty(FieldProperty property) {
        return RUNTIME.fieldSet.values().stream()
                .filter(field -> !field.isPrivate())
                .filter(field -> field.getProperties().contains(property))
                .map(BibtexSingleField::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<String> getIEEETranBSTctlYesNoFields() {
        return YES_NO_FIELDS;
    }

    /**
     * These are the fields JabRef always displays as default {@link org.jabref.preferences.JabRefPreferences#setLanguageDependentDefaultValues()}
     *
     * A user can change them. The change is currently stored in the preferences only and not explicitly exposed as
     * separate preferences object
     */
    public static List<String> getDefaultGeneralFields() {
        List<String> defaultGeneralFields = new ArrayList<>(DEFAULT_GENERAL_FIELDS);
        defaultGeneralFields.addAll(SPECIAL_FIELDS);
        return defaultGeneralFields;
    }

    /**
     * Insert a field into the internal list
     */
    private void add(BibtexSingleField field) {
        fieldSet.put(field.getName(), field);
    }

    public static boolean isSingleLineField(final Field fieldName) {
        return !MULTILINE_FIELDS.contains(fieldName.toLowerCase());
    }
}
