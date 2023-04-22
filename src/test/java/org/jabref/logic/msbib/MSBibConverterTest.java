package org.jabref.logic.msbib;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MSBibConverterTest {

    private final BibEntry BIB_ENTRY_TEST = new BibEntry(StandardEntryType.InProceedings)
            .withField(StandardField.AUTHOR, "Igor Steinmacher and Tayana Uchoa Conte and Christoph Treude and Marco Aurélio Gerosa")
            .withField(StandardField.DATE, "14-22 May 2016")
            .withField(StandardField.YEAR, "2016")
            .withField(StandardField.EVENTDATE, "14-22 May 2016")
            .withField(StandardField.EVENTTITLEADDON, "Austin, TX, USA")
            .withField(StandardField.LOCATION, "Austin, TX, USA")
            .withField(StandardField.DOI, "10.1145/2884781.2884806")
            .withField(StandardField.JOURNALTITLE, "2016 IEEE/ACM 38th International Conference on Software Engineering (ICSE)")
            .withField(StandardField.PAGES, "273--284")
            .withField(StandardField.ISBN, "978-1-5090-2071-3")
            .withField(StandardField.ISSN, "1558-1225")
            .withField(StandardField.LANGUAGE, "english")
            .withField(StandardField.PUBLISHER, "IEEE")
            .withField(StandardField.KEYWORDS, "Portals, Documentation, Computer bugs, Joining processes, Industries, Open source software, Newcomers, Newbies, Novices, Beginners, Open Source Software, Barriers, Obstacles, Onboarding, Joining Process")
            .withField(StandardField.TITLE, "Overcoming Open Source Project Entry Barriers with a Portal for Newcomers")
            .withField(StandardField.FILE, ":https\\://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=7886910:PDF");

    private BibEntry entry;

    @Test
    void convert() {
        entry = BIB_ENTRY_TEST;
        MSBibConverter.convert(entry);

        assertEquals(Optional.of("english"), entry.getField(StandardField.LANGUAGE));
    }
}
