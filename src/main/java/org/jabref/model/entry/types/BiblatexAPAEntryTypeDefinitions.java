package org.jabref.model.entry.types;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.StandardField;

import java.util.Arrays;
import java.util.List;

public class BiblatexAPAEntryTypeDefinitions {

    private static final BibEntryType JURISDICTION = new BibEntryTypeBuilder()
            .withType(StandardEntryType.JURISDICTION)
            .withDetailFields(StandardField.ORGANIZATION, StandardField.CITATION_CITEORG, StandardField.CITATION_CITEDATE, StandardField.CITATION_CITEDATE, StandardField.ORIGDATE)
            .withRequiredFields(StandardField.TITLE, StandardField.CITATION, StandardField.CITATION_CITEINFO, StandardField.URL, StandardField.DATE)
            .build();

    private static final BibEntryType LEGISLATION = new BibEntryTypeBuilder()
            .withType(StandardEntryType.LEGISLATION)
            .withImportantFields(StandardField.TITLEADDON, StandardField.ORIGDATE)
            .withRequiredFields(StandardField.TITLE, StandardField.LOCATION, StandardField.URL, StandardField.DATE)
            .build();

    private static final BibEntryType LEGADMINMATERIAL = new BibEntryTypeBuilder()
            .withType(StandardEntryType.LEGADMINMATERIAL)
            .withImportantFields(StandardField.NUMBER, StandardField.SHORTTITLE, StandardField.NOTE, StandardField.KEYWORDS)
            .withRequiredFields(StandardField.TITLE, StandardField.CITATION, StandardField.URL, StandardField.DATE)
            .build();

    private static final BibEntryType CONSTITUTION = new BibEntryTypeBuilder()
            .withType(StandardEntryType.CONSTITUTION)
            .withImportantFields(StandardField.ARTICLE, StandardField.AMENDMENT, StandardField.EVENTDATE, StandardField.KEYWORDS, StandardField.PART, StandardField.SECTION)
            .withRequiredFields(StandardField.SOURCE, StandardField.TYPE)
            .build();

    public static final List<BibEntryType> ALL = Arrays.asList(JURISDICTION, LEGISLATION, LEGADMINMATERIAL, CONSTITUTION);
}
