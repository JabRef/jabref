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

import org.jabref.model.entry.specialfields.SpecialField;

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
    /**
     * These are the fields JabRef always displays as default
     * {@link org.jabref.preferences.JabRefPreferences#setLanguageDependentDefaultValues()}
     *
     * A user can change them. The change is currently stored in the preferences only and not explicitly exposed as separate preferences object
     */
    public static final List<String> DEFAULT_GENERAL_FIELDS = Arrays.asList(
            FieldName.CROSSREF, FieldName.KEYWORDS, FieldName.FILE, FieldName.DOI, FieldName.URL, FieldName.GROUPS, FieldName.OWNER, FieldName.TIMESTAMP
    );

    // Lists of fields with special properties
    private static final List<String> INTEGER_FIELDS = Arrays.asList(
            FieldName.CTLMAX_NAMES_FORCED_ETAL, FieldName.CTLNAMES_SHOW_ETAL, FieldName.CTLALT_STRETCH_FACTOR, FieldName.VOLUMES, FieldName.PMID
    );

    private static final List<String> YES_NO_FIELDS = Arrays.asList(
            FieldName.CTLUSE_ARTICLE_NUMBER, FieldName.CTLUSE_PAPER, FieldName.CTLUSE_URL, FieldName.CTLUSE_FORCED_ETAL, FieldName.CTLUSE_ALT_SPACING, FieldName.CTLDASH_REPEATED_NAMES
    );

    private static final List<String> DATE_FIELDS = Arrays.asList(
            FieldName.DATE, FieldName.EVENTDATE, FieldName.ORIGDATE, FieldName.URLDATE
    );

    private static final List<String> PERSON_NAME_FIELDS = Arrays.asList(
            FieldName.AUTHOR, FieldName.EDITOR, FieldName.EDITORA, FieldName.EDITORB, FieldName.EDITORC, FieldName.TRANSLATOR, FieldName.ANNOTATOR,
            FieldName.COMMENTATOR, FieldName.INTRODUCTION, FieldName.FOREWORD, FieldName.AFTERWORD,
            FieldName.BOOKAUTHOR, FieldName.HOLDER, FieldName.SHORTAUTHOR, FieldName.SHORTEDITOR, FieldName.SORTNAME,
            FieldName.NAMEADDON, FieldName.ASSIGNEE
    );

    private static final List<String> EDITOR_TYPE_FIELDS = Arrays.asList(
            FieldName.EDITORTYPE, FieldName.EDITORATYPE, FieldName.EDITORBTYPE, FieldName.EDITORCTYPE
    );

    private static final List<String> PAGINATION_FIELDS = Arrays.asList(
            FieldName.PAGINATION, FieldName.BOOKPAGINATION
    );

    private static final List<String> JOURNAL_NAME_FIELDS = Arrays.asList(
            FieldName.JOURNAL, FieldName.JOURNALTITLE, FieldName.JOURNALSUBTITLE
    );

    private static final List<String> BOOK_NAME_FIELDS = Arrays.asList(
            FieldName.BOOKTITLE, FieldName.MAINTITLE, FieldName.MAINSUBTITLE, FieldName.MAINTITLEADDON, FieldName.BOOKSUBTITLE, FieldName.BOOKTITLEADDON
    );

    private static final List<String> LANGUAGE_FIELDS = Arrays.asList(
            FieldName.LANGUAGE, FieldName.ORIGLANGUAGE
    );

    private static final List<String> MULTI_KEY_FIELDS = Arrays.asList(
            FieldName.RELATED, FieldName.ENTRYSET
    );

    private static final List<String> VERBATIM_FIELDS = Arrays.asList(
            FieldName.URL, FieldName.FILE, FieldName.CTLNAME_FORMAT_STRING, FieldName.CTLNAME_LATEX_CMD, FieldName.CTLNAME_URL_PREFIX
    );

    private static final List<String> SPECIAL_FIELDS = Arrays.asList(
            SpecialField.PRINTED.getFieldName(),
            SpecialField.PRIORITY.getFieldName(), SpecialField.QUALITY.getFieldName(),
            SpecialField.RANKING.getFieldName(), SpecialField.READ_STATUS.getFieldName(),
            SpecialField.RELEVANCE.getFieldName()
    );

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
        add(new BibtexSingleField(FieldName.ADDRESS));
        // An annotation. It is not used by the standard bibliography styles,
        // but may be used by others that produce an annotated bibliography.
        // http://www.ecst.csuchico.edu/~jacobsd/bib/formats/bibtex.html
        add(new BibtexSingleField(FieldName.ANNOTE));
        add(new BibtexSingleField(FieldName.AUTHOR, true, 280));
        add(new BibtexSingleField(FieldName.BOOKTITLE, true, 175));
        add(new BibtexSingleField(FieldName.CHAPTER));
        add(new BibtexSingleField(FieldName.CROSSREF).withProperties(FieldProperty.SINGLE_ENTRY_LINK));
        add(new BibtexSingleField(FieldName.EDITION));
        add(new BibtexSingleField(FieldName.EDITOR, true, 280));
        add(new BibtexSingleField(FieldName.EPRINT).withProperties(FieldProperty.EPRINT));
        add(new BibtexSingleField(FieldName.HOWPUBLISHED));
        add(new BibtexSingleField(FieldName.INSTITUTION));
        add(new BibtexSingleField(FieldName.ISBN).withProperties(FieldProperty.ISBN));
        add(new BibtexSingleField(FieldName.ISSN));
        add(new BibtexSingleField(FieldName.JOURNAL).withProperties(FieldProperty.JOURNAL_NAME));
        add(new BibtexSingleField(FieldName.JOURNALTITLE).withProperties(FieldProperty.JOURNAL_NAME));
        add(new BibtexSingleField(FieldName.KEY));
        add(new BibtexSingleField(FieldName.MONTH).withProperties(FieldProperty.MONTH));
        add(new BibtexSingleField(FieldName.MONTHFILED).withProperties(FieldProperty.MONTH));
        add(new BibtexSingleField(FieldName.NOTE));
        add(new BibtexSingleField(FieldName.NUMBER, true, 60).setNumeric());
        add(new BibtexSingleField(FieldName.ORGANIZATION));
        add(new BibtexSingleField(FieldName.PAGES));
        add(new BibtexSingleField(FieldName.PUBLISHER));
        add(new BibtexSingleField(FieldName.SCHOOL));
        add(new BibtexSingleField(FieldName.SERIES));
        add(new BibtexSingleField(FieldName.TITLE, true, 400));
        add(new BibtexSingleField(FieldName.TYPE).withProperties(FieldProperty.TYPE));
        add(new BibtexSingleField(FieldName.LANGUAGE));
        add(new BibtexSingleField(FieldName.VOLUME, true, 60).setNumeric());
        add(new BibtexSingleField(FieldName.YEAR, true, 60).setNumeric());

        // custom fields not displayed at editor, but as columns in the UI
        for (String fieldName : SPECIAL_FIELDS) {
            BibtexSingleField field = new BibtexSingleField(fieldName, false);
            field.setPrivate();
            field.setWriteable(false);
            field.setDisplayable(false);

            add(field);
        }

        // some semi-standard fields
        BibtexSingleField tempField = new BibtexSingleField(BibEntry.KEY_FIELD).withProperties(FieldProperty.KEY);
        tempField.setPrivate();
        add(tempField);

        add(new BibtexSingleField(FieldName.DOI).withProperties(FieldProperty.DOI));
        add(new BibtexSingleField(FieldName.EID));
        add(new BibtexSingleField(FieldName.DATE).withProperties(FieldProperty.DATE));
        add(new BibtexSingleField(FieldName.PMID, false, 60).setNumeric());

        // additional fields
        add(new BibtexSingleField(FieldName.LOCATION, false));
        add(new BibtexSingleField(FieldName.ABSTRACT, false, 400).withProperties(FieldProperty.MULTILINE_TEXT));
        add(new BibtexSingleField(FieldName.URL, false).withProperties(FieldProperty.EXTERNAL, FieldProperty.VERBATIM));
        add(new BibtexSingleField(FieldName.COMMENT, false));
        add(new BibtexSingleField(FieldName.KEYWORDS, false));
        add(new BibtexSingleField(FieldName.FILE, false).withProperties(FieldProperty.FILE_EDITOR, FieldProperty.VERBATIM));
        add(new BibtexSingleField(FieldName.RELATED, false).withProperties(FieldProperty.MULTIPLE_ENTRY_LINK));

        // some biblatex fields
        add(new BibtexSingleField(FieldName.GENDER).withProperties(FieldProperty.GENDER));
        add(new BibtexSingleField(FieldName.PUBSTATE).withProperties(FieldProperty.PUBLICATION_STATE));

        // some internal fields
        tempField = new BibtexSingleField(FieldName.NUMBER_COL, false, 32);
        tempField.setPrivate();
        tempField.setWriteable(false);
        tempField.setDisplayable(false);
        add(tempField);

        tempField = new BibtexSingleField(FieldName.OWNER, false).withProperties(FieldProperty.OWNER);
        tempField.setPrivate();
        add(tempField);

        timeStampField = FieldName.TIMESTAMP;
        tempField = new BibtexSingleField(FieldName.TIMESTAMP, false).withProperties(FieldProperty.DATE);
        tempField.setPrivate();
        add(tempField);

        tempField = new BibtexSingleField(BibEntry.TYPE_HEADER, false, 75);
        tempField.setPrivate();
        add(tempField);

        tempField = new BibtexSingleField(FieldName.SEARCH_INTERNAL, false);
        tempField.setPrivate();
        tempField.setWriteable(false);
        tempField.setDisplayable(false);
        add(tempField);

        tempField = new BibtexSingleField(FieldName.GROUPSEARCH_INTERNAL, false);
        tempField.setPrivate();
        tempField.setWriteable(false);
        tempField.setDisplayable(false);
        add(tempField);

        tempField = new BibtexSingleField(FieldName.MARKED_INTERNAL, false);
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

    public static Set<FieldProperty> getFieldProperties(String name) {
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

    public static int getFieldLength(String name) {
        return InternalBibtexFields.getField(name)
                .map(BibtexSingleField::getLength)
                .orElse(BibtexSingleField.DEFAULT_FIELD_LENGTH);
    }

    /**
     * Returns a sorted list with all field names
     */
    public static List<String> getAllPublicFieldNames() {
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

    public static boolean isDisplayableField(String field) {
        Optional<BibtexSingleField> sField = InternalBibtexFields.getField(field);
        return !sField.isPresent() || sField.get().isDisplayable();
    }

    /**
     * Returns true if the given field is a standard BibTeX field.
     *
     * @param field a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isStandardField(String field) {
        Optional<BibtexSingleField> sField = InternalBibtexFields.getField(field);
        return sField.isPresent() && sField.get().isStandard();
    }

    public static boolean isNumeric(String field) {
        Optional<BibtexSingleField> sField = InternalBibtexFields.getField(field);
        return sField.isPresent() && sField.get().isNumeric();
    }

    public static boolean isInternalField(String field) {
        return field.startsWith("__");
    }

    public static List<String> getJournalNameFields() {
        return getFieldsWithProperty(FieldProperty.JOURNAL_NAME);
    }

    /**
     * Returns a sorted List with all field names including internal field names
     */
    public static List<String> getAllPublicAndInternalFieldNames() {
        //add the internal field names to public fields
        List<String> publicAndInternalFields = new ArrayList<>();
        publicAndInternalFields.addAll(InternalBibtexFields.getAllPublicFieldNames());
        publicAndInternalFields.add(FieldName.INTERNAL_ALL_FIELD);
        publicAndInternalFields.add(FieldName.INTERNAL_ALL_TEXT_FIELDS_FIELD);

        // sort the entries
        Collections.sort(publicAndInternalFields);

        return publicAndInternalFields;
    }

    public static List<String> getBookNameFields() {
        return getFieldsWithProperty(FieldProperty.BOOK_NAME);
    }

    public static List<String> getPersonNameFields() {
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
     * Insert a field into the internal list
     */
    private void add(BibtexSingleField field) {
        fieldSet.put(field.getName(), field);
    }
}
