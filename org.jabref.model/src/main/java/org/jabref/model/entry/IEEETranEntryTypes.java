package org.jabref.model.entry;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
            addAllOptional(FieldName.AUTHOR, FieldName.MONTH, FieldName.YEAR, FieldName.TITLE, FieldName.LANGUAGE,
                    FieldName.HOWPUBLISHED, FieldName.ORGANIZATION, FieldName.ADDRESS, FieldName.NOTE, FieldName.URL);
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

    /**
     * The periodical entry type is used for journals and magazines.
     * <p>
     * Required fields: title, year
     * Optional fields: editor, language, series, volume, number, organization, month, note, url
     */
    public static final EntryType PERIODICAL = new BibtexEntryType() {

        {
            addAllRequired(FieldName.TITLE, FieldName.YEAR);
            addAllOptional(FieldName.EDITOR, FieldName.LANGUAGE, FieldName.SERIES, FieldName.VOLUME, FieldName.NUMBER,
                    FieldName.ORGANIZATION, FieldName.MONTH, FieldName.NOTE, FieldName.URL);
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
            addAllRequired(FieldName.NATIONALITY, FieldName.NUMBER,
                    FieldName.orFields(FieldName.YEAR, FieldName.YEARFILED));
            addAllOptional(FieldName.AUTHOR, FieldName.TITLE, FieldName.LANGUAGE, FieldName.ASSIGNEE, FieldName.ADDRESS,
                    FieldName.TYPE, FieldName.NUMBER, FieldName.DAY, FieldName.DAYFILED, FieldName.MONTH,
                    FieldName.MONTHFILED, FieldName.NOTE, FieldName.URL);
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
            addAllRequired(FieldName.TITLE, FieldName.orFields(FieldName.ORGANIZATION, FieldName.INSTITUTION));
            addAllOptional(FieldName.AUTHOR, FieldName.LANGUAGE, FieldName.HOWPUBLISHED, FieldName.TYPE,
                    FieldName.NUMBER, FieldName.REVISION, FieldName.ADDRESS, FieldName.MONTH, FieldName.YEAR,
                    FieldName.NOTE, FieldName.URL);
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
