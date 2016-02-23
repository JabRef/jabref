/*  Copyright (C) 2003-2015 Raik Nagel and JabRef contributors
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

package net.sf.jabref.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.IEEETranEntryTypes;
import net.sf.jabref.specialfields.SpecialFieldsUtils;

public class InternalBibtexFields {

    // some internal fields
    public static final String SEARCH = "__search";
    public static final String GROUPSEARCH = "__groupsearch";
    public static final String MARKED = "__markedentry";
    public static final String OWNER = "owner";
    public static final String TIMESTAMP = "timestamp"; // it's also definied at the JabRefPreferences class
    private static final String ENTRYTYPE = "entrytype";
    public static final String EXTRA_YES_NO = "yesNo"; // Blank/Yes/No Combo-box
    public static final String EXTRA_URL = "url"; // Drop target for URL
    public static final String EXTRA_DATEPICKER = "datepicker"; // Calendar button and double-click in field to set current date
    public static final String EXTRA_JOURNAL_NAMES = "journalNames"; // Journal abbreviation button
    public static final String EXTRA_EXTERNAL = "external"; // Open external viewer on double-click
    public static final String EXTRA_BROWSE = "browse"; // Browse button, file dialog
    public static final String EXTRA_BROWSE_DOC = "browseDoc"; // Browse button, file dialog with extension .fieldname
    public static final String EXTRA_BROWSE_DOC_ZIP = "browseDocZip"; // Browse button, file dialog with extension .fieldname, .fieldname.bz2, .filedname.gz
    public static final String EXTRA_SET_OWNER = "setOwner"; // Button to set owner to current used
    public static final String EXTRA_MONTH = "month"; // Button to show the months and set abbreviation

    public static final String[] DEFAULT_INSPECTION_FIELDS = new String[]
            {"author", "title", "year", BibEntry.KEY_FIELD};

    // singleton instance
    private static final InternalBibtexFields RUNTIME = new InternalBibtexFields();

    // contains all bibtex-field objects (BibtexSingleField)
    private final Map<String, BibtexSingleField> fieldSet;

    // contains all known (and public) bibtex fieldnames
    private final String[] PUBLIC_FIELDS;


    private InternalBibtexFields() {
        fieldSet = new HashMap<>();
        BibtexSingleField dummy;

        // FIRST: all standard fields
        // These are the fields that BibTex might want to treat, so these
        // must conform to BibTex rules.
        add(new BibtexSingleField("address", true, GUIGlobals.SMALL_W));
        // An annotation. It is not used by the standard bibliography styles,
        // but may be used by others that produce an annotated bibliography.
        // http://www.ecst.csuchico.edu/~jacobsd/bib/formats/bibtex.html
        add(new BibtexSingleField("annote", true, GUIGlobals.LARGE_W));
        add(new BibtexSingleField("author", true, GUIGlobals.MEDIUM_W, 280));
        add(new BibtexSingleField("booktitle", true, 175));
        add(new BibtexSingleField("chapter", true, GUIGlobals.SMALL_W));
        add(new BibtexSingleField("crossref", true, GUIGlobals.SMALL_W));
        add(new BibtexSingleField("edition", true, GUIGlobals.SMALL_W));
        add(new BibtexSingleField("editor", true, GUIGlobals.MEDIUM_W, 280));
        add(new BibtexSingleField("howpublished", true, GUIGlobals.MEDIUM_W));
        add(new BibtexSingleField("institution", true, GUIGlobals.MEDIUM_W));

        dummy = new BibtexSingleField("journal", true, GUIGlobals.SMALL_W);
        dummy.setExtras(EXTRA_JOURNAL_NAMES);
        add(dummy);
        dummy = new BibtexSingleField("journaltitle", true, GUIGlobals.SMALL_W);
        dummy.setExtras(EXTRA_JOURNAL_NAMES);
        add(dummy);

        add(new BibtexSingleField("key", true));
        dummy = new BibtexSingleField("month", true, GUIGlobals.SMALL_W);
        dummy.setExtras(EXTRA_MONTH);
        add(dummy);
        add(new BibtexSingleField("note", true, GUIGlobals.MEDIUM_W));
        add(new BibtexSingleField("number", true, GUIGlobals.SMALL_W, 60).setNumeric(true));
        add(new BibtexSingleField("organization", true, GUIGlobals.MEDIUM_W));
        add(new BibtexSingleField("pages", true, GUIGlobals.SMALL_W));
        add(new BibtexSingleField("publisher", true, GUIGlobals.MEDIUM_W));
        add(new BibtexSingleField("school", true, GUIGlobals.MEDIUM_W));
        add(new BibtexSingleField("series", true, GUIGlobals.SMALL_W));
        add(new BibtexSingleField("title", true, 400));
        add(new BibtexSingleField("type", true, GUIGlobals.SMALL_W));
        add(new BibtexSingleField("language", true, GUIGlobals.SMALL_W));
        add(new BibtexSingleField("volume", true, GUIGlobals.SMALL_W, 60).setNumeric(true));
        add(new BibtexSingleField("year", true, GUIGlobals.SMALL_W, 60).setNumeric(true));

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

        dummy = new BibtexSingleField("doi", true, GUIGlobals.SMALL_W);
        dummy.setExtras(EXTRA_EXTERNAL);
        add(dummy);
        add(new BibtexSingleField("eid", true, GUIGlobals.SMALL_W));

        dummy = new BibtexSingleField("date", true);
        dummy.setPrivate();
        add(dummy);

        add(new BibtexSingleField("pmid", false, GUIGlobals.SMALL_W, 60).setNumeric(true));

        // additional fields ------------------------------------------------------
        add(new BibtexSingleField("location", false));
        add(new BibtexSingleField("abstract", false, GUIGlobals.LARGE_W, 400));

        dummy = new BibtexSingleField("url", false, GUIGlobals.SMALL_W);
        dummy.setExtras(EXTRA_EXTERNAL);
        add(dummy);

        dummy = new BibtexSingleField("pdf", false, GUIGlobals.SMALL_W);
        dummy.setExtras(EXTRA_BROWSE_DOC);
        add(dummy);

        dummy = new BibtexSingleField("ps", false, GUIGlobals.SMALL_W);
        dummy.setExtras(EXTRA_BROWSE_DOC_ZIP);
        add(dummy);
        add(new BibtexSingleField("comment", false, GUIGlobals.MEDIUM_W));
        add(new BibtexSingleField("keywords", false, GUIGlobals.SMALL_W));
        //FIELD_EXTRAS.put("keywords", "selector");

        dummy = new BibtexSingleField(Globals.FILE_FIELD, false);
        dummy.setEditorType(GUIGlobals.FILE_LIST_EDITOR);
        add(dummy);

        add(new BibtexSingleField("search", false, 75));

        // some internal fields ----------------------------------------------
        dummy = new BibtexSingleField(GUIGlobals.NUMBER_COL, false, 32);
        dummy.setPrivate();
        dummy.setWriteable(false);
        dummy.setDisplayable(false);
        add(dummy);

        dummy = new BibtexSingleField(InternalBibtexFields.OWNER, false, GUIGlobals.SMALL_W);
        dummy.setExtras(EXTRA_SET_OWNER);
        dummy.setPrivate();
        add(dummy);

        dummy = new BibtexSingleField(InternalBibtexFields.TIMESTAMP, false, GUIGlobals.SMALL_W);
        dummy.setExtras(EXTRA_DATEPICKER);
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
        for (String yesNoField : IEEETranEntryTypes.IEEETRANBSTCTL_YES_NO_FIELDS) {
            dummy = new BibtexSingleField(yesNoField, false);
            dummy.setExtras(EXTRA_YES_NO);
            add(dummy);
        }

        for (String numericField : IEEETranEntryTypes.IEEETRANBSTCTL_NUMERIC_FIELDS) {
            add(new BibtexSingleField(numericField, false).setNumeric(true));
        }

        // collect all public fields for the PUBLIC_FIELDS array
        List<String> pFields = new ArrayList<>(fieldSet.size());
        for (BibtexSingleField sField : fieldSet.values()) {
            if (!sField.isPrivate()) {
                pFields.add(sField.getFieldName());
                // or export the complete BibtexSingleField ?
                // BibtexSingleField.toString() { return fieldname ; }
            }
        }

        PUBLIC_FIELDS = pFields.toArray(new String[pFields.size()]);
        // sort the entries
        Arrays.sort(PUBLIC_FIELDS);
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
        String key = field.name;
        fieldSet.put(key, field);
    }

    // --------------------------------------------------------------------------
    //  the "static area"
    // --------------------------------------------------------------------------
    private static BibtexSingleField getField(String name) {
        if (name != null) {
            return InternalBibtexFields.RUNTIME.fieldSet.get(name.toLowerCase());
        }

        return null;
    }

    public static String getFieldExtras(String name) {
        BibtexSingleField sField = InternalBibtexFields.getField(name);
        if (sField != null) {
            return sField.getExtras();
        }
        return null;
    }

    public static int getEditorType(String name) {
        BibtexSingleField sField = InternalBibtexFields.getField(name);
        if (sField != null) {
            return sField.getEditorType();
        }
        return GUIGlobals.STANDARD_EDITOR;
    }

    public static double getFieldWeight(String name) {
        BibtexSingleField sField = InternalBibtexFields.getField(name);
        if (sField != null) {
            return sField.getWeight();
        }
        return GUIGlobals.DEFAULT_FIELD_WEIGHT;
    }

    public static void setFieldWeight(String fieldName, double weight) {
        BibtexSingleField sField = InternalBibtexFields.getField(fieldName);
        if (sField != null) {
            sField.setWeight(weight);
        }
    }

    public static int getFieldLength(String name) {
        BibtexSingleField sField = InternalBibtexFields.getField(name);
        if (sField != null) {
            return sField.getLength();
        }
        return GUIGlobals.DEFAULT_FIELD_LENGTH;
    }

    public static boolean isWriteableField(String field) {
        BibtexSingleField sField = InternalBibtexFields.getField(field);
        return (sField == null) || sField.isWriteable();
    }

    public static boolean isDisplayableField(String field) {
        BibtexSingleField sField = InternalBibtexFields.getField(field);
        return (sField == null) || sField.isDisplayable();
    }

    /**
     * Returns true if the given field is a standard Bibtex field.
     *
     * @param field a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isStandardField(String field) {
        BibtexSingleField sField = InternalBibtexFields.getField(field);
        return (sField != null) && sField.isStandard();
    }

    public static boolean isNumeric(String field) {
        BibtexSingleField sField = InternalBibtexFields.getField(field);
        return (sField != null) && sField.isNumeric();
    }

    /**
     * returns a List with all fieldnames
     */
    public static List<String> getAllFieldNames() {
        return Arrays.asList(InternalBibtexFields.RUNTIME.PUBLIC_FIELDS);
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
        return InternalBibtexFields.RUNTIME.PUBLIC_FIELDS[t];
    }

    /**
     * returns the number of available fields
     */
    public static int numberOfPublicFields() {
        return InternalBibtexFields.RUNTIME.PUBLIC_FIELDS.length;
    }


    /*
       public static int getPreferredFieldLength(String name) {
       int l = DEFAULT_FIELD_LENGTH;
       Object o = fieldLength.get(name.toLowerCase());
       if (o != null)
       l = ((Integer)o).intValue();
       return l;
       }*/

    // --------------------------------------------------------------------------
    // a container class for all properties of a bibtex-field
    // --------------------------------------------------------------------------
    private static class BibtexSingleField {

        private static final int STANDARD = 0x01; // it is a standard bibtex-field
        private static final int PRIVATE = 0x02; // internal use, e.g. owner, timestamp
        private static final int DISPLAYABLE = 0x04; // These fields cannot be shown inside the source editor panel
        private static final int WRITEABLE = 0x08; // These fields will not be saved to the .bib file.

        // the fieldname
        private final String name;

        // contains the standard, private, displayable, writable infos
        // default is: not standard, public, displayable and writable
        private int flag = BibtexSingleField.DISPLAYABLE | BibtexSingleField.WRITEABLE;

        private int length = GUIGlobals.DEFAULT_FIELD_LENGTH;
        private double weight = GUIGlobals.DEFAULT_FIELD_WEIGHT;

        private int editorType = GUIGlobals.STANDARD_EDITOR;


        // the extras data
        // fieldExtras contains mappings to tell the EntryEditor to add a specific
        // function to this field, for instance a "browse" button for the "pdf" field.
        private String extras;

        // This value defines whether contents of this field are expected to be
        // numeric values. This can be used to sort e.g. volume numbers correctly:
        private boolean numeric;


        // a comma separated list of alternative bibtex-fieldnames, e.g.
        // "LCCN" is the same like "lib-congress"
        // private String otherNames = null ;

        // a Hashmap for a lot of additional "not standard" properties
        // todo: add the handling in a key=value manner
        // private HashMap props = new HashMap() ;

        public BibtexSingleField(String fieldName, boolean pStandard) {
            name = fieldName;
            setFlag(pStandard, BibtexSingleField.STANDARD);
        }

        public BibtexSingleField(String fieldName, boolean pStandard, double pWeight) {
            name = fieldName;
            setFlag(pStandard, BibtexSingleField.STANDARD);
            weight = pWeight;
        }

        public BibtexSingleField(String fieldName, boolean pStandard, int pLength) {
            name = fieldName;
            setFlag(pStandard, BibtexSingleField.STANDARD);
            length = pLength;
        }

        public BibtexSingleField(String fieldName, boolean pStandard,
                double pWeight, int pLength) {
            name = fieldName;
            setFlag(pStandard, BibtexSingleField.STANDARD);
            weight = pWeight;
            length = pLength;
        }

        // -----------------------------------------------------------------------
        // -----------------------------------------------------------------------

        /**
         * Sets or onsets the given flag
         * @param setToOn if true, set the flag; if false, unset the flat
         * @param flagID, the id of the flag
         */
        private void setFlag(boolean setToOn, int flagID) {
            if (setToOn) {
                // set the flag
                flag = flag | flagID;
            } else {
                // unset the flag
                flag = flag & (0xff ^ flagID);
            }
        }

        private boolean isSet(int flagID) {
            return (flag & flagID) == flagID;
        }

        // -----------------------------------------------------------------------
        public boolean isStandard() {
            return isSet(BibtexSingleField.STANDARD);
        }

        public void setPrivate() {
            flag = flag | BibtexSingleField.PRIVATE;
        }

        public boolean isPrivate() {
            return isSet(BibtexSingleField.PRIVATE);
        }

        public void setDisplayable(boolean value) {
            setFlag(value, BibtexSingleField.DISPLAYABLE);
        }

        public boolean isDisplayable() {
            return isSet(BibtexSingleField.DISPLAYABLE);
        }

        public void setWriteable(boolean value) {
            setFlag(value, BibtexSingleField.WRITEABLE);
        }

        public boolean isWriteable() {
            return isSet(BibtexSingleField.WRITEABLE);
        }

        // -----------------------------------------------------------------------

        public void setExtras(String pExtras) {
            extras = pExtras;
        }

        // fieldExtras contains mappings to tell the EntryEditor to add a specific
        // function to this field, for instance a "browse" button for the "pdf" field.
        public String getExtras() {
            return extras;
        }

        public void setEditorType(int type) {
            editorType = type;
        }

        public int getEditorType() {
            return editorType;
        }

        // -----------------------------------------------------------------------

        public void setWeight(double value) {
            this.weight = value;
        }

        public double getWeight() {
            return this.weight;
        }

        /**
         * @return The maximum (expected) length of the field value; <em>not</em> the length of the field name
         */
        public int getLength() {
            return this.length;
        }

        // -----------------------------------------------------------------------

        public String getFieldName() {
            return name;
        }

        /**
         * Set this field's numeric property
         *
         * @param numeric true to indicate that this is a numeric field.
         * @return this BibtexSingleField instance. Makes it easier to call this
         * method on the fly while initializing without using a local variable.
         */
        public BibtexSingleField setNumeric(boolean numeric) {
            this.numeric = numeric;
            return this;
        }

        public boolean isNumeric() {
            return numeric;
        }

    }
}
