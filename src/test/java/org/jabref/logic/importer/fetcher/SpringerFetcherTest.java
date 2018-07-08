package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
class SpringerFetcherTest {

    SpringerFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new SpringerFetcher();
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry expected = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        expected.setField("author", "Steinmacher, Igor and Gerosa, Marco and Conte, Tayana U. and Redmiles, David F.");
        expected.setField("date", "2018-06-14");
        expected.setField("doi", "10.1007/s10606-018-9335-z");
        expected.setField("issn", "0925-9724");
        expected.setField("journal", "Computer Supported Cooperative Work (CSCW)");
        expected.setField("month", "#jun#");
        expected.setField("pages", "1--44");
        expected.setField("publisher", "Springer");
        expected.setField("title", "Overcoming Social Barriers When Contributing to Open Source Software Projects");
        expected.setField("year", "2018");
        expected.setField("file", "online:http\\://link.springer.com/openurl/pdf?id=doi\\:10.1007/s10606-018-9335-z:PDF");
        expected.setField("abstract", "An influx of newcomers is critical to the survival, long-term success, and continuity of many Open Source Software (OSS) community-based projects. However, newcomers face many barriers when making their first contribution, leading in many cases to dropouts. Due to the collaborative nature of community-based OSS projects, newcomers may be susceptible to social barriers, such as communication breakdowns and reception issues. In this article, we report a two-phase study aimed at better understanding social barriers faced by newcomers. In the first phase, we qualitatively analyzed the literature and data collected from practitioners to identify barriers that hinder newcomers’ first contribution. We designed a model composed of 58 barriers, including 13 social barriers. In the second phase, based on the barriers model, we developed FLOSScoach, a portal to support newcomers making their first contribution. We evaluated the portal in a diary-based study and found that the portal guided the newcomers and reduced the need for communication. Our results provide insights for communities that want to support newcomers and lay a foundation for building better onboarding tools. The contributions of this paper include identifying and gathering empirical evidence of social barriers faced by newcomers; understanding how social barriers can be reduced or avoided by using a portal that organizes proper information for newcomers (FLOSScoach); presenting guidelines for communities and newcomers on how to reduce or avoid social barriers; and identifying new streams of research.");

        List<BibEntry> fetchedEntries = fetcher.performSearch("JabRef Social Barriers Steinmacher");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }
}
