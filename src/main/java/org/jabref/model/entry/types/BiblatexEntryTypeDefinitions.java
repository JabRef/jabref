package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;

/**
 * This class defines entry types for biblatex support.
 * It is based on the <a href="https://texdoc.org/serve/biblatex.pdf/0">biblatex documentation</a>
 * <p>
 * The definitions for BibTeX are done at {@link BibtexEntryTypeDefinitions}
 */
public class BiblatexEntryTypeDefinitions {

    private static final BibEntryType ARTICLE = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Article)
            .withRequiredFields(
                    StandardField.AUTHOR, StandardField.TITLE, StandardField.JOURNALTITLE, StandardField.DATE)
            .withImportantFields(
                    StandardField.SUBTITLE, StandardField.EDITOR, StandardField.SERIES, StandardField.VOLUME, StandardField.NUMBER,
                    StandardField.EID, StandardField.ISSUE, StandardField.PAGES, StandardField.NOTE, StandardField.ISSN, StandardField.DOI,
                    StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE, StandardField.LANGUAGEID)
            .withDetailFields(
                    StandardField.TRANSLATOR, StandardField.ANNOTATOR, StandardField.COMMENTATOR,
                    StandardField.TITLEADDON, StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC,
                    StandardField.JOURNALSUBTITLE, StandardField.ISSUETITLE, StandardField.ISSUESUBTITLE, StandardField.LANGUAGE,
                    StandardField.ORIGLANGUAGE, StandardField.VERSION,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType BOOK = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Book)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.DATE)
            .withImportantFields(StandardField.EDITOR,
                    StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE, StandardField.MAINSUBTITLE,
                    StandardField.MAINTITLEADDON, StandardField.VOLUME, StandardField.EDITION, StandardField.PUBLISHER, StandardField.ISBN,
                    StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE, StandardField.LANGUAGEID)
            .withDetailFields(StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC,
                    StandardField.TRANSLATOR, StandardField.ANNOTATOR, StandardField.COMMENTATOR, StandardField.INTRODUCTION,
                    StandardField.FOREWORD, StandardField.AFTERWORD,
                    StandardField.LANGUAGE,
                    StandardField.ORIGLANGUAGE, StandardField.PART, StandardField.VOLUMES,
                    StandardField.SERIES, StandardField.NUMBER, StandardField.NOTE, StandardField.LOCATION,
                    StandardField.ADDENDUM,
                    StandardField.PUBSTATE)
            .build();

    private static final BibEntryType MVBOOK = new BibEntryTypeBuilder()
            .withType(StandardEntryType.MvBook)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.DATE)
            .withImportantFields(StandardField.EDITOR, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.EDITION,
                    StandardField.PUBLISHER, StandardField.ISBN, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE, StandardField.LANGUAGEID)
            .withDetailFields(StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC,
                    StandardField.TRANSLATOR, StandardField.ANNOTATOR, StandardField.COMMENTATOR, StandardField.INTRODUCTION,
                    StandardField.FOREWORD, StandardField.AFTERWORD,
                    StandardField.LANGUAGE, StandardField.ORIGLANGUAGE, StandardField.VOLUMES, StandardField.SERIES,
                    StandardField.NUMBER, StandardField.NOTE, StandardField.LOCATION,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType INBOOK = new BibEntryTypeBuilder()
            .withType(StandardEntryType.InBook)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.BOOKTITLE, StandardField.DATE)
            .withImportantFields(
                    StandardField.BOOKAUTHOR, StandardField.EDITOR, StandardField.SUBTITLE, StandardField.TITLEADDON,
                    StandardField.MAINTITLE, StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.BOOKSUBTITLE,
                    StandardField.BOOKTITLEADDON, StandardField.VOLUME, StandardField.EDITION, StandardField.PUBLISHER,
                    StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE, StandardField.LANGUAGEID)
            .withDetailFields(StandardField.EDITORA, StandardField.EDITORB,
                    StandardField.EDITORC, StandardField.TRANSLATOR, StandardField.ANNOTATOR, StandardField.COMMENTATOR,
                    StandardField.INTRODUCTION, StandardField.FOREWORD, StandardField.AFTERWORD,
                    StandardField.LANGUAGE, StandardField.ORIGLANGUAGE,
                    StandardField.PART, StandardField.VOLUMES, StandardField.SERIES,
                    StandardField.NUMBER, StandardField.NOTE, StandardField.LOCATION,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType BOOKINBOOK = new BibEntryTypeBuilder()
            .withType(StandardEntryType.BookInBook)
            .withRequiredFields(INBOOK.getRequiredFields())
            .withImportantFields(INBOOK.getImportantOptionalFields())
            .withDetailFields(INBOOK.getDetailOptionalFields())
            .build();

    private static final BibEntryType SUPPBOOK = new BibEntryTypeBuilder()
            .withType(StandardEntryType.SuppBook)
            .withImportantFields(INBOOK.getImportantOptionalFields())
            .withDetailFields(INBOOK.getDetailOptionalFields())
            .withRequiredFields(INBOOK.getRequiredFields())
            .build();

    private static final BibEntryType BOOKLET = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Booklet)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.DATE)
            .withImportantFields(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.HOWPUBLISHED,
                    StandardField.CHAPTER, StandardField.PAGES, StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS,
                    StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE, StandardField.LANGUAGEID)
            .withDetailFields(StandardField.LANGUAGE,
                    StandardField.TYPE, StandardField.NOTE, StandardField.LOCATION,
                    StandardField.PAGETOTAL, StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType COLLECTION = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Collection)
            .withRequiredFields(StandardField.EDITOR, StandardField.TITLE, StandardField.DATE)
            .withImportantFields(
                    StandardField.TRANSLATOR, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE,
                    StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.VOLUME, StandardField.EDITION,
                    StandardField.PUBLISHER, StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES, StandardField.DOI,
                    StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC,
                    StandardField.ANNOTATOR, StandardField.COMMENTATOR, StandardField.INTRODUCTION, StandardField.FOREWORD,
                    StandardField.AFTERWORD,
                    StandardField.LANGUAGE, StandardField.ORIGLANGUAGE,
                    StandardField.PART, StandardField.VOLUMES, StandardField.SERIES,
                    StandardField.NUMBER, StandardField.NOTE, StandardField.LOCATION,
                    StandardField.PAGETOTAL, StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType MVCOLLECTION = new BibEntryTypeBuilder()
            .withType(StandardEntryType.MvCollection)
            .withRequiredFields(StandardField.EDITOR, StandardField.TITLE, StandardField.DATE)
            .withImportantFields(StandardField.TRANSLATOR, StandardField.SUBTITLE, StandardField.TITLEADDON,
                    StandardField.EDITION, StandardField.PUBLISHER, StandardField.ISBN, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC,
                    StandardField.ANNOTATOR, StandardField.COMMENTATOR, StandardField.INTRODUCTION, StandardField.FOREWORD,
                    StandardField.AFTERWORD, StandardField.LANGUAGE,
                    StandardField.ORIGLANGUAGE, StandardField.VOLUMES, StandardField.SERIES, StandardField.NUMBER,
                    StandardField.NOTE, StandardField.LOCATION, StandardField.PAGETOTAL,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType INCOLLECTION = new BibEntryTypeBuilder()
            .withType(StandardEntryType.InCollection)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.BOOKTITLE, StandardField.DATE)
            .withImportantFields(StandardField.TRANSLATOR, StandardField.SUBTITLE, StandardField.TITLEADDON,
                    StandardField.MAINTITLE, StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.BOOKSUBTITLE,
                    StandardField.BOOKTITLEADDON, StandardField.VOLUME, StandardField.EDITION, StandardField.PUBLISHER,
                    StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.EDITOR, StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC,
                    StandardField.ANNOTATOR, StandardField.COMMENTATOR, StandardField.INTRODUCTION,
                    StandardField.FOREWORD, StandardField.AFTERWORD,
                    StandardField.LANGUAGE, StandardField.ORIGLANGUAGE,
                    StandardField.PART, StandardField.VOLUMES, StandardField.SERIES, StandardField.NUMBER,
                    StandardField.NOTE, StandardField.LOCATION,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType SUPPCOLLECTION = new BibEntryTypeBuilder()
            .withType(StandardEntryType.SuppCollection)
            .withRequiredFields(INCOLLECTION.getRequiredFields())
            .withImportantFields(INCOLLECTION.getImportantOptionalFields())
            .withDetailFields(INCOLLECTION.getDetailOptionalFields())
            .build();

    private static final BibEntryType MANUAL = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Manual)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.DATE)
            .withImportantFields(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.EDITION, StandardField.PUBLISHER,
                    StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.LANGUAGE,
                    StandardField.TYPE, StandardField.SERIES, StandardField.NUMBER, StandardField.VERSION, StandardField.NOTE,
                    StandardField.ORGANIZATION, StandardField.LOCATION,
                    StandardField.PAGETOTAL, StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType MISC = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Misc)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.DATE)
            .withImportantFields(
                    StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.HOWPUBLISHED, StandardField.LOCATION, StandardField.DOI,
                    StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.LANGUAGE,
                    StandardField.TYPE, StandardField.VERSION, StandardField.NOTE, StandardField.ORGANIZATION,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType ONLINE = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Online)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.DATE, StandardField.URL)
            .withImportantFields(
                    StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.URLDATE)
            .withDetailFields(StandardField.LANGUAGE, StandardField.VERSION,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType PATENT = new BibEntryTypeBuilder()
            .withType(IEEETranEntryType.Patent)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.NUMBER, StandardField.DATE)
            .withImportantFields(StandardField.HOLDER,
                    StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS,
                    StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.TYPE,
                    StandardField.VERSION, StandardField.LOCATION, StandardField.NOTE, StandardField.ADDENDUM,
                    StandardField.PUBSTATE)
            .build();

    private static final BibEntryType PERIODICAL = new BibEntryTypeBuilder()
            .withType(IEEETranEntryType.Periodical)
            .withRequiredFields(StandardField.EDITOR, StandardField.TITLE, StandardField.DATE)
            .withImportantFields(
                    StandardField.SUBTITLE, StandardField.ISSUETITLE, StandardField.ISSUESUBTITLE, StandardField.ISSN, StandardField.DOI,
                    StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC,
                    StandardField.LANGUAGE, StandardField.SERIES,
                    StandardField.VOLUME, StandardField.NUMBER, StandardField.ISSUE, StandardField.NOTE,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType SUPPPERIODICAL = new BibEntryTypeBuilder()
            .withType(StandardEntryType.SuppPeriodical)
            .withRequiredFields(ARTICLE.getRequiredFields())
            .withImportantFields(ARTICLE.getImportantOptionalFields())
            .withDetailFields(ARTICLE.getDetailOptionalFields())
            .build();

    private static final BibEntryType PROCEEDINGS = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Proceedings)
            .withRequiredFields(StandardField.TITLE, StandardField.DATE)
            .withImportantFields(
                    StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE, StandardField.MAINSUBTITLE,
                    StandardField.MAINTITLEADDON, StandardField.EVENTTITLE, StandardField.VOLUME, StandardField.PUBLISHER, StandardField.ISBN,
                    StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.EDITOR, StandardField.EVENTTITLEADDON,
                    StandardField.EVENTDATE, StandardField.VENUE, StandardField.LANGUAGE, StandardField.PART,
                    StandardField.VOLUMES, StandardField.SERIES, StandardField.NUMBER, StandardField.NOTE, StandardField.ORGANIZATION,
                    StandardField.LOCATION,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType MVPROCEEDINGS = new BibEntryTypeBuilder()
            .withType(StandardEntryType.MvProceedings)
            .withRequiredFields(StandardField.TITLE, StandardField.DATE)
            .withImportantFields(
                    StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE, StandardField.MAINSUBTITLE,
                    StandardField.MAINTITLEADDON, StandardField.EVENTTITLE, StandardField.VOLUME, StandardField.PUBLISHER, StandardField.ISBN,
                    StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.EDITOR,
                    StandardField.EVENTTITLEADDON, StandardField.EVENTDATE, StandardField.VENUE, StandardField.LANGUAGE,
                    StandardField.VOLUMES, StandardField.SERIES, StandardField.NUMBER, StandardField.NOTE, StandardField.ORGANIZATION,
                    StandardField.LOCATION,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType INPROCEEDINGS = new BibEntryTypeBuilder()
            .withType(StandardEntryType.InProceedings)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.BOOKTITLE, StandardField.DATE)
            .withImportantFields(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE,
                    StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.BOOKSUBTITLE,
                    StandardField.BOOKTITLEADDON, StandardField.EVENTTITLE, StandardField.VOLUME, StandardField.PUBLISHER,
                    StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.EDITOR,
                    StandardField.EVENTTITLEADDON, StandardField.EVENTDATE, StandardField.VENUE,
                    StandardField.LANGUAGE, StandardField.PART, StandardField.VOLUMES, StandardField.SERIES,
                    StandardField.NUMBER, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.LOCATION,
                    StandardField.ADDENDUM,
                    StandardField.PUBSTATE)
            .build();

    private static final BibEntryType REFERENCE = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Reference)
            .withRequiredFields(COLLECTION.getRequiredFields())
            .withImportantFields(COLLECTION.getImportantOptionalFields())
            .withDetailFields(COLLECTION.getDetailOptionalFields())
            .build();

    private static final BibEntryType MVREFERENCE = new BibEntryTypeBuilder()
            .withType(StandardEntryType.MvReference)
            .withRequiredFields(MVCOLLECTION.getRequiredFields())
            .withImportantFields(MVCOLLECTION.getImportantOptionalFields())
            .withDetailFields(MVCOLLECTION.getDetailOptionalFields())
            .build();

    private static final BibEntryType INREFERENCE = new BibEntryTypeBuilder()
            .withType(StandardEntryType.InReference)
            .withRequiredFields(INCOLLECTION.getRequiredFields())
            .withImportantFields(INCOLLECTION.getImportantOptionalFields())
            .withDetailFields(INCOLLECTION.getDetailOptionalFields())
            .build();

    private static final BibEntryType REPORT = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Report)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.TYPE, StandardField.INSTITUTION, StandardField.DATE)
            .withImportantFields(
                    StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.NUMBER, StandardField.ISRN,
                    StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.LANGUAGE,
                    StandardField.VERSION, StandardField.NOTE, StandardField.LOCATION,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType SET = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Set)
            .withRequiredFields(StandardField.ENTRYSET, StandardField.CROSSREF)
            .build();

    private static final BibEntryType THESIS = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Thesis)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.TYPE, StandardField.INSTITUTION, StandardField.DATE)
            .withImportantFields(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.CHAPTER,
                    StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS,
                    StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.LANGUAGE, StandardField.NOTE,
                    StandardField.LOCATION, StandardField.ISBN,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType UNPUBLISHED = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Unpublished)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.DATE)
            .withImportantFields(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.HOWPUBLISHED,
                    StandardField.PUBSTATE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.LANGUAGE,
                    StandardField.NOTE, StandardField.LOCATION, StandardField.ADDENDUM, StandardField.EVENTTITLE,
                    StandardField.EVENTDATE, StandardField.VENUE)
            .build();

    private static final BibEntryType CONFERENCE = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Conference)
            .withRequiredFields(INPROCEEDINGS.getRequiredFields())
            .withImportantFields(INPROCEEDINGS.getImportantOptionalFields())
            .withDetailFields(INPROCEEDINGS.getDetailOptionalFields())
            .build();

    private static final BibEntryType ELECTRONIC = new BibEntryTypeBuilder()
            .withType(IEEETranEntryType.Electronic)
            .withRequiredFields(ONLINE.getRequiredFields())
            .withImportantFields(ONLINE.getImportantOptionalFields())
            .withDetailFields(ONLINE.getDetailOptionalFields())
            .build();

    private static final BibEntryType MASTERSTHESIS = new BibEntryTypeBuilder()
            .withType(StandardEntryType.MastersThesis)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.INSTITUTION, StandardField.DATE)
            .withImportantFields(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.TYPE,
                    StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.LANGUAGE, StandardField.NOTE,
                    StandardField.LOCATION, StandardField.ISBN,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType PHDTHESIS = new BibEntryTypeBuilder()
            .withType(StandardEntryType.PhdThesis)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.INSTITUTION, StandardField.DATE)
            .withImportantFields(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.TYPE,
                    StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.LANGUAGE, StandardField.NOTE,
                    StandardField.LOCATION, StandardField.ISBN,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType TECHREPORT = new BibEntryTypeBuilder()
            .withType(StandardEntryType.TechReport)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.INSTITUTION, StandardField.DATE)
            .withImportantFields(
                    StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.TYPE, StandardField.NUMBER, StandardField.ISRN,
                    StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.LANGUAGE,
                    StandardField.VERSION, StandardField.NOTE, StandardField.LOCATION,
                    StandardField.ADDENDUM,
                    StandardField.PUBSTATE)
            .build();

    private static final BibEntryType WWW = new BibEntryTypeBuilder()
            .withType(StandardEntryType.WWW)
            .withRequiredFields(ONLINE.getRequiredFields())
            .withImportantFields(ONLINE.getImportantOptionalFields())
            .withDetailFields(ONLINE.getDetailOptionalFields())
            .build();

    private static final BibEntryType SOFTWARE = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Software)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.DATE)
            .withImportantFields(
                    StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.HOWPUBLISHED, StandardField.LOCATION, StandardField.DOI,
                    StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.LANGUAGE,
                    StandardField.TYPE, StandardField.VERSION, StandardField.NOTE, StandardField.ORGANIZATION,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    private static final BibEntryType DATASET = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Dataset)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.DATE)
            .withImportantFields(
                    StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.HOWPUBLISHED, StandardField.LOCATION, StandardField.DOI,
                    StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)
            .withDetailFields(StandardField.LANGUAGE, StandardField.EDITION,
                    StandardField.TYPE, StandardField.VERSION, StandardField.NOTE, StandardField.ORGANIZATION,
                    StandardField.ADDENDUM, StandardField.PUBSTATE)
            .build();

    public static final List<BibEntryType> ALL = Arrays.asList(ARTICLE, BOOK, MVBOOK, INBOOK, BOOKINBOOK, SUPPBOOK,
            BOOKLET, COLLECTION, MVCOLLECTION, INCOLLECTION, SUPPCOLLECTION, MANUAL, MISC, ONLINE, PATENT, PERIODICAL,
            SUPPPERIODICAL, PROCEEDINGS, MVPROCEEDINGS, INPROCEEDINGS, REFERENCE, MVREFERENCE, INREFERENCE, REPORT, SET,
            THESIS, UNPUBLISHED, CONFERENCE, ELECTRONIC, MASTERSTHESIS, PHDTHESIS, TECHREPORT, WWW, SOFTWARE, DATASET);

    public static final List<BibEntryType> RECOMMENDED = Arrays.asList(ARTICLE, BOOK, INPROCEEDINGS, REPORT, MISC);
}
