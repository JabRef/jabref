package org.jabref.model.entry;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.IEEEField;
import org.jabref.model.entry.field.StandardField;

/**
 * This class defines entry types for biblatex support.
 * @see <a href="http://mirrors.concertpass.com/tex-archive/macros/latex/contrib/biblatex/doc/biblatex.pdf">biblatex documentation</a>
 */
public class BiblatexEntryTypes {

    public static final BiblatexEntryType ARTICLE = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                StandardField.SUBTITLE, StandardField.EDITOR, StandardField.SERIES, StandardField.VOLUME, StandardField.NUMBER,
                StandardField.EID, StandardField.ISSUE, StandardField.PAGES, StandardField.NOTE, StandardField.ISSN, StandardField.DOI,
                StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.JOURNALTITLE, StandardField.DATE);
            addAllOptional(StandardField.TRANSLATOR, StandardField.ANNOTATOR, StandardField.COMMENTATOR, StandardField.SUBTITLE,
                    StandardField.TITLEADDON, StandardField.EDITOR, StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC,
                    StandardField.JOURNALSUBTITLE, StandardField.ISSUETITLE, StandardField.ISSUESUBTITLE, StandardField.LANGUAGE,
                    StandardField.ORIGLANGUAGE, StandardField.SERIES, StandardField.VOLUME, StandardField.NUMBER, StandardField.EID,
                    StandardField.ISSUE, StandardField.PAGES, StandardField.VERSION, StandardField.NOTE,
                    StandardField.ISSN, StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Article";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType BOOK = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(StandardField.EDITOR,
                StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE, StandardField.MAINSUBTITLE,
                StandardField.MAINTITLEADDON, StandardField.VOLUME, StandardField.EDITION, StandardField.PUBLISHER, StandardField.ISBN,
                StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.DATE);
            addAllOptional(StandardField.EDITOR, StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC,
                    StandardField.TRANSLATOR, StandardField.ANNOTATOR, StandardField.COMMENTATOR, StandardField.INTRODUCTION,
                    StandardField.FOREWORD, StandardField.AFTERWORD, StandardField.SUBTITLE, StandardField.TITLEADDON,
                    StandardField.MAINTITLE, StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.LANGUAGE,
                    StandardField.ORIGLANGUAGE, StandardField.VOLUME, StandardField.PART, StandardField.EDITION, StandardField.VOLUMES,
                    StandardField.SERIES, StandardField.NUMBER, StandardField.NOTE, StandardField.PUBLISHER, StandardField.LOCATION,
                    StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.ADDENDUM,
                    StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE,
                    StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Book";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType MVBOOK = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(
                Arrays.asList(StandardField.EDITOR, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.EDITION,
                        StandardField.PUBLISHER, StandardField.ISBN, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                        StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.DATE);
            addAllOptional(StandardField.EDITOR, StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC,
                    StandardField.TRANSLATOR, StandardField.ANNOTATOR, StandardField.COMMENTATOR, StandardField.INTRODUCTION,
                    StandardField.FOREWORD, StandardField.AFTERWORD, StandardField.SUBTITLE, StandardField.TITLEADDON,
                    StandardField.LANGUAGE, StandardField.ORIGLANGUAGE, StandardField.EDITION, StandardField.VOLUMES, StandardField.SERIES,
                    StandardField.NUMBER, StandardField.NOTE, StandardField.PUBLISHER, StandardField.LOCATION, StandardField.ISBN,
                    StandardField.PAGETOTAL, StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "MvBook";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType INBOOK = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(
                Arrays.asList(StandardField.BOOKAUTHOR, StandardField.EDITOR, StandardField.SUBTITLE, StandardField.TITLEADDON,
                        StandardField.MAINTITLE, StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.BOOKSUBTITLE,
                        StandardField.BOOKTITLEADDON, StandardField.VOLUME, StandardField.EDITION, StandardField.PUBLISHER,
                        StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES, StandardField.DOI, StandardField.EPRINT,
                        StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.BOOKTITLE, StandardField.DATE);
            addAllOptional(StandardField.BOOKAUTHOR, StandardField.EDITOR, StandardField.EDITORA, StandardField.EDITORB,
                    StandardField.EDITORC, StandardField.TRANSLATOR, StandardField.ANNOTATOR, StandardField.COMMENTATOR,
                    StandardField.INTRODUCTION, StandardField.FOREWORD, StandardField.AFTERWORD, StandardField.SUBTITLE,
                    StandardField.TITLEADDON, StandardField.MAINTITLE, StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON,
                    StandardField.BOOKSUBTITLE, StandardField.BOOKTITLEADDON, StandardField.LANGUAGE, StandardField.ORIGLANGUAGE,
                    StandardField.VOLUME, StandardField.PART, StandardField.EDITION, StandardField.VOLUMES, StandardField.SERIES,
                    StandardField.NUMBER, StandardField.NOTE, StandardField.PUBLISHER, StandardField.LOCATION, StandardField.ISBN,
                    StandardField.CHAPTER, StandardField.PAGES, StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI,
                    StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "InBook";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType BOOKINBOOK = new BiblatexEntryType() {

        @Override
        public String getName() {
            return "BookInBook";
        }

        // Same fields as "INBOOK" according to Biblatex 1.0:
        @Override
        public Set<Field> getRequiredFields() {
            return BiblatexEntryTypes.INBOOK.getRequiredFields();
        }

        @Override
        public Set<String> getOptionalFields() {
            return BiblatexEntryTypes.INBOOK.getOptionalFields();
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return BiblatexEntryTypes.INBOOK.getPrimaryOptionalFields();
        }
    };

    public static final BiblatexEntryType SUPPBOOK = new BiblatexEntryType() {

        @Override
        public String getName() {
            return "SuppBook";
        }

        // Same fields as "INBOOK" according to Biblatex 1.0:
        @Override
        public Set<Field> getRequiredFields() {
            return BiblatexEntryTypes.INBOOK.getRequiredFields();
        }

        @Override
        public Set<String> getOptionalFields() {
            return BiblatexEntryTypes.INBOOK.getOptionalFields();
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return BiblatexEntryTypes.INBOOK.getPrimaryOptionalFields();
        }
    };

    public static final BiblatexEntryType BOOKLET = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections
                .unmodifiableSet(new LinkedHashSet<>(Arrays.asList(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.HOWPUBLISHED,
                        StandardField.CHAPTER, StandardField.PAGES, StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS,
                        StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(FieldFactory.orFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.DATE);
            addAllOptional(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.LANGUAGE, StandardField.HOWPUBLISHED,
                    StandardField.TYPE, StandardField.NOTE, StandardField.LOCATION, StandardField.CHAPTER, StandardField.PAGES,
                    StandardField.PAGETOTAL, StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Booklet";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType COLLECTION = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                StandardField.TRANSLATOR, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE,
                StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.VOLUME, StandardField.EDITION,
                StandardField.PUBLISHER, StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES, StandardField.DOI,
                StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.EDITOR, StandardField.TITLE, StandardField.DATE);
            addAllOptional(StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC, StandardField.TRANSLATOR,
                    StandardField.ANNOTATOR, StandardField.COMMENTATOR, StandardField.INTRODUCTION, StandardField.FOREWORD,
                    StandardField.AFTERWORD, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE,
                    StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.LANGUAGE, StandardField.ORIGLANGUAGE,
                    StandardField.VOLUME, StandardField.PART, StandardField.EDITION, StandardField.VOLUMES, StandardField.SERIES,
                    StandardField.NUMBER, StandardField.NOTE, StandardField.PUBLISHER, StandardField.LOCATION, StandardField.ISBN,
                    StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.ADDENDUM, StandardField.PUBSTATE,
                    StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL,
                    StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Collection";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType MVCOLLECTION = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections
                .unmodifiableSet(new LinkedHashSet<>(Arrays.asList(StandardField.TRANSLATOR, StandardField.SUBTITLE, StandardField.TITLEADDON,
                        StandardField.EDITION, StandardField.PUBLISHER, StandardField.ISBN, StandardField.DOI, StandardField.EPRINT,
                        StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.EDITOR, StandardField.TITLE, StandardField.DATE);
            addAllOptional(StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC, StandardField.TRANSLATOR,
                    StandardField.ANNOTATOR, StandardField.COMMENTATOR, StandardField.INTRODUCTION, StandardField.FOREWORD,
                    StandardField.AFTERWORD, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.LANGUAGE,
                    StandardField.ORIGLANGUAGE, StandardField.EDITION, StandardField.VOLUMES, StandardField.SERIES, StandardField.NUMBER,
                    StandardField.NOTE, StandardField.PUBLISHER, StandardField.LOCATION, StandardField.ISBN, StandardField.PAGETOTAL,
                    StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS,
                    StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "MvCollection";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType INCOLLECTION = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections
                .unmodifiableSet(new LinkedHashSet<>(Arrays.asList(StandardField.TRANSLATOR, StandardField.SUBTITLE, StandardField.TITLEADDON,
                        StandardField.MAINTITLE, StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.BOOKSUBTITLE,
                        StandardField.BOOKTITLEADDON, StandardField.VOLUME, StandardField.EDITION, StandardField.PUBLISHER,
                        StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES, StandardField.DOI, StandardField.EPRINT,
                        StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.BOOKTITLE, StandardField.DATE);
            addAllOptional(StandardField.EDITOR, StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC,
                    StandardField.TRANSLATOR, StandardField.ANNOTATOR, StandardField.COMMENTATOR, StandardField.INTRODUCTION,
                    StandardField.FOREWORD, StandardField.AFTERWORD, StandardField.SUBTITLE, StandardField.TITLEADDON,
                    StandardField.MAINTITLE, StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.BOOKSUBTITLE,
                    StandardField.BOOKTITLEADDON, StandardField.LANGUAGE, StandardField.ORIGLANGUAGE, StandardField.VOLUME,
                    StandardField.PART, StandardField.EDITION, StandardField.VOLUMES, StandardField.SERIES, StandardField.NUMBER,
                    StandardField.NOTE, StandardField.PUBLISHER, StandardField.LOCATION, StandardField.ISBN, StandardField.CHAPTER,
                    StandardField.PAGES, StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "InCollection";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType SUPPCOLLECTION = new BiblatexEntryType() {

        @Override
        public String getName() {
            return "SuppCollection";
        }

        // Treated as alias of "INCOLLECTION" according to Biblatex 1.0:
        @Override
        public Set<Field> getRequiredFields() {
            return BiblatexEntryTypes.INCOLLECTION.getRequiredFields();
        }

        @Override
        public Set<String> getOptionalFields() {
            return BiblatexEntryTypes.INCOLLECTION.getOptionalFields();
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return BiblatexEntryTypes.INCOLLECTION.getPrimaryOptionalFields();
        }
    };

    public static final BiblatexEntryType MANUAL = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(
                new LinkedHashSet<>(Arrays.asList(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.EDITION, StandardField.PUBLISHER,
                        StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES, StandardField.DOI, StandardField.EPRINT,
                        StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(FieldFactory.orFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.DATE);
            addAllOptional(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.LANGUAGE, StandardField.EDITION,
                    StandardField.TYPE, StandardField.SERIES, StandardField.NUMBER, StandardField.VERSION, StandardField.NOTE,
                    StandardField.ORGANIZATION, StandardField.PUBLISHER, StandardField.LOCATION, StandardField.ISBN, StandardField.CHAPTER,
                    StandardField.PAGES, StandardField.PAGETOTAL, StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI,
                    StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Manual";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType MISC = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.HOWPUBLISHED, StandardField.LOCATION, StandardField.DOI,
                StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(FieldFactory.orFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.DATE);
            addAllOptional(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.LANGUAGE, StandardField.HOWPUBLISHED,
                    StandardField.TYPE, StandardField.VERSION, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.LOCATION,
                    StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Misc";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType ONLINE = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.URLDATE)));

        {
            addAllRequired(FieldFactory.orFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.DATE, StandardField.URL);
            addAllOptional(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.LANGUAGE, StandardField.VERSION,
                    StandardField.NOTE, StandardField.ORGANIZATION, StandardField.ADDENDUM, StandardField.PUBSTATE,
                    StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Online";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType PATENT = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(StandardField.HOLDER,
                StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS,
                StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.NUMBER, StandardField.DATE);
            addAllOptional(StandardField.HOLDER, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.TYPE,
                    StandardField.VERSION, StandardField.LOCATION, StandardField.NOTE, StandardField.ADDENDUM,
                    StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE,
                    StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Patent";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType PERIODICAL = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                StandardField.SUBTITLE, StandardField.ISSUETITLE, StandardField.ISSUESUBTITLE, StandardField.ISSN, StandardField.DOI,
                StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.EDITOR, StandardField.TITLE, StandardField.DATE);
            addAllOptional(StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC, StandardField.SUBTITLE,
                    StandardField.ISSUETITLE, StandardField.ISSUESUBTITLE, StandardField.LANGUAGE, StandardField.SERIES,
                    StandardField.VOLUME, StandardField.NUMBER, StandardField.ISSUE, StandardField.NOTE,
                    StandardField.ISSN, StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Periodical";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType SUPPPERIODICAL = new BiblatexEntryType() {

        @Override
        public String getName() {
            return "SuppPeriodical";
        }

        // Treated as alias of "ARTICLE" according to Biblatex 1.0:
        @Override
        public Set<Field> getRequiredFields() {
            return BiblatexEntryTypes.ARTICLE.getRequiredFields();
        }

        @Override
        public Set<String> getOptionalFields() {
            return BiblatexEntryTypes.ARTICLE.getOptionalFields();
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return BiblatexEntryTypes.ARTICLE.getPrimaryOptionalFields();
        }
    };

    public static final BiblatexEntryType PROCEEDINGS = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE, StandardField.MAINSUBTITLE,
                StandardField.MAINTITLEADDON, StandardField.EVENTTITLE, StandardField.VOLUME, StandardField.PUBLISHER, StandardField.ISBN,
                StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.TITLE, StandardField.DATE);
            addAllOptional(StandardField.EDITOR, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE,
                    StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.EVENTTITLE, StandardField.EVENTTITLEADDON,
                    StandardField.EVENTDATE, StandardField.VENUE, StandardField.LANGUAGE, StandardField.VOLUME, StandardField.PART,
                    StandardField.VOLUMES, StandardField.SERIES, StandardField.NUMBER, StandardField.NOTE, StandardField.ORGANIZATION,
                    StandardField.PUBLISHER, StandardField.LOCATION, StandardField.ISBN,
                    StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.ADDENDUM, StandardField.PUBSTATE,
                    StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL,
                    StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Proceedings";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType MVPROCEEDINGS = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE, StandardField.MAINSUBTITLE,
                StandardField.MAINTITLEADDON, StandardField.EVENTTITLE, StandardField.VOLUME, StandardField.PUBLISHER, StandardField.ISBN,
                StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.TITLE, StandardField.DATE);
            addAllOptional(StandardField.EDITOR, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.EVENTTITLE,
                    StandardField.EVENTTITLEADDON, StandardField.EVENTDATE, StandardField.VENUE, StandardField.LANGUAGE,
                    StandardField.VOLUMES, StandardField.SERIES, StandardField.NUMBER, StandardField.NOTE, StandardField.ORGANIZATION,
                    StandardField.PUBLISHER, StandardField.LOCATION, StandardField.ISBN, StandardField.PAGETOTAL,
                    StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS,
                    StandardField.EPRINTTYPE, StandardField.URL,
                    StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "MvProceedings";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType INPROCEEDINGS = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections
                .unmodifiableSet(new LinkedHashSet<>(Arrays.asList(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE,
                        StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.BOOKSUBTITLE,
                        StandardField.BOOKTITLEADDON, StandardField.EVENTTITLE, StandardField.VOLUME, StandardField.PUBLISHER,
                        StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES, StandardField.DOI, StandardField.EPRINT,
                        StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.BOOKTITLE, StandardField.DATE);
            addAllOptional(StandardField.EDITOR, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.MAINTITLE,
                    StandardField.MAINSUBTITLE, StandardField.MAINTITLEADDON, StandardField.BOOKSUBTITLE, StandardField.BOOKTITLEADDON,
                    StandardField.EVENTTITLE, StandardField.EVENTTITLEADDON, StandardField.EVENTDATE, StandardField.VENUE,
                    StandardField.LANGUAGE, StandardField.VOLUME, StandardField.PART, StandardField.VOLUMES, StandardField.SERIES,
                    StandardField.NUMBER, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.PUBLISHER, StandardField.LOCATION,
                    StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES, StandardField.ADDENDUM,
                    StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE,
                    StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "InProceedings";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType REFERENCE = new BiblatexEntryType() {

        @Override
        public String getName() {
            return "Reference";
        }

        // Treated as alias of "COLLECTION" according to Biblatex 1.0:
        @Override
        public Set<Field> getRequiredFields() {
            return BiblatexEntryTypes.COLLECTION.getRequiredFields();
        }

        @Override
        public Set<String> getOptionalFields() {
            return BiblatexEntryTypes.COLLECTION.getOptionalFields();
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return BiblatexEntryTypes.COLLECTION.getPrimaryOptionalFields();
        }
    };

    public static final BiblatexEntryType MVREFERENCE = new BiblatexEntryType() {

        @Override
        public String getName() {
            return "MvReference";
        }

        // Treated as alias of "MVCOLLECTION" according to Biblatex 1.0:
        @Override
        public Set<Field> getRequiredFields() {
            return BiblatexEntryTypes.MVCOLLECTION.getRequiredFields();
        }

        @Override
        public Set<String> getOptionalFields() {
            return BiblatexEntryTypes.MVCOLLECTION.getOptionalFields();
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return BiblatexEntryTypes.MVCOLLECTION.getPrimaryOptionalFields();
        }
    };

    public static final BiblatexEntryType INREFERENCE = new BiblatexEntryType() {

        @Override
        public String getName() {
            return "InReference";
        }

        // Treated as alias of "INCOLLECTION" according to Biblatex 1.0:
        @Override
        public Set<Field> getRequiredFields() {
            return BiblatexEntryTypes.INCOLLECTION.getRequiredFields();
        }

        @Override
        public Set<String> getOptionalFields() {
            return BiblatexEntryTypes.INCOLLECTION.getOptionalFields();
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return BiblatexEntryTypes.INCOLLECTION.getPrimaryOptionalFields();
        }
    };

    public static final BiblatexEntryType REPORT = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(
                Arrays.asList(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.NUMBER, StandardField.ISRN,
                        StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                        StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.TYPE, StandardField.INSTITUTION, StandardField.DATE);
            addAllOptional(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.LANGUAGE, StandardField.NUMBER,
                    StandardField.VERSION, StandardField.NOTE, StandardField.LOCATION, StandardField.ISRN,
                    StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.ADDENDUM, StandardField.PUBSTATE,
                    StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL,
                    StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Report";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType SET = new BiblatexEntryType() {

        {
            addAllRequired(StandardField.ENTRYSET, StandardField.CROSSREF);
        }

        @Override
        public String getName() {
            return "Set";
        }
    };

    public static final BiblatexEntryType THESIS = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections
                .unmodifiableSet(new LinkedHashSet<>(Arrays.asList(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.CHAPTER,
                        StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS,
                        StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.TYPE, StandardField.INSTITUTION, StandardField.DATE);
            addAllOptional(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.LANGUAGE, StandardField.NOTE,
                    StandardField.LOCATION, StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES,
                    StandardField.PAGETOTAL, StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Thesis";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType UNPUBLISHED = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections
                .unmodifiableSet(new LinkedHashSet<>(Arrays.asList(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.HOWPUBLISHED,
                        StandardField.PUBSTATE, StandardField.URL, StandardField.URLDATE)));

        {
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.DATE);
            addAllOptional(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.LANGUAGE, StandardField.HOWPUBLISHED,
                    StandardField.NOTE, StandardField.LOCATION, StandardField.ADDENDUM, StandardField.PUBSTATE,
                    StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "Unpublished";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    // === Type aliases: ===

    public static final BiblatexEntryType CONFERENCE = new BiblatexEntryType() {

        @Override
        public String getName() {
            return "Conference";
        }

        // Treated as alias of "INPROCEEDINGS" according to Biblatex 1.0:
        @Override
        public Set<Field> getRequiredFields() {
            return BiblatexEntryTypes.INPROCEEDINGS.getRequiredFields();
        }

        @Override
        public Set<String> getOptionalFields() {
            return BiblatexEntryTypes.INPROCEEDINGS.getOptionalFields();
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return BiblatexEntryTypes.INPROCEEDINGS.getPrimaryOptionalFields();
        }
    };

    public static final BiblatexEntryType ELECTRONIC = new BiblatexEntryType() {

        @Override
        public String getName() {
            return "Electronic";
        }

        // Treated as alias of "ONLINE" according to Biblatex 1.0:
        @Override
        public Set<Field> getRequiredFields() {
            return BiblatexEntryTypes.ONLINE.getRequiredFields();
        }

        @Override
        public Set<String> getOptionalFields() {
            return BiblatexEntryTypes.ONLINE.getOptionalFields();
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return BiblatexEntryTypes.ONLINE.getPrimaryOptionalFields();
        }
    };

    public static final BiblatexEntryType MASTERSTHESIS = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections
                .unmodifiableSet(new LinkedHashSet<>(Arrays.asList(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.TYPE,
                        StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                        StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            // Treated as alias of "THESIS", except StandardField.TYPE field is optional
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.INSTITUTION, StandardField.DATE);
            addAllOptional(StandardField.TYPE, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.LANGUAGE, StandardField.NOTE,
                    StandardField.LOCATION, StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES,
                    StandardField.PAGETOTAL, StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "MastersThesis";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType PHDTHESIS = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections
                .unmodifiableSet(new LinkedHashSet<>(Arrays.asList(StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.TYPE,
                        StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                        StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            // Treated as alias of "THESIS", except StandardField.TYPE field is optional
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.INSTITUTION, StandardField.DATE);
            addAllOptional(StandardField.TYPE, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.LANGUAGE, StandardField.NOTE,
                    StandardField.LOCATION, StandardField.ISBN, StandardField.CHAPTER, StandardField.PAGES,
                    StandardField.PAGETOTAL, StandardField.ADDENDUM, StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT,
                    StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "PhdThesis";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType TECHREPORT = new BiblatexEntryType() {

        private final Set<String> primaryOptionalFields = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.TYPE, StandardField.NUMBER, StandardField.ISRN,
                StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.DOI, StandardField.EPRINT,
                StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE)));

        {
            // Treated as alias of "REPORT", except StandardField.TYPE field is optional
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.INSTITUTION, StandardField.DATE);
            addAllOptional(StandardField.TYPE, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.LANGUAGE,
                    StandardField.NUMBER, StandardField.VERSION, StandardField.NOTE, StandardField.LOCATION,
                    StandardField.ISRN, StandardField.CHAPTER, StandardField.PAGES, StandardField.PAGETOTAL, StandardField.ADDENDUM,
                    StandardField.PUBSTATE, StandardField.DOI, StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE,
                    StandardField.URL, StandardField.URLDATE);
        }

        @Override
        public String getName() {
            return "TechReport";
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BiblatexEntryType WWW = new BiblatexEntryType() {

        @Override
        public String getName() {
            return "WWW";
        }

        // Treated as alias of "ONLINE" according to Biblatex 1.0:
        @Override
        public Set<Field> getRequiredFields() {
            return BiblatexEntryTypes.ONLINE.getRequiredFields();
        }

        @Override
        public Set<String> getOptionalFields() {
            return BiblatexEntryTypes.ONLINE.getOptionalFields();
        }

        @Override
        public Set<String> getPrimaryOptionalFields() {
            return BiblatexEntryTypes.ONLINE.getPrimaryOptionalFields();
        }
    };

    /**
     * This type is used for IEEEtran.bst to control various
     * be repeated or not. Not a very elegant solution, but it works...
     */
    public static final BiblatexEntryType IEEETRANBSTCTL = new BiblatexEntryType() {

        {
            addAllOptional(IEEEField.CTLUSE_ARTICLE_NUMBER, IEEEField.CTLUSE_PAPER, IEEEField.CTLUSE_FORCED_ETAL,
                    IEEEField.CTLUSE_URL, IEEEField.CTLMAX_NAMES_FORCED_ETAL, IEEEField.CTLNAMES_SHOW_ETAL,
                    IEEEField.CTLUSE_ALT_SPACING, IEEEField.CTLALT_STRETCH_FACTOR, IEEEField.CTLDASH_REPEATED_NAMES,
                    IEEEField.CTLNAME_FORMAT_STRING, IEEEField.CTLNAME_LATEX_CMD, IEEEField.CTLNAME_URL_PREFIX);
        }

        @Override
        public String getName() {
            return "IEEEtranBSTCTL";
        }
    };

    public static final List<EntryType> ALL = Arrays.asList(ARTICLE, BOOK, MVBOOK, INBOOK, BOOKINBOOK, SUPPBOOK,
            BOOKLET, COLLECTION, MVCOLLECTION, INCOLLECTION, SUPPCOLLECTION, MANUAL, MISC, ONLINE, PATENT, PERIODICAL,
            SUPPPERIODICAL, PROCEEDINGS, MVPROCEEDINGS, INPROCEEDINGS, REFERENCE, MVREFERENCE, INREFERENCE, REPORT, SET,
            THESIS, UNPUBLISHED, CONFERENCE, ELECTRONIC, MASTERSTHESIS, PHDTHESIS, TECHREPORT, WWW, IEEETRANBSTCTL);


    private BiblatexEntryTypes() {
    }

    public static Optional<EntryType> getType(String name) {
        return ALL.stream().filter(e -> e.getName().equalsIgnoreCase(name)).findFirst();
    }
}
