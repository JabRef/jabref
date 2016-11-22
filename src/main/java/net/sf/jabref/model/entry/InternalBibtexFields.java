package net.sf.jabref.model.entry;

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

import net.sf.jabref.model.entry.specialfields.SpecialField;

/**
 * Handling of bibtex fields.
 * All bibtex-field related stuff should be placed here!
 * Because we can export these informations into additional
 * config files -> simple extension and definition of new fields....
 *
 * TODO:
 *  - handling of identically fields with different names
 *    e.g. LCCN = lib-congress
 *  - group id for each fields, e.g. standard, jurabib, bio....
 *  - add a additional properties functionality into the BibtexSingleField class
 */
public class InternalBibtexFields {

    // contains all bibtex-field objects (BibtexSingleField)
    private final Map<String, BibtexSingleField> fieldSet;

    // the name with the current time stamp field, needed in case we want to change it
    private String timeStampField;

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
    private static InternalBibtexFields RUNTIME = new InternalBibtexFields(FieldName.TIMESTAMP);


    private InternalBibtexFields(String timeStampFieldName) {
        fieldSet = new HashMap<>();
        BibtexSingleField dummy;

        // FIRST: all standard fields
        // These are the fields that BibTex might want to treat, so these
        // must conform to BibTex rules.
        add(new BibtexSingleField(FieldName.ADDRESS, true, BibtexSingleField.SMALL_W));
        // An annotation. It is not used by the standard bibliography styles,
        // but may be used by others that produce an annotated bibliography.
        // http://www.ecst.csuchico.edu/~jacobsd/bib/formats/bibtex.html
        add(new BibtexSingleField(FieldName.ANNOTE, true, BibtexSingleField.LARGE_W));
        add(new BibtexSingleField(FieldName.AUTHOR, true, BibtexSingleField.MEDIUM_W, 280));
        add(new BibtexSingleField(FieldName.BOOKTITLE, true, 175));
        add(new BibtexSingleField(FieldName.CHAPTER, true, BibtexSingleField.SMALL_W));
        dummy = new BibtexSingleField(FieldName.CROSSREF, true, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperty.CROSSREF, FieldProperty.SINGLE_ENTRY_LINK));
        add(dummy);
        add(new BibtexSingleField(FieldName.EDITION, true, BibtexSingleField.SMALL_W));
        add(new BibtexSingleField(FieldName.EDITOR, true, BibtexSingleField.MEDIUM_W, 280));
        dummy = new BibtexSingleField(FieldName.EPRINT, true, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperty.EPRINT));
        add(dummy);
        add(new BibtexSingleField(FieldName.HOWPUBLISHED, true, BibtexSingleField.MEDIUM_W));
        add(new BibtexSingleField(FieldName.INSTITUTION, true, BibtexSingleField.MEDIUM_W));

        dummy = new BibtexSingleField(FieldName.ISBN, true, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperty.ISBN));
        add(dummy);

        add(new BibtexSingleField(FieldName.ISSN, true, BibtexSingleField.SMALL_W));

        dummy = new BibtexSingleField(FieldName.JOURNAL, true, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperty.JOURNAL_NAME));
        add(dummy);
        dummy = new BibtexSingleField(FieldName.JOURNALTITLE, true, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperty.JOURNAL_NAME));
        add(dummy);

        add(new BibtexSingleField(FieldName.KEY, true));
        dummy = new BibtexSingleField(FieldName.MONTH, true, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperty.MONTH));
        add(dummy);
        add(new BibtexSingleField(FieldName.NOTE, true, BibtexSingleField.MEDIUM_W));
        add(new BibtexSingleField(FieldName.NUMBER, true, BibtexSingleField.SMALL_W, 60).setNumeric(true));
        add(new BibtexSingleField(FieldName.ORGANIZATION, true, BibtexSingleField.MEDIUM_W));
        add(new BibtexSingleField(FieldName.PAGES, true, BibtexSingleField.SMALL_W));
        add(new BibtexSingleField(FieldName.PUBLISHER, true, BibtexSingleField.MEDIUM_W));
        add(new BibtexSingleField(FieldName.SCHOOL, true, BibtexSingleField.MEDIUM_W));
        add(new BibtexSingleField(FieldName.SERIES, true, BibtexSingleField.SMALL_W));
        add(new BibtexSingleField(FieldName.TITLE, true, 400));
        dummy = new BibtexSingleField(FieldName.TYPE, true, BibtexSingleField.SMALL_W);
        dummy.getFieldProperties().add(FieldProperty.TYPE);
        add(dummy);
        add(new BibtexSingleField(FieldName.LANGUAGE, true, BibtexSingleField.SMALL_W));
        add(new BibtexSingleField(FieldName.VOLUME, true, BibtexSingleField.SMALL_W, 60).setNumeric(true));
        add(new BibtexSingleField(FieldName.YEAR, true, BibtexSingleField.SMALL_W, 60).setNumeric(true));

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

        dummy = new BibtexSingleField(FieldName.DOI, true, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperty.DOI));
        add(dummy);
        add(new BibtexSingleField(FieldName.EID, true, BibtexSingleField.SMALL_W));

        dummy = new BibtexSingleField(FieldName.DATE, true);
        dummy.setExtras(EnumSet.of(FieldProperty.DATE));
        add(dummy);

        add(new BibtexSingleField(FieldName.PMID, false, BibtexSingleField.SMALL_W, 60).setNumeric(true));

        // additional fields ------------------------------------------------------
        add(new BibtexSingleField(FieldName.LOCATION, false));
        add(new BibtexSingleField(FieldName.ABSTRACT, false, BibtexSingleField.LARGE_W, 400));

        dummy = new BibtexSingleField(FieldName.URL, false, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperty.EXTERNAL, FieldProperty.VERBATIM));
        add(dummy);

        add(new BibtexSingleField("comment", false, BibtexSingleField.MEDIUM_W));
        add(new BibtexSingleField(FieldName.KEYWORDS, false, BibtexSingleField.SMALL_W));

        dummy = new BibtexSingleField(FieldName.FILE, false);
        dummy.setExtras(EnumSet.of(FieldProperty.FILE_EDITOR, FieldProperty.VERBATIM));
        add(dummy);

        dummy = new BibtexSingleField(FieldName.RELATED, false);
        dummy.setExtras(EnumSet.of(FieldProperty.MULTIPLE_ENTRY_LINK));
        add(dummy);

        // some BibLatex fields
        dummy = new BibtexSingleField(FieldName.GENDER, true, BibtexSingleField.SMALL_W);
        dummy.getFieldProperties().add(FieldProperty.GENDER);
        add(dummy);

        dummy = new BibtexSingleField(FieldName.PUBSTATE, true, BibtexSingleField.SMALL_W);
        dummy.getFieldProperties().add(FieldProperty.PUBLICATION_STATE);
        add(dummy);

        // some internal fields ----------------------------------------------
        dummy = new BibtexSingleField(FieldName.NUMBER_COL, false, 32);
        dummy.setPrivate();
        dummy.setWriteable(false);
        dummy.setDisplayable(false);
        add(dummy);

        dummy = new BibtexSingleField(FieldName.OWNER, false, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperty.OWNER));
        dummy.setPrivate();
        add(dummy);

        timeStampField = timeStampFieldName;
        dummy = new BibtexSingleField(timeStampFieldName, false, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperty.DATE));
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
            dummy = new BibtexSingleField(yesNoField, false);
            dummy.setExtras(EnumSet.of(FieldProperty.YES_NO));
            add(dummy);
        }

        // Fields that should be an integer value
        for (String numericField : INTEGER_FIELDS) {
            BibtexSingleField field = fieldSet.get(numericField);
            if (field == null) {
                field = new BibtexSingleField(numericField, true, BibtexSingleField.SMALL_W).setNumeric(true);
            }
            field.getFieldProperties().add(FieldProperty.INTEGER);
            add(field);
        }

        // Fields that should be treated as verbatim, so no formatting requirements
        for (String fieldText : VERBATIM_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true, BibtexSingleField.SMALL_W);
            }
            field.getFieldProperties().add(FieldProperty.VERBATIM);
            add(field);
        }

        // Set all fields with person names
        for (String fieldText : BIBLATEX_PERSON_NAME_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true, BibtexSingleField.SMALL_W);
            }
            field.getFieldProperties().add(FieldProperty.PERSON_NAMES);
            add(field);
        }

        // Set all fields which should contain editor types
        for (String fieldText : BIBLATEX_EDITOR_TYPE_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true, BibtexSingleField.SMALL_W);
            }
            field.getFieldProperties().add(FieldProperty.EDITOR_TYPE);
            add(field);
        }

        // Set all fields which are pagination fields
        for (String fieldText : BIBLATEX_PAGINATION_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true, BibtexSingleField.SMALL_W);
            }
            field.getFieldProperties().add(FieldProperty.PAGINATION);
            add(field);
        }

        // Set all fields with dates
        for (String fieldText : BIBLATEX_DATE_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true, BibtexSingleField.SMALL_W);
            }
            field.getFieldProperties().add(FieldProperty.DATE);
            field.getFieldProperties().add(FieldProperty.ISO_DATE);
            add(field);
        }

        // Set all fields with journal names
        for (String fieldText : BIBLATEX_JOURNAL_NAME_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true, BibtexSingleField.SMALL_W);
            }
            field.getFieldProperties().add(FieldProperty.JOURNAL_NAME);
            add(field);
        }

        // Set all fields with book names
        for (String fieldText : BIBLATEX_BOOK_NAME_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true, BibtexSingleField.SMALL_W);
            }
            field.getFieldProperties().add(FieldProperty.BOOK_NAME);
            add(field);
        }

        // Set all fields containing a language
        for (String fieldText : BIBLATEX_LANGUAGE_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true, BibtexSingleField.SMALL_W);
            }
            field.getFieldProperties().add(FieldProperty.LANGUAGE);
            add(field);
        }

        // Set all fields with multiple key links
        for (String fieldText : BIBLATEX_MULTI_KEY_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true, BibtexSingleField.SMALL_W);
            }
            field.getFieldProperties().add(FieldProperty.MULTIPLE_ENTRY_LINK);
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
                field.setNumeric(nF.contains(fieldName));
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

    /**
     * insert a field into the internal list
     */
    private void add(BibtexSingleField field) {
        // field == null check
        String key = field.getFieldName();
        fieldSet.put(key, field);
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

    public static Set<FieldProperty> getFieldProperties(String name) {
        Optional<BibtexSingleField> sField = InternalBibtexFields.getField(name);
        if (sField.isPresent()) {
            return sField.get().getFieldProperties();
        }
        return EnumSet.noneOf(FieldProperty.class);
    }

    public static double getFieldWeight(String name) {
        Optional<BibtexSingleField> sField = InternalBibtexFields.getField(name);
        if (sField.isPresent()) {
            return sField.get().getWeight();
        }
        return BibtexSingleField.DEFAULT_FIELD_WEIGHT;
    }

    public static void setFieldWeight(String fieldName, double weight) {
        Optional<BibtexSingleField> sField = InternalBibtexFields.getField(fieldName);
        if (sField.isPresent()) {
            sField.get().setWeight(weight);
        }
    }

    public static int getFieldLength(String name) {
        Optional<BibtexSingleField> sField = InternalBibtexFields.getField(name);
        if (sField.isPresent()) {
            return sField.get().getLength();
        }
        return BibtexSingleField.DEFAULT_FIELD_LENGTH;
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

    /**
     * returns a List with all fieldnames
     */
    public static List<String> getAllPublicFieldNames() {
        // collect all public fields
        List<String> publicFields = new ArrayList<>();
        for (BibtexSingleField sField : InternalBibtexFields.RUNTIME.fieldSet.values()) {
            if (!sField.isPrivate()) {
                publicFields.add(sField.getFieldName());
                // or export the complete BibtexSingleField ?
                // BibtexSingleField.toString() { return fieldname ; }
            }
        }

        // sort the entries
        Collections.sort(publicFields);

        return publicFields;
    }

    /**
     * returns a List with all fieldnames incl. internal fieldnames
     */
    public static List<String> getAllPublicAndInteralFieldNames() {
        //add the internal field names to public fields
        List<String> publicAndInternalFields = new ArrayList<>();
        publicAndInternalFields.addAll(InternalBibtexFields.getAllPublicFieldNames());
        publicAndInternalFields.add(FieldName.INTERNAL_ALL_FIELD);
        publicAndInternalFields.add(FieldName.INTERNAL_ALL_TEXT_FIELDS_FIELD);

        // sort the entries
        Collections.sort(publicAndInternalFields);

        return publicAndInternalFields;
    }

    public static List<String> getJournalNameFields() {
        return InternalBibtexFields.getAllPublicFieldNames().stream().filter(
                fieldName -> InternalBibtexFields.getFieldProperties(fieldName).contains(FieldProperty.JOURNAL_NAME))
                .collect(Collectors.toList());
    }

    public static List<String> getBookNameFields() {
        return InternalBibtexFields.getAllPublicFieldNames().stream()
                .filter(fieldName -> InternalBibtexFields.getFieldProperties(fieldName).contains(FieldProperty.BOOK_NAME))
                .collect(Collectors.toList());
    }

    public static List<String> getPersonNameFields() {
        return InternalBibtexFields.getAllPublicFieldNames().stream().filter(
                fieldName -> InternalBibtexFields.getFieldProperties(fieldName).contains(FieldProperty.PERSON_NAMES))
                .collect(Collectors.toList());
    }

    public static List<String> getIEEETranBSTctlYesNoFields() {
        return IEEETRANBSTCTL_YES_NO_FIELDS;
    }

}
