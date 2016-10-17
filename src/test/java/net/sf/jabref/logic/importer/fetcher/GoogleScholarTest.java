package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.bibtex.FieldContentParserPreferences;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.support.DevEnvironment;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoogleScholarTest {

    private GoogleScholar finder;
    private BibEntry entry;

    @Before
    public void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentParserPreferences()).thenReturn(
                mock(FieldContentParserPreferences.class));
        finder = new GoogleScholar(importFormatPreferences);
        entry = new BibEntry();
    }

    @Test(expected = NullPointerException.class)
    public void rejectNullParameter() throws IOException, FetcherException {
        finder.findFullText(null);
        Assert.fail();
    }

    @Test
    public void requiresEntryTitle() throws IOException, FetcherException {
        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    public void linkFound() throws IOException, FetcherException {
        // CI server is blocked by Google
        Assume.assumeFalse(DevEnvironment.isCircleCI());

        entry.setField("title", "Towards Application Portability in Platform as a Service");

        Assert.assertEquals(
                Optional.of(new URL("https://www.uni-bamberg.de/fileadmin/uni/fakultaeten/wiai_lehrstuehle/praktische_informatik/Dateien/Publikationen/sose14-towards-application-portability-in-paas.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    public void noLinkFound() throws IOException, FetcherException {
        // CI server is blocked by Google
        Assume.assumeFalse(DevEnvironment.isCircleCI());

        entry.setField("title", "Pro WF: Windows Workflow in NET 3.5");

        Assert.assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    public void findSingleEntry() throws FetcherException {
        // CI server is blocked by Google
        Assume.assumeFalse(DevEnvironment.isCircleCI());

        entry.setType(BibtexEntryTypes.INPROCEEDINGS.getName());
        entry.setCiteKey("geiger2013detecting");
        entry.setField(FieldName.TITLE, "Detecting Interoperability and Correctness Issues in BPMN 2.0 Process Models.");
        entry.setField(FieldName.AUTHOR, "Geiger, Matthias and Wirtz, Guido");
        entry.setField(FieldName.BOOKTITLE, "ZEUS");
        entry.setField(FieldName.YEAR, "2013");
        entry.setField(FieldName.PAGES, "41--44");

        List<BibEntry> foundEntries = finder.performSearch("info:RExzBa3OlkQJ:scholar.google.com");

        Assert.assertEquals(Collections.singletonList(entry), foundEntries);
    }

    @Test
    public void find20Entries() throws FetcherException {
        // CI server is blocked by Google
        Assume.assumeFalse(DevEnvironment.isCircleCI());

        List<BibEntry> foundEntries = finder.performSearch("random test string");

        Assert.assertEquals(20, foundEntries.size());
    }
}
