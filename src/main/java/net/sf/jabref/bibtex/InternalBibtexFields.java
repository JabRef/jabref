/*  Copyright (C) 2003-2016 Raik Nagel and JabRef contributors
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
//
// function : Handling of bibtex fields.
//            All bibtex-field related stuff should be placed here!
//            Because we can export these informations into additional
//            config files -> simple extension and definition of new fields....
//
// todo     : - handling of identically fields with different names
//              e.g. LCCN = lib-congress
//            - group id for each fields, e.g. standard, jurabib, bio....
//            - add a additional properties functionality into the
//              BibtexSingleField class
//
// modified : r.nagel 25.04.2006
//            export/import of some definition from/to a xml file

package net.sf.jabref.bibtex;

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

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.specialfields.SpecialFieldsUtils;

public class InternalBibtexFields {

    // some internal fields
    public static final String SEARCH = "__search";
    public static final String GROUPSEARCH = "__groupsearch";
    public static final String MARKED = "__markedentry";
    public static final String OWNER = "owner";
    public static final String TIMESTAMP = "timestamp";
    private static final String ENTRYTYPE = "entrytype";
    public static final String NUMBER_COL = "#";


    // contains all bibtex-field objects (BibtexSingleField)
    private final Map<String, BibtexSingleField> fieldSet;

    // contains all known (and public) bibtex fieldnames
    private final List<String> PUBLIC_FIELDS = new ArrayList<>();

    // Lists of fields with special properties
    public static final List<String> IEEETRANBSTCTL_NUMERIC_FIELDS = Arrays.asList("ctlmax_names_forced_etal",
            "ctlnames_show_etal", "ctlalt_stretch_factor");
    public static final List<String> IEEETRANBSTCTL_YES_NO_FIELDS = Arrays.asList("ctluse_article_number",
            "ctluse_paper", "ctluse_url", "ctluse_forced_etal", "ctluse_alt_spacing", "ctldash_repeated_names");
    public static final List<String> BIBLATEX_DATE_FIELDS = Arrays.asList("date", "eventdate", "origdate", "urldate");
    public static final List<String> BIBLATEX_PERSON_NAME_FIELDS = Arrays.asList("author", "editor", "editora",
            "editorb", "editorc", "translator", "annotator", "commentator", "introduction", "foreword", "afterword",
            "bookauthor", "holder", "shortauthor", "shorteditor", "sortname");

    // singleton instance
    private static final InternalBibtexFields RUNTIME = new InternalBibtexFields();

    private InternalBibtexFields() {
        fieldSet = new HashMap<>();
        BibtexSingleField dummy;

        // FIRST: all standard fields
        // These are the fields that BibTex might want to treat, so these
        // must conform to BibTex rules.
        add(new BibtexSingleField("address", true, BibtexSingleField.SMALL_W));
        // An annotation. It is not used by the standard bibliography styles,
        // but may be used by others that produce an annotated bibliography.
        // http://www.ecst.csuchico.edu/~jacobsd/bib/formats/bibtex.html
        add(new BibtexSingleField("annote", true, BibtexSingleField.LARGE_W));
        add(new BibtexSingleField("author", true, BibtexSingleField.MEDIUM_W, 280));
        add(new BibtexSingleField("booktitle", true, 175));
        add(new BibtexSingleField("chapter", true, BibtexSingleField.SMALL_W));
        add(new BibtexSingleField("crossref", true, BibtexSingleField.SMALL_W));
        add(new BibtexSingleField("edition", true, BibtexSingleField.SMALL_W));
        add(new BibtexSingleField("editor", true, BibtexSingleField.MEDIUM_W, 280));
        add(new BibtexSingleField("howpublished", true, BibtexSingleField.MEDIUM_W));
        add(new BibtexSingleField("institution", true, BibtexSingleField.MEDIUM_W));

        dummy = new BibtexSingleField("journal", true, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperties.JOURNAL_NAME));
        add(dummy);
        dummy = new BibtexSingleField("journaltitle", true, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperties.JOURNAL_NAME));
        add(dummy);

        add(new BibtexSingleField("key", true));
        dummy = new BibtexSingleField("month", true, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperties.MONTH));
        add(dummy);
        add(new BibtexSingleField("note", true, BibtexSingleField.MEDIUM_W));
        add(new BibtexSingleField("number", true, BibtexSingleField.SMALL_W, 60).setNumeric(true));
        add(new BibtexSingleField("organization", true, BibtexSingleField.MEDIUM_W));
        add(new BibtexSingleField("pages", true, BibtexSingleField.SMALL_W));
        add(new BibtexSingleField("publisher", true, BibtexSingleField.MEDIUM_W));
        add(new BibtexSingleField("school", true, BibtexSingleField.MEDIUM_W));
        add(new BibtexSingleField("series", true, BibtexSingleField.SMALL_W));
        add(new BibtexSingleField("title", true, 400));
        add(new BibtexSingleField("type", true, BibtexSingleField.SMALL_W));
        add(new BibtexSingleField("language", true, BibtexSingleField.SMALL_W));
        add(new BibtexSingleField("volume", true, BibtexSingleField.SMALL_W, 60).setNumeric(true));
        add(new BibtexSingleField("year", true, BibtexSingleField.SMALL_W, 60).setNumeric(true));

        // custom fields not displayed at editor, but as columns in the UI
        dummy = new BibtexSingleField(SpecialFieldsUtils.FIELDNAME_RANKING, false);
        if (!Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS)) {
            dummy.setPrivate();
            dummy.setWriteable(false);
            dummy.setDisplayable(false);
        }
        add(dummy);
        dummy = new BibtexSingleField(SpecialFieldsUtils.FIELDNAME_PRIORITY, false);
        if (!Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS)) {
            dummy.setPrivate();
            dummy.setWriteable(false);
            dummy.setDisplayable(false);
        }
        add(dummy);
        dummy = new BibtexSingleField(SpecialFieldsUtils.FIELDNAME_RELEVANCE, false);
        if (!Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS)) {
            dummy.setPrivate();
            dummy.setWriteable(false);
            dummy.setDisplayable(false);
        }
        add(dummy);
        dummy = new BibtexSingleField(SpecialFieldsUtils.FIELDNAME_QUALITY, false);
        if (!Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS)) {
            dummy.setPrivate();
            dummy.setWriteable(false);
            dummy.setDisplayable(false);
        }
        add(dummy);
        dummy = new BibtexSingleField(SpecialFieldsUtils.FIELDNAME_READ, false);
        if (!Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS)) {
            dummy.setPrivate();
            dummy.setWriteable(false);
            dummy.setDisplayable(false);
        }
        add(dummy);
        dummy = new BibtexSingleField(SpecialFieldsUtils.FIELDNAME_PRINTED, false);
        if (!Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SERIALIZESPECIALFIELDS)) {
            dummy.setPrivate();
            dummy.setWriteable(false);
            dummy.setDisplayable(false);
        }
        add(dummy);

        // some semi-standard fields
        dummy = new BibtexSingleField(BibEntry.KEY_FIELD, true);
        dummy.setPrivate();
        add(dummy);

        dummy = new BibtexSingleField("doi", true, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperties.DOI));
        add(dummy);
        add(new BibtexSingleField("eid", true, BibtexSingleField.SMALL_W));

        dummy = new BibtexSingleField("date", true);
        dummy.setPrivate();
        add(dummy);

        add(new BibtexSingleField("pmid", false, BibtexSingleField.SMALL_W, 60).setNumeric(true));

        // additional fields ------------------------------------------------------
        add(new BibtexSingleField("location", false));
        add(new BibtexSingleField("abstract", false, BibtexSingleField.LARGE_W, 400));

        dummy = new BibtexSingleField("url", false, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperties.EXTERNAL));
        add(dummy);

        add(new BibtexSingleField("comment", false, BibtexSingleField.MEDIUM_W));
        add(new BibtexSingleField("keywords", false, BibtexSingleField.SMALL_W));

        dummy = new BibtexSingleField(Globals.FILE_FIELD, false);
        dummy.setExtras(EnumSet.of(FieldProperties.FILE_EDITOR));
        add(dummy);

        add(new BibtexSingleField("search", false, 75));

        // some BibLatex fields
        dummy = new BibtexSingleField("gender", true, BibtexSingleField.SMALL_W);
        dummy.getExtras().add(FieldProperties.GENDER);
        add(dummy);

        // some internal fields ----------------------------------------------
        dummy = new BibtexSingleField(InternalBibtexFields.NUMBER_COL, false, 32);
        dummy.setPrivate();
        dummy.setWriteable(false);
        dummy.setDisplayable(false);
        add(dummy);

        dummy = new BibtexSingleField(InternalBibtexFields.OWNER, false, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperties.OWNER));
        dummy.setPrivate();
        add(dummy);

        dummy = new BibtexSingleField(InternalBibtexFields.TIMESTAMP, false, BibtexSingleField.SMALL_W);
        dummy.setExtras(EnumSet.of(FieldProperties.DATE));
        dummy.setPrivate();
        add(dummy);

        dummy = new BibtexSingleField(InternalBibtexFields.ENTRYTYPE, false, 75);
        dummy.setPrivate();
        add(dummy);

        dummy = new BibtexSingleField(InternalBibtexFields.SEARCH, false);
        dummy.setPrivate();
        dummy.setWriteable(false);
        dummy.setDisplayable(false);
        add(dummy);

        dummy = new BibtexSingleField(InternalBibtexFields.GROUPSEARCH, false);
        dummy.setPrivate();
        dummy.setWriteable(false);
        dummy.setDisplayable(false);
        add(dummy);

        dummy = new BibtexSingleField(InternalBibtexFields.MARKED, false);
        dummy.setPrivate();
        dummy.setWriteable(true); // This field must be written to file!
        dummy.setDisplayable(false);
        add(dummy);

        // IEEEtranBSTCTL fields
        for (String yesNoField : IEEETRANBSTCTL_YES_NO_FIELDS) {
            dummy = new BibtexSingleField(yesNoField, false);
            dummy.setExtras(EnumSet.of(FieldProperties.YES_NO));
            add(dummy);
        }

        for (String numericField : IEEETRANBSTCTL_NUMERIC_FIELDS) {
            dummy = new BibtexSingleField(numericField, false).setNumeric(true);
            dummy.getExtras().add(FieldProperties.INTEGER);
            add(dummy);
        }

        // Set all fields with person names
        for (String fieldText : BIBLATEX_PERSON_NAME_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true, BibtexSingleField.SMALL_W);
            }
            field.getExtras().add(FieldProperties.PERSON_NAMES);
            add(field);
        }

        // Set all fields with dates
        for (String fieldText : BIBLATEX_DATE_FIELDS) {
            BibtexSingleField field = fieldSet.get(fieldText);
            if (field == null) {
                field = new BibtexSingleField(fieldText, true, BibtexSingleField.SMALL_W);
            }
            field.getExtras().add(FieldProperties.DATE);
            add(field);
        }

        // collect all public fields for the PUBLIC_FIELDS array
        for (BibtexSingleField sField : fieldSet.values()) {
            if (!sField.isPrivate()) {
                PUBLIC_FIELDS.add(sField.getFieldName());
                // or export the complete BibtexSingleField ?
                // BibtexSingleField.toString() { return fieldname ; }
            }
        }

        // sort the entries
        Collections.sort(PUBLIC_FIELDS);
    }

    /**
     * Read the "numericFields" string array from preferences, and activate numeric
     * sorting for all fields listed in the array. If an unknown field name is included,
     * add a field descriptor for the new field.
     */
    public static void setNumericFieldsFromPrefs() {
        List<String> numFields = Globals.prefs.getStringList(JabRefPreferences.NUMERIC_FIELDS);
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

    public static Set<FieldProperties> getFieldExtras(String name) {
        Optional<BibtexSingleField> sField = InternalBibtexFields.getField(name);
        if (sField.isPresent()) {
            return sField.get().getExtras();
        }
        return EnumSet.noneOf(FieldProperties.class);
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

    /**
     * returns a List with all fieldnames
     */
    public static List<String> getAllFieldNames() {
        return new ArrayList<>(InternalBibtexFields.RUNTIME.PUBLIC_FIELDS);
    }

    /**
     * returns a List with only private fieldnames
     */
    public static List<String> getAllPrivateFieldNames() {
        List<String> pFields = new ArrayList<>();
        for (BibtexSingleField sField : InternalBibtexFields.RUNTIME.fieldSet.values()) {
            if (sField.isPrivate()) {
                pFields.add(sField.getFieldName());
            }
        }
        return pFields;

    }

    /**
     * returns the fieldname of the entry at index t
     */
    public static String getFieldName(int t) {
        return InternalBibtexFields.RUNTIME.PUBLIC_FIELDS.get(t);
    }

    /**
     * returns the number of available fields
     */
    public static int numberOfPublicFields() {
        return InternalBibtexFields.RUNTIME.PUBLIC_FIELDS.size();
    }


    /*
       public static int getPreferredFieldLength(String name) {
       int l = DEFAULT_FIELD_LENGTH;
       Object o = fieldLength.get(name.toLowerCase());
       if (o != null)
       l = ((Integer)o).intValue();
       return l;
       }*/

}
