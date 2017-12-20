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
 *  - handling of identically fields with different names (https://github.com/JabRef/jabref/issues/521)
 *    e.g. LCCN = lib-congress, journaltitle = journal
 *  - group id for each fields, e.g. standard, jurabib, bio, ...
 *  - add a additional properties functionality into the BibtexSingleField class
 */
public class InternalBibtexFields {

    /**
     * These are the fields JabRef always displays as default
     * {@link org.jabref.preferences.JabRefPreferences#setLanguageDependentDefaultValues()}
     *
     * A user can change them. The change is currently stored in the preferences only and not explicitley exposed as separte preferences object
     */
    public static final List<String> DEFAULT_GENERAL_FIELDS = Arrays.asList(FieldName.CROSSREF, FieldName.KEYWORDS, FieldName.FILE, FieldName.DOI, FieldName.URL, FieldName.GROUPS, FieldName.COMMENT, FieldName.OWNER, FieldName.TIMESTAMP);

    // Lists of fields with special properties
    private static final List<String> INTEGER_FIELDS = Arrays.asList(FieldName.CTLMAX_NAMES_FORCED_ETAL,
            FieldName.CTLNAMES_SHOW_ETAL, FieldName.CTLALT_STRETCH_FACTOR, FieldName.VOLUMES, FieldName.PMID);

    private static final List<String> IEEETRANBSTCTL_YES_NO_FIELDS = Arrays.asList(FieldName.CTLUSE_ARTICLE_NUMBER,
            FieldName.CTLUSE_PAPER, FieldName.CTLUSE_URL, FieldName.CTLUSE_FORCED_ETAL, FieldName.CTLUSE_ALT_SPACING,
            FieldName.CTLDASH_REPEATED_NAMES);

    private static final List<String> BIBLATEX_DATE_FIELDS = Arrays.asList(FieldName.DATE, FieldName.EVENTDATE,
            FieldName.ORIGDATE, FieldName.URLDATE);

    private static final List<String> BIBLATEX_PERSON_NAME_FIELDS = Arrays.asList(FieldName.AUTHOR, FieldName.EDITOR,
            FieldName.EDITORA, FieldName.EDITORB, FieldName.EDITORC, FieldName.TRANSLATOR, FieldName.ANNOTATOR,
            FieldName.COMMENTATOR, FieldName.INTRODUCTION, FieldName.FOREWORD, FieldName.AFTERWORD,
            FieldName.BOOKAUTHOR, FieldName.HOLDER, FieldName.SHORTAUTHOR, FieldName.SHORTEDITOR, FieldName.SORTNAME,
            FieldName.NAMEADDON, FieldName.ASSIGNEE);

    private static final List<String> BIBLATEX_EDITOR_TYPE_FIELDS = Arrays.asList(FieldName.EDITORTYPE,
            FieldName.EDITORATYPE, FieldName.EDITORBTYPE, FieldName.EDITORCTYPE);

    private static final List<String> BIBLATEX_PAGINATION_FIELDS = Arrays.asList(FieldName.PAGINATION,
            FieldName.BOOKPAGINATION);

    private static final List<String> BIBLATEX_JOURNAL_NAME_FIELDS = Arrays.asList(FieldName.JOURNAL,
            FieldName.JOURNALTITLE, FieldName.JOURNALSUBTITLE);

    private static final List<String> BIBLATEX_BOOK_NAME_FIELDS = Arrays.asList(FieldName.BOOKTITLE,
            FieldName.MAINTITLE, FieldName.MAINSUBTITLE, FieldName.MAINTITLEADDON, FieldName.BOOKSUBTITLE,
            FieldName.BOOKTITLEADDON);

    private static final List<String> BIBLATEX_LANGUAGE_FIELDS = Arrays.asList(FieldName.LANGUAGE,
            FieldName.ORIGLANGUAGE);

    private static final List<String> BIBLATEX_MULTI_KEY_FIELDS = Arrays.asList(FieldName.RELATED, FieldName.ENTRYSET);

    private static final List<String> VERBATIM_FIELDS = Arrays.asList(FieldName.URL, FieldName.FILE,
            FieldName.CTLNAME_FORMAT_STRING, FieldName.CTLNAME_LATEX_CMD, FieldName.CTLNAME_URL_PREFIX);

    private static final List<String> SPECIAL_FIELDS = Arrays.asList(SpecialField.PRINTED.getFieldName(),
            SpecialField.PRIORITY.getFieldName(), SpecialField.QUALITY.getFieldName(),
            SpecialField.RANKING.getFieldName(), SpecialField.READ_STATUS.getFieldName(),
            SpecialField.RELEVANCE.getFieldName());

    // singleton instance
    private static InternalBibtexFields RUNTIME = new InternalBibtexFields();

    // contains all bibtex-field objects (BibtexSingleField)
    private final Map<String, BibtexSingleField> fieldSet;

    // the name with the current time stamp field, needed in case we want to change it
    private String timeStampField;


    private InternalBibtexFields() {
        fieldSet = new HashMap<>();
        BibtexSingleField dummy;

        // FIRST: all standard fields
        // These are the fields that BibTeX might want to treat, so these
        // must conform to BibTeX rules.
        add(new BibtexSingleField(FieldName.ADDRESS, true));
        // An annotation. It is not used by the standard bibliography styles,
        // but may be used by others that produce an annotated bibliography.
        // http://www.ecst.csuchico.edu/~jacobsd/bib/formats/bibtex.html
        add(new BibtexSingleField(FieldName.ANNOTE, true));
        add(new BibtexSingleField(FieldName.AUTHOR, true, 280));
        add(new BibtexSingleField(FieldName.BOOKTITLE, true, 175));
        add(new BibtexSingleField(FieldName.CHAPTER, true));
        dummy = new BibtexSingleField(FieldName.CROSSREF, true).withProperties(FieldProperty.SINGLE_ENTRY_LINK);
        add(dummy);
        add(new BibtexSingleField(FieldName.EDITION, true));
        add(new BibtexSingleField(FieldName.EDITOR, true, 280));
        dummy = new BibtexSingleField(FieldName.EPRINT, true).withProperties(FieldProperty.EPRINT);
        add(dummy);
        add(new BibtexSingleField(FieldName.HOWPUBLISHED, true));
        add(new BibtexSingleField(FieldName.INSTITUTION, true));

        dummy = new BibtexSingleField(FieldName.ISBN, true).withProperties(FieldProperty.ISBN);
        add(dummy);

        add(new BibtexSingleField(FieldName.ISSN, true));

        dummy = new BibtexSingleField(FieldName.JOURNAL, true).withProperties(FieldProperty.JOURNAL_NAME);
        add(dummy);
        dummy = new BibtexSingleField(FieldName.JOURNALTITLE, true).withProperties(FieldProperty.JOURNAL_NAME);
        add(dummy);

        add(new BibtexSingleField(FieldName.KEY, true));
        dummy = new BibtexSingleField(FieldName.MONTH, true).withProperties(FieldProperty.MONTH);
        add(dummy);
        add(new BibtexSingleField(FieldName.NOTE, true));
        add(new BibtexSingleField(FieldName.NUMBER, true, 60).setNumeric(true));
        add(new BibtexSingleField(FieldName.ORGANIZATION, true));
        add(new BibtexSingleField(FieldName.PAGES, true));
        add(new BibtexSingleField(FieldName.PUBLISHER, true));
        add(new BibtexSingleField(FieldName.SCHOOL, true));
        add(new BibtexSingleField(FieldName.SERIES, true));
        add(new BibtexSingleField(FieldName.TITLE, true, 400));
        dummy = new BibtexSingleField(FieldName.TYPE, true).withProperties(FieldProperty.TYPE);
        add(dummy);
        add(new BibtexSingleField(FieldName.LANGUAGE, true));
        add(new BibtexSingleField(FieldName.VOLUME, true, 60).setNumeric(true));
        add(new BibtexSingleField(FieldName.YEAR, true, 60).setNumeric(true));

        // custom fields not displayed at editor, but as columns in the UI
        for (String fieldName : SPECIAL_FIELDS) {
            dummy = new BibtexSingleField(fieldName, false);

            dummy.setPrivate();
            dummy.setWriteable(false);
            dummy.setDisplayable(false);

            add(dummy);
        }

        // some semi-standard fields
        dummy = new BibtexSingleField(BibEntry.KEY_FIELD, true);
        dummy.setPrivate();
        add(dummy);

        dummy = new BibtexSingleField(FieldName.DOI, true).withProperties(FieldProperty.DOI);
        add(dummy);
        add(new BibtexSingleField(FieldName.EID, true));

        dummy = new BibtexSingleField(FieldName.DATE, true).withProperties(FieldProperty.DATE);
        add(dummy);

        add(new BibtexSingleField(FieldName.PMID, false, 60).setNumeric(true));

        // additional fields ------------------------------------------------------
        add(new BibtexSingleField(FieldName.LOCATION, false));
        add(new BibtexSingleField(FieldName.ABSTRACT, false, 400).withProperties(FieldProperty.MULTILINE_TEXT));

        dummy = new BibtexSingleField(FieldName.URL, false).withProperties(FieldProperty.EXTERNAL, FieldProperty.VERBATIM);
        add(dummy);

        add(new BibtexSingleField(FieldName.COMMENT, false));
        add(new BibtexSingleField(FieldName.KEYWORDS, false));

        dummy = new BibtexSingleField(FieldName.FILE, false).withProperties(FieldProperty.FILE_EDITOR, FieldProperty.VERBATIM);
        add(dummy);

        dummy = new BibtexSingleField(FieldName.RELATED, false).withProperties(FieldProperty.MULTIPLE_ENTRY_LINK);
        add(dummy);

        // some biblatex fields
        dummy = new BibtexSingleField(FieldName.GENDER, true).withProperties(FieldProperty.GENDER);
        add(dummy);

        dummy = new BibtexSingleField(FieldName.PUBSTATE, true).withProperties(FieldProperty.PUBLICATION_STATE);
        add(dummy);

        // some internal fields ----------------------------------------------
        dummy = new BibtexSingleField(FieldName.NUMBER_COL, false, 32);
        dummy.setPrivate();
        dummy.setWriteable(false);
        dummy.setDisplayable(false);
        add(dummy);

        dummy = new BibtexSingleField(FieldName.OWNER, false).withProperties(FieldProperty.OWNER);
        dummy.setPrivate();
        add(dummy);

        timeStampField = FieldName.TIMESTAMP;
        dummy = new BibtexSingleField(FieldName.TIMESTAMP, false).withProperties(FieldProperty.DATE);
        dummy.setPrivate();
        add(dummy);

        dummy = new BibtexSingleField(BibEntry.TYPE_HEADER, false, 75);
        dummy.setPrivate();
        add(dummy);

        dummy = new BibtexSingleField(FieldName.SEARCH_INTERNAL, false);
        dummy.setPrivate();
        dummy.setWriteable(false);
        dummy.setDisplayable(false);
        add(dummy);

        dummy = new BibtexSingleField(FieldName.GROUPSEARCH_INTERNAL, false);
        dummy.setPrivate();
        dummy.setWriteable(false);
        dummy.setDisplayable(false);
        add(dummy);

        dummy = new BibtexSingleField(FieldName.MARKED_INTERNAL, false);
        dummy.setPrivate();
        dummy.setWriteable(true); // This field must be written to file!
        dummy.setDisplayable(false);
        add(dummy);

        // IEEEtranBSTCTL fields that should be "yes" or "no"
        for (String yesNoField : IEEETRANBSTCTL_YES_NO_FIELDS) {
            dummy = new BibtexSingleField(yesNoField, false).withProperties(FieldProperty.YES_NO);
            add(dummy);
        }

        // Fields that should be an integer value
        for (String numericField : INTEGER_FIELDS) {
            BibtexSingleField field = fieldSet.get(numericField);
            if (field == null) {
                field = new BibtexSingleField(numericField, true).setNumeric(true);
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
        for (String fieldText : BIBLATEX_PERSON_NAME_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true);
            }
            field.getProperties().add(FieldProperty.PERSON_NAMES);
            add(field);
        }

        // Set all fields which should contain editor types
        for (String fieldText : BIBLATEX_EDITOR_TYPE_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true);
            }
            field.getProperties().add(FieldProperty.EDITOR_TYPE);
            add(field);
        }

        // Set all fields which are pagination fields
        for (String fieldText : BIBLATEX_PAGINATION_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true);
            }
            field.getProperties().add(FieldProperty.PAGINATION);
            add(field);
        }

        // Set all fields with dates
        for (String fieldText : BIBLATEX_DATE_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true);
            }
            field.getProperties().add(FieldProperty.DATE);
            field.getProperties().add(FieldProperty.ISO_DATE);
            add(field);
        }

        // Set all fields with journal names
        for (String fieldText : BIBLATEX_JOURNAL_NAME_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true);
            }
            field.getProperties().add(FieldProperty.JOURNAL_NAME);
            add(field);
        }

        // Set all fields with book names
        for (String fieldText : BIBLATEX_BOOK_NAME_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true);
            }
            field.getProperties().add(FieldProperty.BOOK_NAME);
            add(field);
        }

        // Set all fields containing a language
        for (String fieldText : BIBLATEX_LANGUAGE_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true);
            }
            field.getProperties().add(FieldProperty.LANGUAGE);
            add(field);
        }

        // Set all fields with multiple key links
        for (String fieldText : BIBLATEX_MULTI_KEY_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true);
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
        // Build a Set of field names for the fields that should be sorted numerically:
        Set<String> nF = new HashSet<>();
        nF.addAll(numFields);
        // Look through all registered fields, and activate numeric sorting if necessary:
        for (String fieldName : InternalBibtexFields.RUNTIME.fieldSet.keySet()) {
            BibtexSingleField field = InternalBibtexFields.RUNTIME.fieldSet.get(fieldName);
            if (!field.isNumeric() && nF.contains(fieldName)) {
                field.setNumeric(true);
            }
            nF.remove(fieldName); // remove, so we clear the set of all standard fields.
        }
        // If there are fields left in nF, these must be non-standard fields. Add descriptors for them:
        for (String fieldName : nF) {
            BibtexSingleField field = new BibtexSingleField(fieldName, false);
            field.setNumeric(true);
            InternalBibtexFields.RUNTIME.fieldSet.put(fieldName, field);
        }

    }

    public static Set<FieldProperty> getFieldProperties(String name) {
        return getField(name)
                .map(BibtexSingleField::getProperties)
                .orElse(EnumSet.noneOf(FieldProperty.class));
    }

    // --------------------------------------------------------------------------
    //  the "static area"
    // --------------------------------------------------------------------------
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
     * returns a List with all fieldnames
     */
    public static List<String> getAllPublicFieldNames() {
        // collect all public fields
        List<String> publicFields = new ArrayList<>();
        for (BibtexSingleField sField : InternalBibtexFields.RUNTIME.fieldSet.values()) {
            if (!sField.isPrivate()) {
                publicFields.add(sField.getName());
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
     * Returns true if the given field is a standard Bibtex field.
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
     * returns a List with all fieldnames incl. internal fieldnames
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
        return IEEETRANBSTCTL_YES_NO_FIELDS;
    }

    /**
     * insert a field into the internal list
     */
    private void add(BibtexSingleField field) {
        String key = field.getName();
        fieldSet.put(key, field);
    }
}
