package org.jabref.model.entry.types;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.StandardField;

import java.util.Arrays;
import java.util.List;

public class BiblatexAPAEntryTypeDefinitions {

    private static final BibEntryType JURISDICTION = new BibEntryTypeBuilder()
            .withType(BiblatexApaEntryType.Jurisdiction)
            .withDetailFields(StandardField.ORGANIZATION, StandardField.CITATION_CITEORG, StandardField.CITATION_CITEDATE, StandardField.CITATION_CITEDATE, StandardField.ORIGDATE)
            .withRequiredFields(StandardField.TITLE, StandardField.CITATION, StandardField.CITATION_CITEINFO, StandardField.URL, StandardField.DATE)
            .build();

    private static final BibEntryType LEGISLATION = new BibEntryTypeBuilder()
            .withType(BiblatexApaEntryType.Legislation)
            .withImportantFields(StandardField.TITLEADDON, StandardField.ORIGDATE)
            .withRequiredFields(StandardField.TITLE, StandardField.LOCATION, StandardField.URL, StandardField.DATE)
            .build();

    private static final BibEntryType LEGADMINMATERIAL = new BibEntryTypeBuilder()
            .withType(BiblatexApaEntryType.Legadminmaterial)
            .withImportantFields(StandardField.NUMBER, StandardField.SHORTTITLE, StandardField.NOTE, StandardField.KEYWORDS)
            .withRequiredFields(StandardField.TITLE, StandardField.CITATION, StandardField.URL, StandardField.DATE)
            .build();

    private static final BibEntryType CONSTITUTION = new BibEntryTypeBuilder()
            .withType(BiblatexApaEntryType.Constitution)
            .withImportantFields(StandardField.ARTICLE, StandardField.AMENDMENT, StandardField.EVENTDATE, StandardField.KEYWORDS, StandardField.PART, StandardField.SECTION)
            .withRequiredFields(StandardField.SOURCE, StandardField.TYPE)
            .build();

    private static final BibEntryType LEGAL = new BibEntryTypeBuilder()
        .withType(BiblatexApaEntryType.Legal)
        .withRequiredFields(StandardField.TITLE, StandardField.DATE, StandardField.URI, StandardField.KEYWORDS, StandardField.PART, StandardField.SECTION)
        .build();

    public static final List<BibEntryType> ALL = Arrays.asList(JURISDICTION, LEGISLATION, LEGADMINMATERIAL, CONSTITUTION, LEGAL);
}
