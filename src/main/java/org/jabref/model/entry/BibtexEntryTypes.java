package org.jabref.model.entry;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

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
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.JOURNAL, StandardField.YEAR);
            addAllOptional(StandardField.VOLUME, StandardField.NUMBER, StandardField.PAGES, StandardField.MONTH, StandardField.ISSN, StandardField.NOTE);
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
            addAllRequired(StandardField.TITLE, StandardField.PUBLISHER, StandardField.YEAR, FieldFactory.orFields(StandardField.AUTHOR, StandardField.EDITOR));
            addAllOptional(StandardField.VOLUME, StandardField.NUMBER, StandardField.SERIES, StandardField.ADDRESS, StandardField.EDITION, StandardField.MONTH, StandardField.ISBN, StandardField.NOTE);
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
            addAllRequired(StandardField.TITLE);
            addAllOptional(StandardField.AUTHOR, StandardField.HOWPUBLISHED, StandardField.ADDRESS, StandardField.MONTH, StandardField.YEAR, StandardField.NOTE);
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
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.BOOKTITLE, StandardField.YEAR);
            addAllOptional(StandardField.EDITOR, StandardField.VOLUME, StandardField.NUMBER, StandardField.SERIES, StandardField.PAGES, StandardField.ADDRESS, StandardField.MONTH, StandardField.ORGANIZATION,
                    StandardField.PUBLISHER, StandardField.NOTE);
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
            addAllRequired(FieldFactory.orFields(StandardField.CHAPTER, StandardField.PAGES), StandardField.TITLE, StandardField.PUBLISHER, StandardField.YEAR, FieldFactory.orFields(StandardField.AUTHOR, StandardField.EDITOR));
            addAllOptional(StandardField.VOLUME, StandardField.NUMBER, StandardField.SERIES, StandardField.TYPE, StandardField.ADDRESS, StandardField.EDITION, StandardField.MONTH, StandardField.ISBN, StandardField.NOTE);
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
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.BOOKTITLE, StandardField.PUBLISHER, StandardField.YEAR);
            addAllOptional(StandardField.EDITOR, StandardField.VOLUME, StandardField.NUMBER, StandardField.SERIES, StandardField.TYPE, StandardField.CHAPTER, StandardField.PAGES, StandardField.ADDRESS, StandardField.EDITION,
                    StandardField.MONTH, StandardField.ISBN, StandardField.NOTE);
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
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.BOOKTITLE, StandardField.YEAR);
            addAllOptional(StandardField.EDITOR, StandardField.VOLUME, StandardField.NUMBER, StandardField.SERIES, StandardField.PAGES, StandardField.ADDRESS, StandardField.MONTH, StandardField.ORGANIZATION,
                    StandardField.PUBLISHER, StandardField.NOTE);
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
            addAllRequired(StandardField.TITLE);
            addAllOptional(StandardField.AUTHOR, StandardField.ORGANIZATION, StandardField.ADDRESS, StandardField.EDITION, StandardField.MONTH, StandardField.YEAR, StandardField.ISBN, StandardField.NOTE);
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
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.SCHOOL, StandardField.YEAR);
            addAllOptional(StandardField.TYPE, StandardField.ADDRESS, StandardField.MONTH, StandardField.NOTE);
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
            addAllOptional(StandardField.AUTHOR, StandardField.TITLE, StandardField.HOWPUBLISHED, StandardField.MONTH, StandardField.YEAR, StandardField.NOTE);
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
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.SCHOOL, StandardField.YEAR);
            addAllOptional(StandardField.TYPE, StandardField.ADDRESS, StandardField.MONTH, StandardField.NOTE);
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
            addAllRequired(StandardField.TITLE, StandardField.YEAR);
            addAllOptional(StandardField.EDITOR, StandardField.VOLUME, StandardField.NUMBER, StandardField.SERIES, StandardField.ADDRESS, StandardField.PUBLISHER, StandardField.MONTH,
                    StandardField.ORGANIZATION, StandardField.ISBN, StandardField.NOTE);
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
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.INSTITUTION, StandardField.YEAR);
            addAllOptional(StandardField.TYPE, StandardField.NUMBER, StandardField.ADDRESS, StandardField.MONTH, StandardField.NOTE);
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
            addAllRequired(StandardField.AUTHOR, StandardField.TITLE, StandardField.NOTE);
            addAllOptional(StandardField.MONTH, StandardField.YEAR);
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

    public static EntryType getTypeOrDefault(String name) {
        return getType(name).orElseGet(() -> new CustomEntryType(name, "required", "optional"));
    }
}
