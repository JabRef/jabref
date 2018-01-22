package org.jabref.model.entry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jabref.model.strings.StringUtil;

/**
 * String constants for BibTeX entry field names
 */
public class FieldName {
    // Character separating field names that are to be used in sequence as
    // fallbacks for a single column (e.g. "author/editor" to use editor where
    // author is not set):
    public static final String FIELD_SEPARATOR = "/";

    public static final String INTERNAL_ALL_FIELD = "all";
    public static final String INTERNAL_ALL_TEXT_FIELDS_FIELD = "all-text-fields";

    // Field name constants
    public static final String ABSTRACT = "abstract";
    public static final String ADDENDUM = "addendum";
    public static final String ADDRESS = "address";
    public static final String AFTERWORD = "afterword";
    public static final String ANNOTE = "annote";
    public static final String ANNOTATION = "annotation";
    public static final String ANNOTATOR = "annotator";
    public static final String ASSIGNEE = "assignee";
    public static final String AUTHOR = "author";
    public static final String BOOKAUTHOR = "bookauthor";
    public static final String BOOKPAGINATION = "bookpagination";
    public static final String BOOKSUBTITLE = "booksubtitle";
    public static final String BOOKTITLE = "booktitle";
    public static final String BOOKTITLEADDON = "booktitleaddon";
    public static final String CHAPTER = "chapter";
    public static final String COMMENTATOR = "commentator";
    public static final String COMMENT = "comment";
    public static final String CROSSREF = "crossref";
    public static final String DATE = "date";
    public static final String DAY = "day";
    public static final String DAYFILED = "dayfiled";
    public static final String DOI = "doi";
    public static final String EDITION = "edition";
    public static final String EDITOR = "editor";
    public static final String EDITORA = "editora";
    public static final String EDITORB = "editorb";
    public static final String EDITORC = "editorc";
    public static final String EDITORTYPE = "editortype";
    public static final String EDITORATYPE = "editoratype";
    public static final String EDITORBTYPE = "editorbtype";
    public static final String EDITORCTYPE = "editorctype";
    public static final String EID = "eid";
    public static final String ENTRYSET = "entryset";
    public static final String EPRINT = "eprint";
    public static final String EPRINTCLASS = "eprintclass";
    public static final String EPRINTTYPE = "eprinttype";
    public static final String EVENTDATE = "eventdate";
    public static final String EVENTTITLE = "eventtitle";
    public static final String EVENTTITLEADDON = "eventtitleaddon";
    public static final String FILE = "file";
    public static final String FOREWORD = "foreword";
    public static final String FOLDER = "folder";
    public static final String GENDER = "gender";
    public static final String HOLDER = "holder";
    public static final String HOWPUBLISHED = "howpublished";
    public static final String INSTITUTION = "institution";
    public static final String INTRODUCTION = "introduction";
    public static final String ISBN = "isbn";
    public static final String ISRN = "isrn";
    public static final String ISSN = "issn";
    public static final String ISSUE = "issue";
    public static final String ISSUETITLE = "issuetitle";
    public static final String ISSUESUBTITLE = "issuesubtitle";
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
    public static final String MONTHFILED = "monthfiled";
    public static final String NAMEADDON = "nameaddon";
    public static final String NATIONALITY = "nationality";
    public static final String NOTE = "note";
    public static final String NUMBER = "number";
    public static final String ORGANIZATION = "organization";
    public static final String ORIGDATE = "origdate";
    public static final String ORIGLANGUAGE = "origlanguage";
    public static final String PAGES = "pages";
    public static final String PAGETOTAL = "pagetotal";
    public static final String PAGINATION = "pagination";
    public static final String PART = "part";
    public static final String PDF = "pdf";
    public static final String PMID = "pmid";
    public static final String PS = "ps";
    public static final String PUBLISHER = "publisher";
    public static final String PUBSTATE = "pubstate";
    public static final String RELATED = "related";
    public static final String REPORTNO = "reportno";
    public static final String REVIEW = "review";
    public static final String REVISION = "revision";
    public static final String SCHOOL = "school";
    public static final String SERIES = "series";
    public static final String SHORTAUTHOR = "shortauthor";
    public static final String SHORTEDITOR = "shorteditor";
    public static final String SHORTTITLE = "shorttitle";
    public static final String SORTNAME = "sortname";
    public static final String SUBTITLE = "subtitle";
    public static final String TITLE = "title";
    public static final String TITLEADDON = "titleaddon";
    public static final String TRANSLATOR = "translator";
    public static final String TYPE = "type";
    public static final String URI = "uri";
    public static final String URL = "url";
    public static final String URLDATE = "urldate";
    public static final String VENUE = "venue";
    public static final String VERSION = "version";
    public static final String VOLUME = "volume";
    public static final String VOLUMES = "volumes";
    public static final String YEAR = "year";
    public static final String YEARFILED = "yearfiled";
    public static final String MR_NUMBER = "mrnumber";

    // IEEE BSTctl fields
    public static final String CTLALT_STRETCH_FACTOR = "ctlalt_stretch_factor";
    public static final String CTLDASH_REPEATED_NAMES = "ctldash_repeated_names";
    public static final String CTLMAX_NAMES_FORCED_ETAL = "ctlmax_names_forced_etal";
    public static final String CTLNAME_FORMAT_STRING = "ctlname_format_string";
    public static final String CTLNAME_LATEX_CMD = "ctlname_latex_cmd";
    public static final String CTLNAME_URL_PREFIX = "ctlname_url_prefix";
    public static final String CTLNAMES_SHOW_ETAL = "ctlnames_show_etal";
    public static final String CTLUSE_ALT_SPACING = "ctluse_alt_spacing";
    public static final String CTLUSE_ARTICLE_NUMBER = "ctluse_article_number";
    public static final String CTLUSE_FORCED_ETAL = "ctluse_forced_etal";
    public static final String CTLUSE_PAPER = "ctluse_paper";
    public static final String CTLUSE_URL = "ctluse_url";

    // JabRef internal field names
    public static final String OWNER = "owner";
    public static final String TIMESTAMP = "timestamp"; // Not the actual field name, but the default value
    public static final String NUMBER_COL = "#";
    public static final String GROUPS = "groups";
    public static final String SEARCH_INTERNAL = "__search";
    public static final String GROUPSEARCH_INTERNAL = "__groupsearch";
    public static final String MARKED_INTERNAL = "__markedentry";

    // Map to hold alternative display names
    private static final Map<String, String> DISPLAY_NAMES = new HashMap<>();

    private FieldName() {
    }

    public static String orFields(String... fields) {
        return String.join(FieldName.FIELD_SEPARATOR, fields);
    }

    public static String orFields(List<String> fields) {
        return String.join(FieldName.FIELD_SEPARATOR, fields);
    }

    static {
        DISPLAY_NAMES.put(FieldName.DOI, "DOI");
        DISPLAY_NAMES.put(FieldName.ISBN, "ISBN");
        DISPLAY_NAMES.put(FieldName.ISRN, "ISRN");
        DISPLAY_NAMES.put(FieldName.ISSN, "ISSN");
        DISPLAY_NAMES.put(FieldName.PMID, "PMID");
        DISPLAY_NAMES.put(FieldName.PS, "PS");
        DISPLAY_NAMES.put(FieldName.PDF, "PDF");
        DISPLAY_NAMES.put(FieldName.URI, "URI");
        DISPLAY_NAMES.put(FieldName.URL, "URL");
    }

    /**
     * @param field - field to get the display version for
     * @return A version of the field name more suitable for display
     */
    public static String getDisplayName(String field) {
        String lowercaseField = field.toLowerCase(Locale.ROOT);

        if (DISPLAY_NAMES.containsKey(lowercaseField)) {
            return DISPLAY_NAMES.get(lowercaseField);
        }
        return StringUtil.capitalizeFirst(field);
    }

    public static List<String> getNotTextFieldNames() {
        return Arrays.asList(FieldName.DOI, FieldName.FILE, FieldName.URL, FieldName.URI, FieldName.ISBN, FieldName.ISSN, FieldName.MONTH, FieldName.DATE, FieldName.YEAR);
    }

    public static List<String> getIdentifierFieldNames() {
        return Arrays.asList(FieldName.DOI, FieldName.EPRINT, FieldName.PMID);
    }
}
