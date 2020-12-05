package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

/**
 * This class represents all supported entry types used in a study definition file
 */
public class SystematicLiteratureReviewStudyEntryTypeDefinitions {

    /**
     * Entry type used for study meta data within a study definition file
     *
     * <ul>
     * <li>Required fields: author, lastsearchdate, name, enabled</li>
     * <li>Optional fields:</li>
     * </ul>
     */
    private static final BibEntryType STUDY_ENTRY = new BibEntryTypeBuilder()
            .withType(SystematicLiteratureReviewStudyEntryType.STUDY_ENTRY)
            .withRequiredFields(StandardField.AUTHOR, new UnknownField("lastsearchdate"), new UnknownField("name"), new UnknownField("researchquestions"))
            .build();

    /**
     * Entry type for the queries within the study definition file
     *
     * <ul>
     * <li>Required fields: query</li>
     * <li>Optional fields:</li>
     * </ul>
     */
    private static final BibEntryType SEARCH_QUERY_ENTRY = new BibEntryTypeBuilder()
            .withType(SystematicLiteratureReviewStudyEntryType.SEARCH_QUERY_ENTRY)
            .withRequiredFields(new UnknownField("query"))
            .build();

    /**
     * Entry type for the targeted libraries within a study definition file
     *
     * <ul>
     * <li>Required fields: name, enabled</li>
     * <li>Optional fields: comment</li>
     * </ul>
     */
    private static final BibEntryType LIBRARY_ENTRY = new BibEntryTypeBuilder()
            .withType(SystematicLiteratureReviewStudyEntryType.STUDY_ENTRY)
            .withRequiredFields(new UnknownField("name"), new UnknownField("enabled"))
            .withImportantFields(StandardField.COMMENT)
            .build();

    public static final List<BibEntryType> ALL = Arrays.asList(STUDY_ENTRY, SEARCH_QUERY_ENTRY, LIBRARY_ENTRY);

    private SystematicLiteratureReviewStudyEntryTypeDefinitions() {
    }
}
