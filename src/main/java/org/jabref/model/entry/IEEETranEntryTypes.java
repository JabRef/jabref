package org.jabref.model.entry;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.IEEEField;
import org.jabref.model.entry.field.StandardField;

/**
 * This class represents all supported IEEETran entry types.
 *
 * @see http://ctan.sharelatex.com/tex-archive/macros/latex/contrib/IEEEtran/bibtex/IEEEtran_bst_HOWTO.pdf
 * <p>
 * Electronic, IEEETranBSTCTL, Periodical, Patent, Standard
 */
public class IEEETranEntryTypes {
    /**
     * Electronic entry type for internet references
     * <p>
     * Required fields:
     * Optional fields: author, month, year, title, language, howpublished, organization, address, note, url
     */
    public static final EntryType ELECTRONIC = new BibtexEntryType() {

        {
            addAllOptional(StandardField.AUTHOR, StandardField.MONTH, StandardField.YEAR, StandardField.TITLE, StandardField.LANGUAGE,
                    StandardField.HOWPUBLISHED, StandardField.ORGANIZATION, StandardField.ADDRESS, StandardField.NOTE, StandardField.URL);
        }

        @Override
        public String getName() {
            return "Electronic";
        }
    };

    /**
     * Special entry type that can be used to externally control some aspects of the bibliography style.
     */
    public static final EntryType IEEETRANBSTCTL = new BibtexEntryType() {

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

    /**
     * The periodical entry type is used for journals and magazines.
     * <p>
     * Required fields: title, year
     * Optional fields: editor, language, series, volume, number, organization, month, note, url
     */
    public static final EntryType PERIODICAL = new BibtexEntryType() {

        {
            addAllRequired(StandardField.TITLE, StandardField.YEAR);
            addAllOptional(StandardField.EDITOR, StandardField.LANGUAGE, StandardField.SERIES, StandardField.VOLUME, StandardField.NUMBER,
                    StandardField.ORGANIZATION, StandardField.MONTH, StandardField.NOTE, StandardField.URL);
        }

        @Override
        public String getName() {
            return "Periodical";
        }
    };

    /**
     * Entry type for patents.
     * <p>
     * Required fields: nationality, number, year or yearfiled
     * Optional fields: author, title, language, assignee, address, type, number, day, dayfiled, month, monthfiled, note, url
     */
    public static final EntryType PATENT = new BibtexEntryType() {

        {
            addAllRequired(StandardField.NATIONALITY, StandardField.NUMBER,
                    FieldFactory.orFields(StandardField.YEAR, StandardField.YEARFILED));
            addAllOptional(StandardField.AUTHOR, StandardField.TITLE, StandardField.LANGUAGE, StandardField.ASSIGNEE, StandardField.ADDRESS,
                    StandardField.TYPE, StandardField.NUMBER, StandardField.DAY, StandardField.DAYFILED, StandardField.MONTH,
                    StandardField.MONTHFILED, StandardField.NOTE, StandardField.URL);
        }

        @Override
        public String getName() {
            return "Patent";
        }
    };

    /**
     * The standard entry type is used for proposed or formally published standards.
     * <p>
     * Required fields: title, organization or institution
     * Optional fields: author, language, howpublished, type, number, revision, address, month, year, note, url
     */
    public static final EntryType STANDARD = new BibtexEntryType() {

        {
            addAllRequired(StandardField.TITLE, FieldFactory.orFields(StandardField.ORGANIZATION, StandardField.INSTITUTION));
            addAllOptional(StandardField.AUTHOR, StandardField.LANGUAGE, StandardField.HOWPUBLISHED, StandardField.TYPE,
                    StandardField.NUMBER, StandardField.REVISION, StandardField.ADDRESS, StandardField.MONTH, StandardField.YEAR,
                    StandardField.NOTE, StandardField.URL);
        }

        @Override
        public String getName() {
            return "Standard";
        }
    };

    public static final List<EntryType> ALL = Arrays.asList(ELECTRONIC, IEEETRANBSTCTL, PERIODICAL, PATENT, STANDARD);

    private IEEETranEntryTypes() {
    }

    public static Optional<EntryType> getType(String name) {
        return ALL.stream().filter(e -> e.getName().equalsIgnoreCase(name)).findFirst();
    }
}
