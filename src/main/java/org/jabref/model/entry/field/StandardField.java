package org.jabref.model.entry.field;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.jabref.gui.fieldeditors.FieldNameLabel;

/**
 * Standard BibTeX and BibLaTeX fields, as well as "normal" JabRef specific fields.
 *
 * See {@link FieldNameLabel#getDescription(org.jabref.model.entry.field.Field)} for a description of each field.
 */
public enum StandardField implements Field {

    ABSTRACT("abstract"),
    ADDENDUM("addendum"),
    ADDRESS("address"),
    AFTERWORD("afterword", FieldProperty.PERSON_NAMES),
    ANNOTE("annote"),
    ANNOTATION("annotation"),
    ANNOTATOR("annotator", FieldProperty.PERSON_NAMES),
    ARCHIVEPREFIX("archiveprefix"),
    ASSIGNEE("assignee", FieldProperty.PERSON_NAMES),
    AUTHOR("author", FieldProperty.PERSON_NAMES),
    BOOKAUTHOR("bookauthor", FieldProperty.PERSON_NAMES),
    BOOKPAGINATION("bookpagination", FieldProperty.PAGINATION),
    BOOKSUBTITLE("booksubtitle", FieldProperty.BOOK_NAME),
    BOOKTITLE("booktitle", FieldProperty.BOOK_NAME),
    BOOKTITLEADDON("booktitleaddon"),
    CHAPTER("chapter"),
    COMMENTATOR("commentator", FieldProperty.PERSON_NAMES),
    COMMENT("comment"),
    CROSSREF("crossref", FieldProperty.SINGLE_ENTRY_LINK),
    DATE("date", FieldProperty.DATE),
    DAY("day"),
    DAYFILED("dayfiled"),
    DOI("doi", "DOI", FieldProperty.DOI),
    EDITION("edition", FieldProperty.NUMERIC),
    EDITOR("editor", FieldProperty.PERSON_NAMES),
    EDITORA("editora", FieldProperty.PERSON_NAMES),
    EDITORB("editorb", FieldProperty.PERSON_NAMES),
    EDITORC("editorc", FieldProperty.PERSON_NAMES),
    EDITORTYPE("editortype", FieldProperty.EDITOR_TYPE),
    EDITORATYPE("editoratype", FieldProperty.EDITOR_TYPE),
    EDITORBTYPE("editorbtype", FieldProperty.EDITOR_TYPE),
    EDITORCTYPE("editorctype", FieldProperty.EDITOR_TYPE),
    EID("eid"),
    ENTRYSET("entryset", FieldProperty.MULTIPLE_ENTRY_LINK),
    EPRINT("eprint", FieldProperty.EPRINT),
    EPRINTCLASS("eprintclass"),
    EPRINTTYPE("eprinttype"),
    EVENTDATE("eventdate", FieldProperty.DATE),
    EVENTTITLE("eventtitle"),
    EVENTTITLEADDON("eventtitleaddon"),
    FILE("file", FieldProperty.FILE_EDITOR, FieldProperty.VERBATIM),
    FOREWORD("foreword", FieldProperty.PERSON_NAMES),
    FOLDER("folder"),
    GENDER("gender", FieldProperty.GENDER),
    HALID("hal_id"),
    HALVERSION("hal_version"),
    HOLDER("holder", FieldProperty.PERSON_NAMES),
    HOWPUBLISHED("howpublished"),
    IDS("ids", FieldProperty.MULTIPLE_ENTRY_LINK),
    INSTITUTION("institution"),
    INTRODUCTION("introduction", FieldProperty.PERSON_NAMES),
    INTRODUCEDIN("introducedin"),
    ISBN("isbn", "ISBN", FieldProperty.ISBN),
    ISRN("isrn", "ISRN"),
    ISSN("issn", "ISSN"),
    ISSUE("issue"),
    ISSUETITLE("issuetitle"),
    ISSUESUBTITLE("issuesubtitle"),
    JOURNAL("journal", FieldProperty.JOURNAL_NAME),
    JOURNALSUBTITLE("journalsubtitle", FieldProperty.JOURNAL_NAME),
    JOURNALTITLE("journaltitle", FieldProperty.JOURNAL_NAME),
    KEY("key"),
    KEYWORDS("keywords"),
    LANGUAGE("language", FieldProperty.LANGUAGE),
    LABEL("label"),
    LIBRARY("library"),
    LICENSE("license"),
    LOCATION("location"),
    MAINSUBTITLE("mainsubtitle", FieldProperty.BOOK_NAME),
    MAINTITLE("maintitle", FieldProperty.BOOK_NAME),
    MAINTITLEADDON("maintitleaddon"),
    MONTH("month", FieldProperty.MONTH),
    MONTHFILED("monthfiled", FieldProperty.MONTH),
    NAMEADDON("nameaddon"),
    NATIONALITY("nationality"),
    NOTE("note"),
    NUMBER("number", FieldProperty.NUMERIC),
    ORGANIZATION("organization"),
    ORIGDATE("origdate", FieldProperty.DATE),
    ORIGLANGUAGE("origlanguage", FieldProperty.LANGUAGE),
    PAGES("pages", FieldProperty.PAGES),
    PAGETOTAL("pagetotal"),
    PAGINATION("pagination", FieldProperty.PAGINATION),
    PART("part"),
    PDF("pdf", "PDF"),
    PMID("pmid", "PMID", FieldProperty.NUMERIC),
    PS("ps", "PS"),
    PUBLISHER("publisher"),
    PUBSTATE("pubstate", FieldProperty.PUBLICATION_STATE),
    PRIMARYCLASS("primaryclass"),
    RELATED("related", FieldProperty.MULTIPLE_ENTRY_LINK),
    RELATEDTYPE("relatedtype"),
    RELATEDSTRING("relatedstring"),
    REPORTNO("reportno"),
    REPOSITORY("repository"),
    REVIEW("review"),
    REVISION("revision"),
    SCHOOL("school"),
    SERIES("series"),
    SHORTAUTHOR("shortauthor", FieldProperty.PERSON_NAMES),
    SHORTEDITOR("shorteditor", FieldProperty.PERSON_NAMES),
    SHORTTITLE("shorttitle"),
    SORTKEY("sortkey"),
    SORTNAME("sortname", FieldProperty.PERSON_NAMES),
    SUBTITLE("subtitle"),
    SWHID("swhid"),
    TITLE("title"),
    TITLEADDON("titleaddon"),
    TRANSLATOR("translator", FieldProperty.PERSON_NAMES),
    TYPE("type", FieldProperty.TYPE),
    URI("uri", "URI"),
    URL("url", "URL", FieldProperty.EXTERNAL, FieldProperty.VERBATIM),
    URLDATE("urldate", FieldProperty.DATE),
    VENUE("venue"),
    VERSION("version"),
    VOLUME("volume", FieldProperty.NUMERIC),
    VOLUMES("volumes", FieldProperty.NUMERIC),
    YEAR("year", FieldProperty.NUMERIC),
    YEARFILED("yearfiled"),
    MR_NUMBER("mrnumber"),
    ZBL_NUMBER("zbl"),
    XDATA("xdata", FieldProperty.MULTIPLE_ENTRY_LINK),
    XREF("xref", FieldProperty.SINGLE_ENTRY_LINK),

    // JabRef-specific fields
    GROUPS("groups"),
    OWNER("owner"),
    TIMESTAMP("timestamp", FieldProperty.DATE),
    CREATIONDATE("creationdate", FieldProperty.DATE),
    MODIFICATIONDATE("modificationdate", FieldProperty.DATE);


    private final String name;
    private final String displayName;
    private final Set<FieldProperty> properties;

    StandardField(String name) {
        this.name = name;
        this.displayName = null;
        this.properties = EnumSet.noneOf(FieldProperty.class);
    }

    StandardField(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
        this.properties = EnumSet.noneOf(FieldProperty.class);
    }

    StandardField(String name, String displayName, FieldProperty first, FieldProperty... rest) {
        this.name = name;
        this.displayName = displayName;
        this.properties = EnumSet.of(first, rest);
    }

    StandardField(String name, FieldProperty first, FieldProperty... rest) {
        this.name = name;
        this.displayName = null;
        this.properties = EnumSet.of(first, rest);
    }

    public static Optional<StandardField> fromName(String name) {
        return Arrays.stream(StandardField.values())
                     .filter(field -> field.getName().equalsIgnoreCase(name))
                     .findAny();
    }

    @Override
    public Set<FieldProperty> getProperties() {
        return Collections.unmodifiableSet(properties);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isStandardField() {
        return true;
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
