package net.sf.jabref.model.entry;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * String constants for BibTeX entry field names
 *
 */
public class FieldName {

    // Character separating field names that are to be used in sequence as
    // fallbacks for a single column (e.g. "author/editor" to use editor where
    // author is not set):
    public static final String FIELD_SEPARATOR = "/";

    // Field name constants
    public static final String ABSTRACT = "abstract";
    public static final String ADDRESS = "address";
    public static final String ANNOTE = "annote";
    public static final String ANNOTATION = "annotation";
    public static final String AUTHOR = "author";
    public static final String BOOKAUTHOR = "bookauthor";
    public static final String BOOKSUBTITLE = "booksubtitle";
    public static final String BOOKTITLE = "booktitle";
    public static final String BOOKTITLEADDON = "booktitleaddon";
    public static final String CHAPTER = "chapter";
    public static final String COMMENTS = "comments";
    public static final String CROSSREF = "crossref";
    public static final String DATE = "date";
    public static final String DOI = "doi";
    public static final String EDITION = "edition";
    public static final String EDITOR = "editor";
    public static final String EID = "eid";
    public static final String ENTRYSET = "entryset";
    public static final String EPRINT = "eprint";
    public static final String EPRINTCLASS = "eprintclass";
    public static final String EPRINTTYPE = "eprinttype";
    public static final String FILE = "file";
    public static final String FOLDER = "folder";
    public static final String GENDER = "gender";
    public static final String HOWPUBLISHED = "howpublished";
    public static final String INSTITUTION = "institution";
    public static final String ISBN = "isbn";
    public static final String ISRN = "isrn";
    public static final String ISSN = "issn";
    public static final String ISSUE = "issue";
    public static final String JOURNAL = "journal";
    public static final String JOURNALSUBTITLE = "journalsubtitle";
    public static final String JOURNALTITLE = "journaltitle";
    public static final String KEY = "key";
    public static final String KEYWORDS = "keywords";
    public static final String LANGUAGE = "language";
    public static final String LOCATION = "location";
    public static final String MAINSUBTITLE = "mainsubtitle";
    public static final String MAINTITLE = "maintitle";
    public static final String MAINTITLEADDON = "maintitleaddon";
    public static final String MONTH = "month";
    public static final String NOTE = "note";
    public static final String NUMBER = "number";
    public static final String ORGANIZATION = "organization";
    public static final String ORIGLANGUAGE = "origlanguage";
    public static final String PAGES = "pages";
    public static final String PAGETOTAL = "pagetotal";
    public static final String PART = "part";
    public static final String PDF = "pdf";
    public static final String PS = "ps";
    public static final String PUBLISHER = "publisher";
    public static final String PUBSTATE = "pubstate";
    public static final String RELATED = "related";
    public static final String REPORTNO = "reportno";
    public static final String REVIEW = "review";
    public static final String SCHOOL = "school";
    public static final String SERIES = "series";
    public static final String SUBTITLE = "subtitle";
    public static final String TITLE = "title";
    public static final String TITLEADDON = "titleaddon";
    public static final String TRANSLATOR = "translator";
    public static final String TYPE = "type";
    public static final String URI = "uri";
    public static final String URL = "url";
    public static final String URLDATE = "urldate";
    public static final String VERSION = "version";
    public static final String VOLUME = "volume";
    public static final String VOLUMES = "volumes";
    public static final String YEAR = "year";
    public static final String YEARFILED = "yearfiled";

    // JabRef internal field names
    public static final String OWNER = "owner";
    public static final String TIMESTAMP = "timestamp"; // Not the actual field name, but the default value
    public static final String NUMBER_COL = "#";
    public static final String GROUPS = "groups";
    public static final String SEARCH_INTERNAL = "__search";
    public static final String GROUPSEARCH_INTERNAL = "__groupsearch";
    public static final String MARKED_INTERNAL = "__markedentry";

    // Map to hold alternative display names
    private static final Map<String, String> displayNames = new HashMap<>();


    public static String orFields(String... fields) {
        return String.join(FieldName.FIELD_SEPARATOR, fields);
    }

    public static String orFields(List<String> fields) {
        return String.join(FieldName.FIELD_SEPARATOR, fields);
    }

    static {
        displayNames.put(FieldName.DOI, "DOI");
        displayNames.put(FieldName.ISBN, "ISBN");
        displayNames.put(FieldName.ISRN, "ISRN");
        displayNames.put(FieldName.ISSN, "ISSN");
        displayNames.put(FieldName.PS, "PS");
        displayNames.put(FieldName.PDF, "PDF");
        displayNames.put(FieldName.URI, "URI");
        displayNames.put(FieldName.URL, "URL");
    }


    /**
     * @param field - field to get the display version for
     * @return A version of the field name more suitable for display
     */
    public static String getDisplayName(String field) {
        String lowercaseField = field.toLowerCase(Locale.ROOT);

        if (displayNames.containsKey(lowercaseField)) {
            return displayNames.get(lowercaseField);
        }
        return EntryUtil.capitalizeFirst(field);
    }
}
