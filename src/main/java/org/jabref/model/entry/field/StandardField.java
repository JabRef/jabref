package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Optional;

/**
 * Standard BibTeX and BibLaTex fields
 */
public enum StandardField implements Field<StandardField> {

    ABSTRACT("abstract"),
    ADDENDUM("addendum"),
    ADDRESS("address"),
    AFTERWORD("afterword"),
    ANNOTE("annote"),
    ANNOTATION("annotation"),
    ANNOTATOR("annotator"),
    ARCHIVEPREFIX("archiveprefix"),
    ASSIGNEE("assignee"),
    AUTHOR("author"),
    BOOKAUTHOR("bookauthor"),
    BOOKPAGINATION("bookpagination"),
    BOOKSUBTITLE("booksubtitle"),
    BOOKTITLE("booktitle"),
    BOOKTITLEADDON("booktitleaddon"),
    CHAPTER("chapter"),
    COMMENTATOR("commentator"),
    COMMENT("comment"),
    CROSSREF("crossref"),
    DATE("date"),
    DAY("day"),
    DAYFILED("dayfiled"),
    DOI("doi", "DOI"),
    EDITION("edition"),
    EDITOR("editor"),
    EDITORA("editora"),
    EDITORB("editorb"),
    EDITORC("editorc"),
    EDITORTYPE("editortype"),
    EDITORATYPE("editoratype"),
    EDITORBTYPE("editorbtype"),
    EDITORCTYPE("editorctype"),
    EID("eid"),
    ENTRYSET("entryset"),
    EPRINT("eprint"),
    EPRINTCLASS("eprintclass"),
    EPRINTTYPE("eprinttype"),
    EVENTDATE("eventdate"),
    EVENTTITLE("eventtitle"),
    EVENTTITLEADDON("eventtitleaddon"),
    FILE("file"),
    FOREWORD("foreword"),
    FOLDER("folder"),
    GENDER("gender"),
    HOLDER("holder"),
    HOWPUBLISHED("howpublished"),
    INSTITUTION("institution"),
    INTRODUCTION("introduction"),
    ISBN("isbn", "ISBN"),
    ISRN("isrn", "ISRN"),
    ISSN("issn", "ISSN"),
    ISSUE("issue"),
    ISSUETITLE("issuetitle"),
    ISSUESUBTITLE("issuesubtitle"),
    JOURNAL("journal"),
    JOURNALSUBTITLE("journalsubtitle"),
    JOURNALTITLE("journaltitle"),
    KEY("key"),
    KEYWORDS("keywords"),
    LANGUAGE("language"),
    LOCATION("location"),
    MAINSUBTITLE("mainsubtitle"),
    MAINTITLE("maintitle"),
    MAINTITLEADDON("maintitleaddon"),
    MONTH("month"),
    MONTHFILED("monthfiled"),
    NAMEADDON("nameaddon"),
    NATIONALITY("nationality"),
    NOTE("note"),
    NUMBER("number"),
    ORGANIZATION("organization"),
    ORIGDATE("origdate"),
    ORIGLANGUAGE("origlanguage"),
    PAGES("pages"),
    PAGETOTAL("pagetotal"),
    PAGINATION("pagination"),
    PART("part"),
    PDF("pdf", "PDF"),
    PMID("pmid", "PMID"),
    PS("ps", "PS"),
    PUBLISHER("publisher"),
    PUBSTATE("pubstate"),
    PRIMARYCLASS("primaryclass"),
    RELATED("related"),
    REPORTNO("reportno"),
    REVIEW("review"),
    REVISION("revision"),
    SCHOOL("school"),
    SERIES("series"),
    SHORTAUTHOR("shortauthor"),
    SHORTEDITOR("shorteditor"),
    SHORTTITLE("shorttitle"),
    SORTKEY("sortkey"),
    SORTNAME("sortname"),
    SUBTITLE("subtitle"),
    TITLE("title"),
    TITLEADDON("titleaddon"),
    TRANSLATOR("translator"),
    TYPE("type"),
    URI("uri", "URI"),
    URL("url", "URL"),
    URLDATE("urldate"),
    VENUE("venue"),
    VERSION("version"),
    VOLUME("volume"),
    VOLUMES("volumes"),
    YEAR("year"),
    YEARFILED("yearfiled"),
    MR_NUMBER("mrnumber");

    private final String name;
    private final String displayName;

    StandardField(String name) {
        this.name = name;
        this.displayName = null;
    }

    StandardField(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public static Optional<StandardField> fromName(String name) {
        return Arrays.stream(StandardField.values())
                     .filter(field -> field.getName().equalsIgnoreCase(name))
                     .findAny();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        if (displayName == null) {
            return Field.super.getDisplayName();
        } else {
            return displayName;
        }
    }
}
