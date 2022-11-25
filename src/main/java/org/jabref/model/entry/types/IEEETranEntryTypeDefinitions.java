package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.IEEEField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;

/**
 * This class represents all supported IEEETran entry types.
 * <p>
 * See <a href="http://ctan.sharelatex.com/tex-archive/macros/latex/contrib/IEEEtran/bibtex/IEEEtran_bst_HOWTO.pdf">http://ctan.sharelatex.com/tex-archive/macros/latex/contrib/IEEEtran/bibtex/IEEEtran_bst_HOWTO.pdf</a>
 * <p>
 * Electronic, IEEETranBSTCTL, Periodical, Patent, Standard
 */
public class IEEETranEntryTypeDefinitions {
    /**
     * Electronic entry type for internet references
     * <p>
     * Required fields:
     * Optional fields: author, month, year, title, language, howpublished, organization, address, note, url
     */
    private static final BibEntryType ELECTRONIC = new BibEntryTypeBuilder()
            .withType(IEEETranEntryType.Electronic)
            .withImportantFields(StandardField.AUTHOR, StandardField.MONTH, StandardField.YEAR, StandardField.TITLE, StandardField.LANGUAGE,
                    StandardField.HOWPUBLISHED, StandardField.ORGANIZATION, StandardField.ADDRESS, StandardField.NOTE, StandardField.URL)
            .build();

    /**
     * Special entry type that can be used to externally control some aspects of the bibliography style.
     */
    private static final BibEntryType IEEETRANBSTCTL = new BibEntryTypeBuilder()
            .withType(IEEETranEntryType.IEEEtranBSTCTL)
            .withImportantFields(IEEEField.CTLUSE_ARTICLE_NUMBER, IEEEField.CTLUSE_PAPER, IEEEField.CTLUSE_FORCED_ETAL,
                    IEEEField.CTLUSE_URL, IEEEField.CTLMAX_NAMES_FORCED_ETAL, IEEEField.CTLNAMES_SHOW_ETAL,
                    IEEEField.CTLUSE_ALT_SPACING, IEEEField.CTLALT_STRETCH_FACTOR, IEEEField.CTLDASH_REPEATED_NAMES,
                    IEEEField.CTLNAME_FORMAT_STRING, IEEEField.CTLNAME_LATEX_CMD, IEEEField.CTLNAME_URL_PREFIX)
            .build();

    /**
     * The periodical entry type is used for journals and magazines.
     * <p>
     * Required fields: title, year
     * Optional fields: editor, language, series, volume, number, organization, month, note, url
     */
    private static final BibEntryType PERIODICAL = new BibEntryTypeBuilder()
            .withType(IEEETranEntryType.Periodical)
            .withRequiredFields(StandardField.TITLE, StandardField.YEAR)
            .withImportantFields(StandardField.EDITOR, StandardField.LANGUAGE, StandardField.SERIES, StandardField.VOLUME, StandardField.NUMBER, StandardField.ORGANIZATION, StandardField.MONTH, StandardField.NOTE, StandardField.URL)
            .build();

    /**
     * Entry type for patents.
     * <p>
     * Required fields: nationality, number, year or yearfiled
     * Optional fields: author, title, language, assignee, address, type, number, day, dayfiled, month, monthfiled, note, url
     */
    private static final BibEntryType PATENT = new BibEntryTypeBuilder()
            .withType(IEEETranEntryType.Patent)
            .withRequiredFields(new OrFields(StandardField.YEAR, StandardField.YEARFILED), StandardField.NATIONALITY, StandardField.NUMBER)
            .withImportantFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.LANGUAGE, StandardField.ASSIGNEE, StandardField.ADDRESS,
                    StandardField.TYPE, StandardField.NUMBER, StandardField.DAY, StandardField.DAYFILED, StandardField.MONTH,
                    StandardField.MONTHFILED, StandardField.NOTE, StandardField.URL)
            .build();

    /**
     * The standard entry type is used for proposed or formally published standards.
     * <p>
     * Required fields: title, organization or institution
     * Optional fields: author, language, howpublished, type, number, revision, address, month, year, note, url
     */
    private static final BibEntryType STANDARD = new BibEntryTypeBuilder()
            .withType(IEEETranEntryType.Standard)
            .withRequiredFields(new OrFields(StandardField.ORGANIZATION, StandardField.INSTITUTION), StandardField.TITLE)
            .withImportantFields(StandardField.AUTHOR, StandardField.LANGUAGE, StandardField.HOWPUBLISHED, StandardField.TYPE,
                    StandardField.NUMBER, StandardField.REVISION, StandardField.ADDRESS, StandardField.MONTH, StandardField.YEAR,
                    StandardField.NOTE, StandardField.URL)
            .build();

    public static final List<BibEntryType> ALL = Arrays.asList(ELECTRONIC, IEEETRANBSTCTL, PERIODICAL, PATENT, STANDARD);

    private IEEETranEntryTypeDefinitions() {
    }
}
