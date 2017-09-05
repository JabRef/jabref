package org.jabref.model.entry;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This class represents all supported BibTex entry types.
 * <p>
 * Article, Book, Booklet, Conference, Inbook, Incollection, Inproceedings,
 * Manual, Mastersthesis, Misc, Phdthesis, Proceedings, Techreport, Unpublished
 */
public class BibtexEntryTypes {
    /**
     * An article from a journal or magazine.
     * <p>
     * Required fields: author, title, journal, year.
     * Optional fields: volume, number, pages, month, note.
     */
    public static final EntryType ARTICLE = new BibtexEntryType() {

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.JOURNAL, FieldName.YEAR);
            addAllOptional(FieldName.VOLUME, FieldName.NUMBER, FieldName.PAGES, FieldName.MONTH, FieldName.ISSN, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "Article";
        }
    };

    /**
     * A book with an explicit publisher.
     * <p>
     * Required fields: author or editor, title, publisher, year.
     * Optional fields: volume or number, series, address, edition, month, note.
     */
    public static final EntryType BOOK = new BibtexEntryType() {

        {
            addAllRequired(FieldName.TITLE, FieldName.PUBLISHER, FieldName.YEAR, FieldName.orFields(FieldName.AUTHOR, FieldName.EDITOR));
            addAllOptional(FieldName.VOLUME, FieldName.NUMBER, FieldName.SERIES, FieldName.ADDRESS, FieldName.EDITION, FieldName.MONTH, FieldName.ISBN, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "Book";
        }
    };

    /**
     * A work that is printed and bound, but without a named publisher or sponsoring institution.
     * <p>
     * Required field: title.
     * Optional fields: author, howpublished, address, month, year, note.
     */
    public static final EntryType BOOKLET = new BibtexEntryType() {

        {
            addAllRequired(FieldName.TITLE);
            addAllOptional(FieldName.AUTHOR, FieldName.HOWPUBLISHED, FieldName.ADDRESS, FieldName.MONTH, FieldName.YEAR, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "Booklet";
        }
    };

    /**
     * An article in a conference proceedings.
     * <p>
     * Required fields: author, title, booktitle, year.
     * Optional fields: editor, volume or number, series, pages, address, month, organization, publisher, note.
     */
    public static final EntryType CONFERENCE = new BibtexEntryType() {

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.BOOKTITLE, FieldName.YEAR);
            addAllOptional(FieldName.EDITOR, FieldName.VOLUME, FieldName.NUMBER, FieldName.SERIES, FieldName.PAGES, FieldName.ADDRESS, FieldName.MONTH, FieldName.ORGANIZATION,
                    FieldName.PUBLISHER, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "Conference";
        }
    };

    /**
     * A part of a book, which may be a chapter (or section or whatever) and/or a range of pages.
     * <p>
     * Required fields: author or editor, title, chapter and/or pages, publisher, year.
     * Optional fields: volume or number, series, type, address, edition, month, note.
     */
    public static final EntryType INBOOK = new BibtexEntryType() {

        {
            addAllRequired(FieldName.orFields(FieldName.CHAPTER, FieldName.PAGES), FieldName.TITLE, FieldName.PUBLISHER, FieldName.YEAR, FieldName.orFields(FieldName.AUTHOR, FieldName.EDITOR));
            addAllOptional(FieldName.VOLUME, FieldName.NUMBER, FieldName.SERIES, FieldName.TYPE, FieldName.ADDRESS, FieldName.EDITION, FieldName.MONTH, FieldName.ISBN, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "InBook";
        }
    };

    /**
     * A part of a book having its own title.
     * Required fields: author, title, booktitle, publisher, year.
     * Optional fields: editor, volume or number, series, type, chapter, pages, address, edition, month, note.
     */
    public static final EntryType INCOLLECTION = new BibtexEntryType() {

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.BOOKTITLE, FieldName.PUBLISHER, FieldName.YEAR);
            addAllOptional(FieldName.EDITOR, FieldName.VOLUME, FieldName.NUMBER, FieldName.SERIES, FieldName.TYPE, FieldName.CHAPTER, FieldName.PAGES, FieldName.ADDRESS, FieldName.EDITION,
                    FieldName.MONTH, FieldName.ISBN, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "InCollection";
        }
    };

    /**
     * An article in a conference proceedings.
     * <p>
     * Required fields: author, title, booktitle, year.
     * Optional fields: editor, volume or number, series, pages, address, month, organization, publisher, note.
     */
    public static final EntryType INPROCEEDINGS = new BibtexEntryType() {

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.BOOKTITLE, FieldName.YEAR);
            addAllOptional(FieldName.EDITOR, FieldName.VOLUME, FieldName.NUMBER, FieldName.SERIES, FieldName.PAGES, FieldName.ADDRESS, FieldName.MONTH, FieldName.ORGANIZATION,
                    FieldName.PUBLISHER, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "InProceedings";
        }
    };

    /**
     * Technical documentation.
     * Required field: title.
     * Optional fields: author, organization, address, edition, month, year, note.
     */
    public static final EntryType MANUAL = new BibtexEntryType() {

        {
            addAllRequired(FieldName.TITLE);
            addAllOptional(FieldName.AUTHOR, FieldName.ORGANIZATION, FieldName.ADDRESS, FieldName.EDITION, FieldName.MONTH, FieldName.YEAR, FieldName.ISBN, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "Manual";
        }
    };

    /**
     * A Master's thesis.
     * <p>
     * Required fields: author, title, school, year.
     * Optional fields: type, address, month, note.
     */
    public static final EntryType MASTERSTHESIS = new BibtexEntryType() {

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.SCHOOL, FieldName.YEAR);
            addAllOptional(FieldName.TYPE, FieldName.ADDRESS, FieldName.MONTH, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "MastersThesis";
        }
    };

    /**
     * Use this type when nothing else fits.
     * <p>
     * Required fields: none.
     * Optional fields: author, title, howpublished, month, year, note.
     */
    public static final EntryType MISC = new BibtexEntryType() {

        {
            addAllOptional(FieldName.AUTHOR, FieldName.TITLE, FieldName.HOWPUBLISHED, FieldName.MONTH, FieldName.YEAR, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "Misc";
        }
    };

    /**
     * A PhD thesis.
     * <p>
     * Required fields: author, title, school, year.
     * Optional fields: type, address, month, note.
     */
    public static final EntryType PHDTHESIS = new BibtexEntryType() {

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.SCHOOL, FieldName.YEAR);
            addAllOptional(FieldName.TYPE, FieldName.ADDRESS, FieldName.MONTH, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "PhdThesis";
        }
    };

    /**
     * The proceedings of a conference.
     * <p>
     * Required fields: title, year.
     * Optional fields: editor, volume or number, series, address, month, organization, publisher, note.
     */
    public static final EntryType PROCEEDINGS = new BibtexEntryType() {

        {
            addAllRequired(FieldName.TITLE, FieldName.YEAR);
            addAllOptional(FieldName.EDITOR, FieldName.VOLUME, FieldName.NUMBER, FieldName.SERIES, FieldName.ADDRESS, FieldName.PUBLISHER, FieldName.MONTH,
                    FieldName.ORGANIZATION, FieldName.ISBN, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "Proceedings";
        }
    };

    /**
     * A report published by a school or other institution, usually numbered within a series.
     * <p>
     * Required fields: author, title, institution, year.
     * Optional fields: type, number, address, month, note.
     */
    public static final EntryType TECHREPORT = new BibtexEntryType() {

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.INSTITUTION, FieldName.YEAR);
            addAllOptional(FieldName.TYPE, FieldName.NUMBER, FieldName.ADDRESS, FieldName.MONTH, FieldName.NOTE);
        }

        @Override
        public String getName() {
            return "TechReport";
        }
    };

    /**
     * A document having an author and title, but not formally published.
     * <p>
     * Required fields: author, title, note.
     * Optional fields: month, year.
     */
    public static final EntryType UNPUBLISHED = new BibtexEntryType() {

        {
            addAllRequired(FieldName.AUTHOR, FieldName.TITLE, FieldName.NOTE);
            addAllOptional(FieldName.MONTH, FieldName.YEAR);
        }

        @Override
        public String getName() {
            return "Unpublished";
        }
    };

    public static final List<EntryType> ALL = Arrays.asList(ARTICLE, INBOOK, BOOK, BOOKLET, INCOLLECTION, CONFERENCE,
            INPROCEEDINGS, PROCEEDINGS, MANUAL, MASTERSTHESIS, PHDTHESIS, TECHREPORT, UNPUBLISHED, MISC);

    private BibtexEntryTypes() {
    }

    public static Optional<EntryType> getType(String name) {
        return ALL.stream().filter(e -> e.getName().equalsIgnoreCase(name)).findFirst();
    }
}
