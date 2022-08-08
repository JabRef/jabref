package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.BiblatexSoftwareField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;

public class BiblatexSoftwareEntryTypeDefinitions {
    private static final BibEntryType SOFTWARE = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Software)
            .withImportantFields(StandardField.DATE, StandardField.DOI, StandardField.EPRINTTYPE, StandardField.EPRINTCLASS, StandardField.EPRINT,
                    StandardField.EDITOR, StandardField.FILE, BiblatexSoftwareField.HALID, BiblatexSoftwareField.HALVERSION, StandardField.INSTITUTION, BiblatexSoftwareField.INTRODUCEDIN,
                    BiblatexSoftwareField.LICENSE, StandardField.MONTH, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.PUBLISHER, StandardField.RELATED,
                    BiblatexSoftwareField.RELATEDSTRING, BiblatexSoftwareField.REPOSITORY, BiblatexSoftwareField.SWHID, StandardField.URLDATE, StandardField.VERSION)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.URL, StandardField.VERSION, StandardField.YEAR)
            .build();

    private static final BibEntryType SOFTWAREVERSION = new BibEntryTypeBuilder()
            .withType(BiblatexSoftwareEntryType.SoftwareVersion)
            .withImportantFields(StandardField.DATE, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, BiblatexSoftwareField.HALID, BiblatexSoftwareField.HALVERSION,
                    StandardField.INSTITUTION, BiblatexSoftwareField.INTRODUCEDIN, BiblatexSoftwareField.LICENSE, StandardField.MONTH, StandardField.NOTE, StandardField.ORGANIZATION,
                    StandardField.PUBLISHER, StandardField.RELATED, BiblatexSoftwareField.RELATEDTYPE, BiblatexSoftwareField.RELATEDSTRING,
                    BiblatexSoftwareField.REPOSITORY, BiblatexSoftwareField.SWHID, StandardField.SUBTITLE, StandardField.URLDATE)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.URL, StandardField.YEAR, StandardField.VERSION)
            .withDetailFields(StandardField.DATE, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, BiblatexSoftwareField.HALID, BiblatexSoftwareField.HALVERSION,
                    StandardField.INSTITUTION, BiblatexSoftwareField.INTRODUCEDIN, BiblatexSoftwareField.LICENSE, StandardField.MONTH, StandardField.NOTE, StandardField.ORGANIZATION,
                    StandardField.PUBLISHER, StandardField.RELATED, BiblatexSoftwareField.RELATEDTYPE, BiblatexSoftwareField.RELATEDSTRING,
                    BiblatexSoftwareField.REPOSITORY, BiblatexSoftwareField.SWHID, StandardField.SUBTITLE, StandardField.URLDATE)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.URL, StandardField.YEAR)
            .build();
    private static final BibEntryType SOFTWAREMODULE = new BibEntryTypeBuilder()
            .withType(BiblatexSoftwareEntryType.SoftwareModule)
            .withImportantFields(StandardField.DATE, StandardField.DOI, StandardField.EPRINTTYPE, StandardField.EPRINTCLASS, StandardField.EPRINT,
                    StandardField.EDITOR, StandardField.FILE, BiblatexSoftwareField.HALID, BiblatexSoftwareField.HALVERSION, StandardField.INSTITUTION, BiblatexSoftwareField.INTRODUCEDIN,
                    BiblatexSoftwareField.LICENSE, StandardField.MONTH, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.PUBLISHER, StandardField.RELATED,
                    BiblatexSoftwareField.RELATEDSTRING, BiblatexSoftwareField.REPOSITORY, BiblatexSoftwareField.SWHID, StandardField.URLDATE, StandardField.VERSION)
            .withRequiredFields(StandardField.AUTHOR, StandardField.SUBTITLE, StandardField.URL, StandardField.YEAR)
            .build();

    private static final BibEntryType CODEFRAGMENT = new BibEntryTypeBuilder()
            .withType(BiblatexSoftwareEntryType.CodeFragment)
            .withImportantFields(StandardField.DATE, StandardField.DOI, StandardField.EPRINTTYPE, StandardField.EPRINTCLASS, StandardField.EPRINT,
                    StandardField.EDITOR, StandardField.FILE, BiblatexSoftwareField.HALID, BiblatexSoftwareField.HALVERSION, StandardField.INSTITUTION, BiblatexSoftwareField.INTRODUCEDIN,
                    BiblatexSoftwareField.LICENSE, StandardField.MONTH, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.PUBLISHER, StandardField.RELATED,
                    BiblatexSoftwareField.RELATEDSTRING, BiblatexSoftwareField.REPOSITORY, BiblatexSoftwareField.SWHID, StandardField.URLDATE, StandardField.VERSION)
            .withRequiredFields(StandardField.URL)
            .build();

    public static final List<BibEntryType> ALL = Arrays.asList(SOFTWAREVERSION, SOFTWARE, SOFTWAREMODULE, CODEFRAGMENT);
}
