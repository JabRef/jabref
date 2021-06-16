package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;

public class BiblatexSoftwareEntryTypeDefinitions {
    private static final BibEntryType SOFTWARE = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Software)
            .withImportantFields(StandardField.DATE, StandardField.DOI, StandardField.EPRINTTYPE, StandardField.EPRINTCLASS, StandardField.EPRINT,
                    StandardField.EDITOR, StandardField.FILE, StandardField.HALID, StandardField.HALVERSION, StandardField.INSTITUTION, StandardField.INTRODUCEDIN,
                    StandardField.LICENSE, StandardField.MONTH, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.PUBLISHER, StandardField.RELATED,
                    StandardField.RELATEDSTRING, StandardField.REPOSITORY, StandardField.SWHID, StandardField.URLDATE, StandardField.VERSION)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.URL, StandardField.VERSION, StandardField.YEAR)
            .build();

    private static final BibEntryType SOFTWAREVERSION = new BibEntryTypeBuilder()
            .withType(StandardEntryType.SoftwareVersion)
            .withImportantFields(StandardField.DATE, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.HALID, StandardField.HALVERSION,
                    StandardField.INSTITUTION, StandardField.INTRODUCEDIN, StandardField.LICENSE, StandardField.MONTH, StandardField.NOTE, StandardField.ORGANIZATION,
                    StandardField.PUBLISHER, StandardField.RELATED, StandardField.RELATEDTYPE, StandardField.RELATEDSTRING,
                    StandardField.REPOSITORY, StandardField.SWHID, StandardField.SUBTITLE, StandardField.URLDATE)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.URL, StandardField.YEAR, StandardField.VERSION)
            .withDetailFields(StandardField.DATE, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.HALID, StandardField.HALVERSION,
                    StandardField.INSTITUTION, StandardField.INTRODUCEDIN, StandardField.LICENSE, StandardField.MONTH, StandardField.NOTE, StandardField.ORGANIZATION,
                    StandardField.PUBLISHER, StandardField.RELATED, StandardField.RELATEDTYPE, StandardField.RELATEDSTRING,
                    StandardField.REPOSITORY, StandardField.SWHID, StandardField.SUBTITLE, StandardField.URLDATE)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.URL, StandardField.YEAR)
            .build();
    private static final BibEntryType SOFTWAREMODULE = new BibEntryTypeBuilder()
            .withType(StandardEntryType.SoftwareModule)
            .withImportantFields(StandardField.DATE, StandardField.DOI, StandardField.EPRINTTYPE, StandardField.EPRINTCLASS, StandardField.EPRINT,
                    StandardField.EDITOR, StandardField.FILE, StandardField.HALID, StandardField.HALVERSION, StandardField.INSTITUTION, StandardField.INTRODUCEDIN,
                    StandardField.LICENSE, StandardField.MONTH, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.PUBLISHER, StandardField.RELATED,
                    StandardField.RELATEDSTRING, StandardField.REPOSITORY, StandardField.SWHID, StandardField.URLDATE, StandardField.VERSION)
            .withRequiredFields(StandardField.AUTHOR, StandardField.SUBTITLE, StandardField.URL, StandardField.YEAR)
            .build();

    private static final BibEntryType CODEFRAGMENT = new BibEntryTypeBuilder()
            .withType(StandardEntryType.CodeFragment)
            .withImportantFields(StandardField.DATE, StandardField.DOI, StandardField.EPRINTTYPE, StandardField.EPRINTCLASS, StandardField.EPRINT,
                    StandardField.EDITOR, StandardField.FILE, StandardField.HALID, StandardField.HALVERSION, StandardField.INSTITUTION, StandardField.INTRODUCEDIN,
                    StandardField.LICENSE, StandardField.MONTH, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.PUBLISHER, StandardField.RELATED,
                    StandardField.RELATEDSTRING, StandardField.REPOSITORY, StandardField.SWHID, StandardField.URLDATE, StandardField.VERSION)
            .withRequiredFields(StandardField.URL)
            .build();

    public static final List<BibEntryType> ALL = Arrays.asList(SOFTWAREVERSION, SOFTWARE, SOFTWAREMODULE, CODEFRAGMENT);
}
