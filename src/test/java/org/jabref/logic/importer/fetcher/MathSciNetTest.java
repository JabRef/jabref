package org.jabref.logic.importer.fetcher;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class MathSciNetTest {
    MathSciNet fetcher;
    private BibEntry ratiuEntry;

    @BeforeEach
    void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        fetcher = new MathSciNet(importFormatPreferences);

        ratiuEntry = new BibEntry();
        ratiuEntry.setType(StandardEntryType.Article);
        ratiuEntry.setCitationKey("MR3537908");
        ratiuEntry.setField(StandardField.AUTHOR, "Chechkin, Gregory A. and Ratiu, Tudor S. and Romanov, Maxim S. and Samokhin, Vyacheslav N.");
        ratiuEntry.setField(StandardField.TITLE, "Existence and uniqueness theorems for the two-dimensional {E}ricksen-{L}eslie system");
        ratiuEntry.setField(StandardField.JOURNAL, "Journal of Mathematical Fluid Mechanics");
        ratiuEntry.setField(StandardField.VOLUME, "18");
        ratiuEntry.setField(StandardField.YEAR, "2016");
        ratiuEntry.setField(StandardField.NUMBER, "3");
        ratiuEntry.setField(StandardField.PAGES, "571--589");
        ratiuEntry.setField(StandardField.KEYWORDS, "76A15 (35A01 35A02 35K61 82D30)");
        ratiuEntry.setField(StandardField.MR_NUMBER, "3537908");
        ratiuEntry.setField(StandardField.ISSN, "1422-6928, 1422-6952");
        ratiuEntry.setField(StandardField.DOI, "10.1007/s00021-016-0250-0");
    }

    @Test
    void searchByEntryFindsEntry() throws Exception {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.TITLE, "existence");
        searchEntry.setField(StandardField.AUTHOR, "Ratiu");
        searchEntry.setField(StandardField.JOURNAL, "fluid");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(Collections.singletonList(ratiuEntry), fetchedEntries);
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to MathSciNet and thus gets 401 response")
    void searchByIdInEntryFindsEntry() throws Exception {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.MR_NUMBER, "3537908");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(Collections.singletonList(ratiuEntry), fetchedEntries);
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to MathSciNet and thus gets 401 response")
    void searchByQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("Existence and uniqueness theorems Two-Dimensional Ericksen Leslie System");
        assertFalse(fetchedEntries.isEmpty());
        assertEquals(ratiuEntry, fetchedEntries.get(1));
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to MathSciNet and thus gets 401 response")
    void searchByIdFindsEntry() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3537908");
        assertEquals(Optional.of(ratiuEntry), fetchedEntry);
    }

    @Test
    void testGetParser() throws Exception {
        String json = "{\"results\":[{\"mrnumber\":4158623,\"titles\":{\"title\":\"On the weights of general MDS codes\",\"translatedTitle\":null},\"entryType\":\"J\",\"primaryClass\":{\"code\":\"94B65\",\"description\":\"Bounds on codes\"},\"authors\":[{\"id\":758603,\"name\":\"Alderson, Tim L.\"}],\"issue\":{\"issue\":{\"pubYear\":2020,\"pubYear2\":null,\"volume\":\"66\",\"volume2\":null,\"volume3\":null,\"number\":\"9\",\"journal\":{\"id\":2292,\"shortTitle\":\"IEEE Trans. Inform. Theory\",\"issn\":\"0018-9448\"},\"volSlash\":\"N\",\"isbn\":null,\"elementOrd\":null},\"translatedIssue\":null},\"book\":null,\"reviewer\":{\"public\":true,\"reviewers\":[{\"authId\":889610,\"rvrCode\":85231,\"name\":\"Jitman, Somphong\"}]},\"paging\":{\"paging\":{\"text\":\"5414--5418\"},\"translatedPaging\":null},\"counts\":{\"cited\":2},\"itemType\":\"Reviewed\",\"articleUrl\":\"https://doi.org/10.1109/TIT.2020.2977319\",\"openURL\":{\"imageLink\":\"http://www.lib.unb.ca/img/asin/res20x150.gif\",\"targetLink\":\"https://unb.on.worldcat.org/atoztitles/link?ctx_ver=Z39.88-2004&ctx_enc=info:ofi/enc:UTF-8&rfr_id=info:sid/ams.org:MathSciNet&rft_val_fmt=info:ofi/fmt:kev:mtx:journal&rft_id=info:doi/10.1109%2FTIT.2020.2977319&rft.aufirst=Tim&rft.auinit=TL&rft.auinit1=T&rft.auinitm=L&rft.aulast=Alderson&rft.genre=article&rft.issn=00189448&rft.title=Institute of Electrical and Electronics Engineers  Transactions on Information Theory&rft.atitle=On the weights of general MDS codes&rft.stitle=IEEE Trans  Inform  Theory&rft.volume=66&rft.date=2020&rft.spage=5414&rft.epage=5418&rft.pages=5414-5418&rft.issue=9&rft.jtitle=Institute of Electrical and Electronics Engineers  Transactions on Information Theory\",\"textLink\":\"\"},\"prePubl\":null,\"public\":true}],\"total\":31}";

        InputStream inputStream = new java.io.ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        List<BibEntry> entries = fetcher.getParser().parseEntries(inputStream);

        assertNotNull(entries);
        assertFalse(entries.isEmpty());
        assertEquals(1, entries.size());

        BibEntry parsedEntry = entries.get(0);
        assertEquals("On the weights of general MDS codes", parsedEntry.getField(StandardField.TITLE).orElse(null));
        assertEquals("Alderson, Tim L.", parsedEntry.getField(StandardField.AUTHOR).orElse(null));
        assertEquals("2020", parsedEntry.getField(StandardField.YEAR).orElse(null));
        assertEquals("IEEE Trans. Inform. Theory", parsedEntry.getField(StandardField.JOURNAL).orElse(null));
        assertEquals("66", parsedEntry.getField(StandardField.VOLUME).orElse(null));
        assertEquals("9", parsedEntry.getField(StandardField.NUMBER).orElse(null));
        assertEquals("5414--5418", parsedEntry.getField(StandardField.PAGES).orElse(null));
        assertEquals("4158623", parsedEntry.getField(StandardField.MR_NUMBER).orElse(null));
        assertEquals("Bounds on codes", parsedEntry.getField(StandardField.KEYWORDS).orElse(null));
        assertEquals("https://doi.org/10.1109/TIT.2020.2977319", parsedEntry.getField(StandardField.DOI).orElse(null));
        assertEquals("0018-9448", parsedEntry.getField(StandardField.ISSN).orElse(null));
    }
}
