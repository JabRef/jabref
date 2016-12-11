package net.sf.jabref.model.entry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This class defines entry types for BibLatex support.
 * @see http://mirrors.concertpass.com/tex-archive/macros/latex/contrib/biblatex/doc/biblatex.pdf
 */
public class BibLatexEntryTypes {

    public static final BibLatexEntryType ARTICLE = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(
                FieldName.SUBTITLE, FieldName.EDITOR, FieldName.SERIES, FieldName.VOLUME, FieldName.NUMBER,
                FieldName.EID, FieldName.ISSUE, FieldName.PAGES, FieldName.NOTE, FieldName.ISSN, FieldName.DOI,
                FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.JOURNALTITLE,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.TRANSLATOR, FieldName.ANNOTATOR, FieldName.COMMENTATOR, FieldName.SUBTITLE,
                    FieldName.TITLEADDON, FieldName.EDITOR, FieldName.EDITORA, FieldName.EDITORB, FieldName.EDITORC,
                    FieldName.JOURNALSUBTITLE, FieldName.ISSUETITLE, FieldName.ISSUESUBTITLE, FieldName.LANGUAGE,
                    FieldName.ORIGLANGUAGE, FieldName.SERIES, FieldName.VOLUME, FieldName.NUMBER, FieldName.EID,
                    FieldName.ISSUE, FieldName.MONTH, FieldName.PAGES, FieldName.VERSION, FieldName.NOTE,
                    FieldName.ISSN, FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Article";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType BOOK = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(FieldName.EDITOR,
                FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.MAINTITLE, FieldName.MAINSUBTITLE,
                FieldName.MAINTITLEADDON, FieldName.VOLUME, FieldName.EDITION, FieldName.PUBLISHER, FieldName.ISBN,
                FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT,
                FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITOR, FieldName.EDITORA, FieldName.EDITORB, FieldName.EDITORC,
                    FieldName.TRANSLATOR, FieldName.ANNOTATOR, FieldName.COMMENTATOR, FieldName.INTRODUCTION,
                    FieldName.FOREWORD, FieldName.AFTERWORD, FieldName.SUBTITLE, FieldName.TITLEADDON,
                    FieldName.MAINTITLE, FieldName.MAINSUBTITLE, FieldName.MAINTITLEADDON, FieldName.LANGUAGE,
                    FieldName.ORIGLANGUAGE, FieldName.VOLUME, FieldName.PART, FieldName.EDITION, FieldName.VOLUMES,
                    FieldName.SERIES, FieldName.NUMBER, FieldName.NOTE, FieldName.PUBLISHER, FieldName.LOCATION,
                    FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, FieldName.ADDENDUM,
                    FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE,
                    FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Book";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType MVBOOK = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(
                Arrays.asList(FieldName.EDITOR, FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.EDITION,
                        FieldName.PUBLISHER, FieldName.ISBN, FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT,
                        FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITOR, FieldName.EDITORA, FieldName.EDITORB, FieldName.EDITORC,
                    FieldName.TRANSLATOR, FieldName.ANNOTATOR, FieldName.COMMENTATOR, FieldName.INTRODUCTION,
                    FieldName.FOREWORD, FieldName.AFTERWORD, FieldName.SUBTITLE, FieldName.TITLEADDON,
                    FieldName.LANGUAGE, FieldName.ORIGLANGUAGE, FieldName.EDITION, FieldName.VOLUMES, FieldName.SERIES,
                    FieldName.NUMBER, FieldName.NOTE, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN,
                    FieldName.PAGETOTAL, FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "MvBook";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType INBOOK = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(
                Arrays.asList(FieldName.BOOKAUTHOR, FieldName.EDITOR, FieldName.SUBTITLE, FieldName.TITLEADDON,
                        FieldName.MAINTITLE, FieldName.MAINSUBTITLE, FieldName.MAINTITLEADDON, FieldName.BOOKSUBTITLE,
                        FieldName.BOOKTITLEADDON, FieldName.VOLUME, FieldName.EDITION, FieldName.PUBLISHER,
                        FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.DOI, FieldName.EPRINT,
                        FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.BOOKTITLE,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.BOOKAUTHOR, FieldName.EDITOR, FieldName.EDITORA, FieldName.EDITORB,
                    FieldName.EDITORC, FieldName.TRANSLATOR, FieldName.ANNOTATOR, FieldName.COMMENTATOR,
                    FieldName.INTRODUCTION, FieldName.FOREWORD, FieldName.AFTERWORD, FieldName.SUBTITLE,
                    FieldName.TITLEADDON, FieldName.MAINTITLE, FieldName.MAINSUBTITLE, FieldName.MAINTITLEADDON,
                    FieldName.BOOKSUBTITLE, FieldName.BOOKTITLEADDON, FieldName.LANGUAGE, FieldName.ORIGLANGUAGE,
                    FieldName.VOLUME, FieldName.PART, FieldName.EDITION, FieldName.VOLUMES, FieldName.SERIES,
                    FieldName.NUMBER, FieldName.NOTE, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN,
                    FieldName.CHAPTER, FieldName.PAGES, FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI,
                    FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "InBook";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType BOOKINBOOK = new BibLatexEntryType() {

        @Override
        public String getName() {
            return "BookInBook";
        }

        // Same fields as "INBOOK" according to Biblatex 1.0:
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.INBOOK.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.INBOOK.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INBOOK.getPrimaryOptionalFields();
        }
    };

    public static final BibLatexEntryType SUPPBOOK = new BibLatexEntryType() {

        @Override
        public String getName() {
            return "SuppBook";
        }

        // Same fields as "INBOOK" according to Biblatex 1.0:
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.INBOOK.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.INBOOK.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INBOOK.getPrimaryOptionalFields();
        }
    };

    public static final BibLatexEntryType BOOKLET = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.HOWPUBLISHED,
                        FieldName.CHAPTER, FieldName.PAGES, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS,
                        FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.orFields(FieldName.AUTHOR, FieldName.EDITOR), FieldName.TITLE,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.LANGUAGE, FieldName.HOWPUBLISHED,
                    FieldName.TYPE, FieldName.NOTE, FieldName.LOCATION, FieldName.CHAPTER, FieldName.PAGES,
                    FieldName.PAGETOTAL, FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Booklet";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType COLLECTION = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(
                FieldName.TRANSLATOR, FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.MAINTITLE,
                FieldName.MAINSUBTITLE, FieldName.MAINTITLEADDON, FieldName.VOLUME, FieldName.EDITION,
                FieldName.PUBLISHER, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.DOI,
                FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.EDITOR, FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITORA, FieldName.EDITORB, FieldName.EDITORC, FieldName.TRANSLATOR,
                    FieldName.ANNOTATOR, FieldName.COMMENTATOR, FieldName.INTRODUCTION, FieldName.FOREWORD,
                    FieldName.AFTERWORD, FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.MAINTITLE,
                    FieldName.MAINSUBTITLE, FieldName.MAINTITLEADDON, FieldName.LANGUAGE, FieldName.ORIGLANGUAGE,
                    FieldName.VOLUME, FieldName.PART, FieldName.EDITION, FieldName.VOLUMES, FieldName.SERIES,
                    FieldName.NUMBER, FieldName.NOTE, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN,
                    FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, FieldName.ADDENDUM, FieldName.PUBSTATE,
                    FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Collection";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType MVCOLLECTION = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.TRANSLATOR, FieldName.SUBTITLE, FieldName.TITLEADDON,
                        FieldName.EDITION, FieldName.PUBLISHER, FieldName.ISBN, FieldName.DOI, FieldName.EPRINT,
                        FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.EDITOR, FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITORA, FieldName.EDITORB, FieldName.EDITORC, FieldName.TRANSLATOR,
                    FieldName.ANNOTATOR, FieldName.COMMENTATOR, FieldName.INTRODUCTION, FieldName.FOREWORD,
                    FieldName.AFTERWORD, FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.LANGUAGE,
                    FieldName.ORIGLANGUAGE, FieldName.EDITION, FieldName.VOLUMES, FieldName.SERIES, FieldName.NUMBER,
                    FieldName.NOTE, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN, FieldName.PAGETOTAL,
                    FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS,
                    FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "MvCollection";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType INCOLLECTION = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.TRANSLATOR, FieldName.SUBTITLE, FieldName.TITLEADDON,
                        FieldName.MAINTITLE, FieldName.MAINSUBTITLE, FieldName.MAINTITLEADDON, FieldName.BOOKSUBTITLE,
                        FieldName.BOOKTITLEADDON, FieldName.VOLUME, FieldName.EDITION, FieldName.PUBLISHER,
                        FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.DOI, FieldName.EPRINT,
                        FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.BOOKTITLE,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITOR, FieldName.EDITORA, FieldName.EDITORB, FieldName.EDITORC,
                    FieldName.TRANSLATOR, FieldName.ANNOTATOR, FieldName.COMMENTATOR, FieldName.INTRODUCTION,
                    FieldName.FOREWORD, FieldName.AFTERWORD, FieldName.SUBTITLE, FieldName.TITLEADDON,
                    FieldName.MAINTITLE, FieldName.MAINSUBTITLE, FieldName.MAINTITLEADDON, FieldName.BOOKSUBTITLE,
                    FieldName.BOOKTITLEADDON, FieldName.LANGUAGE, FieldName.ORIGLANGUAGE, FieldName.VOLUME,
                    FieldName.PART, FieldName.EDITION, FieldName.VOLUMES, FieldName.SERIES, FieldName.NUMBER,
                    FieldName.NOTE, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN, FieldName.CHAPTER,
                    FieldName.PAGES, FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "InCollection";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType SUPPCOLLECTION = new BibLatexEntryType() {

        @Override
        public String getName() {
            return "SuppCollection";
        }

        // Treated as alias of "INCOLLECTION" according to Biblatex 1.0:
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.INCOLLECTION.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.INCOLLECTION.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INCOLLECTION.getPrimaryOptionalFields();
        }
    };

    public static final BibLatexEntryType MANUAL = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(
                Arrays.asList(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.EDITION, FieldName.PUBLISHER,
                        FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.DOI, FieldName.EPRINT,
                        FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.orFields(FieldName.AUTHOR, FieldName.EDITOR), FieldName.TITLE,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.LANGUAGE, FieldName.EDITION,
                    FieldName.TYPE, FieldName.SERIES, FieldName.NUMBER, FieldName.VERSION, FieldName.NOTE,
                    FieldName.ORGANIZATION, FieldName.PUBLISHER, FieldName.LOCATION, FieldName.ISBN, FieldName.CHAPTER,
                    FieldName.PAGES, FieldName.PAGETOTAL, FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI,
                    FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Manual";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType MISC = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(
                FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.HOWPUBLISHED, FieldName.LOCATION, FieldName.DOI,
                FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.orFields(FieldName.AUTHOR, FieldName.EDITOR), FieldName.TITLE,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.LANGUAGE, FieldName.HOWPUBLISHED,
                    FieldName.TYPE, FieldName.VERSION, FieldName.NOTE, FieldName.ORGANIZATION, FieldName.LOCATION,
                    FieldName.MONTH, FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Misc";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType ONLINE = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(
                FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.NOTE, FieldName.ORGANIZATION, FieldName.URLDATE));

        {
            addAllRequired(FieldName.orFields(FieldName.AUTHOR, FieldName.EDITOR), FieldName.TITLE,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE), FieldName.URL);
            addAllOptional(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.LANGUAGE, FieldName.VERSION,
                    FieldName.NOTE, FieldName.ORGANIZATION, FieldName.MONTH, FieldName.ADDENDUM, FieldName.PUBSTATE,
                    FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Online";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType PATENT = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(FieldName.HOLDER,
                FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS,
                FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.NUMBER,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.HOLDER, FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.TYPE,
                    FieldName.VERSION, FieldName.LOCATION, FieldName.NOTE, FieldName.MONTH, FieldName.ADDENDUM,
                    FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE,
                    FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Patent";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType PERIODICAL = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(
                FieldName.SUBTITLE, FieldName.ISSUETITLE, FieldName.ISSUESUBTITLE, FieldName.ISSN, FieldName.DOI,
                FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.EDITOR, FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITORA, FieldName.EDITORB, FieldName.EDITORC, FieldName.SUBTITLE,
                    FieldName.ISSUETITLE, FieldName.ISSUESUBTITLE, FieldName.LANGUAGE, FieldName.SERIES,
                    FieldName.VOLUME, FieldName.NUMBER, FieldName.ISSUE, FieldName.MONTH, FieldName.NOTE,
                    FieldName.ISSN, FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Periodical";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType SUPPPERIODICAL = new BibLatexEntryType() {

        @Override
        public String getName() {
            return "SuppPeriodical";
        }

        // Treated as alias of "ARTICLE" according to Biblatex 1.0:
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.ARTICLE.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.ARTICLE.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.ARTICLE.getPrimaryOptionalFields();
        }
    };

    public static final BibLatexEntryType PROCEEDINGS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(
                FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.MAINTITLE, FieldName.MAINSUBTITLE,
                FieldName.MAINTITLEADDON, FieldName.EVENTTITLE, FieldName.VOLUME, FieldName.PUBLISHER, FieldName.ISBN,
                FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT,
                FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITOR, FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.MAINTITLE,
                    FieldName.MAINSUBTITLE, FieldName.MAINTITLEADDON, FieldName.EVENTTITLE, FieldName.EVENTTITLEADDON,
                    FieldName.EVENTDATE, FieldName.VENUE, FieldName.LANGUAGE, FieldName.VOLUME, FieldName.PART,
                    FieldName.VOLUMES, FieldName.SERIES, FieldName.NUMBER, FieldName.NOTE, FieldName.ORGANIZATION,
                    FieldName.PUBLISHER, FieldName.LOCATION, FieldName.MONTH, FieldName.YEAR, FieldName.ISBN,
                    FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, FieldName.ADDENDUM, FieldName.PUBSTATE,
                    FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Proceedings";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType MVPROCEEDINGS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(
                FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.MAINTITLE, FieldName.MAINSUBTITLE,
                FieldName.MAINTITLEADDON, FieldName.EVENTTITLE, FieldName.VOLUME, FieldName.PUBLISHER, FieldName.ISBN,
                FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT,
                FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITOR, FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.EVENTTITLE,
                    FieldName.EVENTTITLEADDON, FieldName.EVENTDATE, FieldName.VENUE, FieldName.LANGUAGE,
                    FieldName.VOLUMES, FieldName.SERIES, FieldName.NUMBER, FieldName.NOTE, FieldName.ORGANIZATION,
                    FieldName.PUBLISHER, FieldName.LOCATION, FieldName.MONTH, FieldName.ISBN, FieldName.PAGETOTAL,
                    FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS,
                    FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "MvProceedings";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType INPROCEEDINGS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.MAINTITLE,
                        FieldName.MAINSUBTITLE, FieldName.MAINTITLEADDON, FieldName.BOOKSUBTITLE,
                        FieldName.BOOKTITLEADDON, FieldName.EVENTTITLE, FieldName.VOLUME, FieldName.PUBLISHER,
                        FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.DOI, FieldName.EPRINT,
                        FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.BOOKTITLE,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.EDITOR, FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.MAINTITLE,
                    FieldName.MAINSUBTITLE, FieldName.MAINTITLEADDON, FieldName.BOOKSUBTITLE, FieldName.BOOKTITLEADDON,
                    FieldName.EVENTTITLE, FieldName.EVENTTITLEADDON, FieldName.EVENTDATE, FieldName.VENUE,
                    FieldName.LANGUAGE, FieldName.VOLUME, FieldName.PART, FieldName.VOLUMES, FieldName.SERIES,
                    FieldName.NUMBER, FieldName.NOTE, FieldName.ORGANIZATION, FieldName.PUBLISHER, FieldName.LOCATION,
                    FieldName.MONTH, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES, FieldName.ADDENDUM,
                    FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE,
                    FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "InProceedings";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType REFERENCE = new BibLatexEntryType() {

        @Override
        public String getName() {
            return "Reference";
        }

        // Treated as alias of "COLLECTION" according to Biblatex 1.0:
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.COLLECTION.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.COLLECTION.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.COLLECTION.getPrimaryOptionalFields();
        }
    };

    public static final BibLatexEntryType MVREFERENCE = new BibLatexEntryType() {

        @Override
        public String getName() {
            return "MvReference";
        }

        // Treated as alias of "MVCOLLECTION" according to Biblatex 1.0:
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.MVCOLLECTION.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.MVCOLLECTION.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.MVCOLLECTION.getPrimaryOptionalFields();
        }
    };

    public static final BibLatexEntryType INREFERENCE = new BibLatexEntryType() {

        @Override
        public String getName() {
            return "InReference";
        }

        // Treated as alias of "INCOLLECTION" according to Biblatex 1.0:
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.INCOLLECTION.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.INCOLLECTION.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INCOLLECTION.getPrimaryOptionalFields();
        }
    };

    public static final BibLatexEntryType REPORT = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(
                Arrays.asList(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.NUMBER, FieldName.ISRN,
                        FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT,
                        FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.TYPE, FieldName.INSTITUTION,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.LANGUAGE, FieldName.NUMBER,
                    FieldName.VERSION, FieldName.NOTE, FieldName.LOCATION, FieldName.MONTH, FieldName.ISRN,
                    FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, FieldName.ADDENDUM, FieldName.PUBSTATE,
                    FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL,
                    FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Report";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType SET = new BibLatexEntryType() {

        {
            addAllRequired(FieldName.ENTRYSET, FieldName.CROSSREF);
        }


        @Override
        public String getName() {
            return "Set";
        }
    };

    public static final BibLatexEntryType THESIS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.CHAPTER,
                        FieldName.PAGES, FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS,
                        FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.TYPE, FieldName.INSTITUTION,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.LANGUAGE, FieldName.NOTE,
                    FieldName.LOCATION, FieldName.MONTH, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES,
                    FieldName.PAGETOTAL, FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Thesis";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType UNPUBLISHED = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.HOWPUBLISHED,
                        FieldName.PUBSTATE, FieldName.URL, FieldName.URLDATE));

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.LANGUAGE, FieldName.HOWPUBLISHED,
                    FieldName.NOTE, FieldName.LOCATION, FieldName.MONTH, FieldName.ADDENDUM, FieldName.PUBSTATE,
                    FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "Unpublished";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    // === Type aliases: ===

    public static final BibLatexEntryType CONFERENCE = new BibLatexEntryType() {

        @Override
        public String getName() {
            return "Conference";
        }

        // Treated as alias of "INPROCEEDINGS" according to Biblatex 1.0:
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.INPROCEEDINGS.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.INPROCEEDINGS.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.INPROCEEDINGS.getPrimaryOptionalFields();
        }
    };

    public static final BibLatexEntryType ELECTRONIC = new BibLatexEntryType() {

        @Override
        public String getName() {
            return "Electronic";
        }

        // Treated as alias of "ONLINE" according to Biblatex 1.0:
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.ONLINE.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.ONLINE.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.ONLINE.getPrimaryOptionalFields();
        }
    };

    public static final BibLatexEntryType MASTERSTHESIS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.TYPE,
                        FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT,
                        FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            // Treated as alias of "THESIS", except FieldName.TYPE field is optional
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.INSTITUTION,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.TYPE, FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.LANGUAGE, FieldName.NOTE,
                    FieldName.LOCATION, FieldName.MONTH, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES,
                    FieldName.PAGETOTAL, FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "MastersThesis";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType PHDTHESIS = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections
                .unmodifiableList(Arrays.asList(FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.TYPE,
                        FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT,
                        FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            // Treated as alias of "THESIS", except FieldName.TYPE field is optional
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.INSTITUTION,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.TYPE, FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.LANGUAGE, FieldName.NOTE,
                    FieldName.LOCATION, FieldName.MONTH, FieldName.ISBN, FieldName.CHAPTER, FieldName.PAGES,
                    FieldName.PAGETOTAL, FieldName.ADDENDUM, FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT,
                    FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "PhdThesis";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType TECHREPORT = new BibLatexEntryType() {

        private final List<String> primaryOptionalFields = Collections.unmodifiableList(Arrays.asList(
                FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.TYPE, FieldName.NUMBER, FieldName.ISRN,
                FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, FieldName.DOI, FieldName.EPRINT,
                FieldName.EPRINTCLASS, FieldName.EPRINTTYPE, FieldName.URL, FieldName.URLDATE));

        {
            // Treated as alias of "REPORT", except FieldName.TYPE field is optional
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.INSTITUTION,
                    FieldName.orFields(FieldName.YEAR, FieldName.DATE));
            addAllOptional(FieldName.TYPE, FieldName.SUBTITLE, FieldName.TITLEADDON, FieldName.LANGUAGE,
                    FieldName.NUMBER, FieldName.VERSION, FieldName.NOTE, FieldName.LOCATION, FieldName.MONTH,
                    FieldName.ISRN, FieldName.CHAPTER, FieldName.PAGES, FieldName.PAGETOTAL, FieldName.ADDENDUM,
                    FieldName.PUBSTATE, FieldName.DOI, FieldName.EPRINT, FieldName.EPRINTCLASS, FieldName.EPRINTTYPE,
                    FieldName.URL, FieldName.URLDATE);
        }


        @Override
        public String getName() {
            return "TechReport";
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return primaryOptionalFields;
        }
    };

    public static final BibLatexEntryType WWW = new BibLatexEntryType() {

        @Override
        public String getName() {
            return "WWW";
        }

        // Treated as alias of "ONLINE" according to Biblatex 1.0:
        @Override
        public List<String> getRequiredFields() {
            return BibLatexEntryTypes.ONLINE.getRequiredFields();
        }

        @Override
        public List<String> getOptionalFields() {
            return BibLatexEntryTypes.ONLINE.getOptionalFields();
        }

        @Override
        public List<String> getPrimaryOptionalFields() {
            return BibLatexEntryTypes.ONLINE.getPrimaryOptionalFields();
        }
    };

    /**
     * This type is used for IEEEtran.bst to control various
     * be repeated or not. Not a very elegant solution, but it works...
     */
    public static final BibLatexEntryType IEEETRANBSTCTL = new BibLatexEntryType() {

        {
            addAllOptional(FieldName.CTLUSE_ARTICLE_NUMBER, FieldName.CTLUSE_PAPER, FieldName.CTLUSE_FORCED_ETAL,
                    FieldName.CTLUSE_URL, FieldName.CTLMAX_NAMES_FORCED_ETAL, FieldName.CTLNAMES_SHOW_ETAL,
                    FieldName.CTLUSE_ALT_SPACING, FieldName.CTLALT_STRETCH_FACTOR, FieldName.CTLDASH_REPEATED_NAMES,
                    FieldName.CTLNAME_FORMAT_STRING, FieldName.CTLNAME_LATEX_CMD, FieldName.CTLNAME_URL_PREFIX);
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


    public static Optional<EntryType> getType(String name) {
        return ALL.stream().filter(e -> e.getName().equalsIgnoreCase(name)).findFirst();
    }
}
