package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import kong.unirest.json.JSONObject;
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

    @DisabledOnCIServer("Disable on CI Server to not hit the API call limit")
    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Article);
        expected.setField(StandardField.AUTHOR, "Steinmacher, Igor and Gerosa, Marco and Conte, Tayana U. and Redmiles, David F.");
        expected.setField(StandardField.DATE, "2019-04-15");
        expected.setField(StandardField.DOI, "10.1007/s10606-018-9335-z");
        expected.setField(StandardField.ISSN, "0925-9724");
        expected.setField(StandardField.JOURNAL, "Computer Supported Cooperative Work (CSCW)");
        expected.setField(StandardField.MONTH, "#apr#");
        expected.setField(StandardField.PAGES, "247--290");
        expected.setField(StandardField.NUMBER, "1-2");
        expected.setField(StandardField.VOLUME, "28");
        expected.setField(StandardField.PUBLISHER, "Springer");
        expected.setField(StandardField.TITLE, "Overcoming Social Barriers When Contributing to Open Source Software Projects");
        expected.setField(StandardField.YEAR, "2019");
        expected.setField(StandardField.FILE, "online:http\\://link.springer.com/openurl/pdf?id=doi\\:10.1007/s10606-018-9335-z:PDF");
        expected.setField(StandardField.ABSTRACT, "An influx of newcomers is critical to the survival, long-term success, and continuity of many Open Source Software (OSS) community-based projects. However, newcomers face many barriers when making their first contribution, leading in many cases to dropouts. Due to the collaborative nature of community-based OSS projects, newcomers may be susceptible to social barriers, such as communication breakdowns and reception issues. In this article, we report a two-phase study aimed at better understanding social barriers faced by newcomers. In the first phase, we qualitatively analyzed the literature and data collected from practitioners to identify barriers that hinder newcomers’ first contribution. We designed a model composed of 58 barriers, including 13 social barriers. In the second phase, based on the barriers model, we developed FLOSScoach, a portal to support newcomers making their first contribution. We evaluated the portal in a diary-based study and found that the portal guided the newcomers and reduced the need for communication. Our results provide insights for communities that want to support newcomers and lay a foundation for building better onboarding tools. The contributions of this paper include identifying and gathering empirical evidence of social barriers faced by newcomers; understanding how social barriers can be reduced or avoided by using a portal that organizes proper information for newcomers (FLOSScoach); presenting guidelines for communities and newcomers on how to reduce or avoid social barriers; and identifying new streams of research.");

        List<BibEntry> fetchedEntries = fetcher.performSearch("JabRef Social Barriers Steinmacher");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }

    @DisabledOnCIServer("Disable on CI Server to not hit the API call limit")
    @Test
    void testSpringerJSONToBibtex() {
        String jsonString = "{\r\n" + "            \"identifier\":\"doi:10.1007/BF01201962\",\r\n"
                + "            \"title\":\"Book reviews\",\r\n"
                + "            \"publicationName\":\"World Journal of Microbiology & Biotechnology\",\r\n"
                + "            \"issn\":\"1573-0972\",\r\n" + "            \"isbn\":\"\",\r\n"
                + "            \"doi\":\"10.1007/BF01201962\",\r\n" + "            \"publisher\":\"Springer\",\r\n"
                + "            \"publicationDate\":\"1992-09-01\",\r\n" + "            \"volume\":\"8\",\r\n"
                + "            \"number\":\"5\",\r\n" + "            \"startingPage\":\"550\",\r\n"
                + "            \"url\":\"http://dx.doi.org/10.1007/BF01201962\",\"copyright\":\"©1992 Rapid Communications of Oxford Ltd.\"\r\n"
                + "        }";

        JSONObject jsonObject = new JSONObject(jsonString);
        BibEntry bibEntry = SpringerFetcher.parseSpringerJSONtoBibtex(jsonObject);
        assertEquals(Optional.of("1992"), bibEntry.getField(StandardField.YEAR));
        assertEquals(Optional.of("5"), bibEntry.getField(StandardField.NUMBER));
        assertEquals(Optional.of("#sep#"), bibEntry.getField(StandardField.MONTH));
        assertEquals(Optional.of("10.1007/BF01201962"), bibEntry.getField(StandardField.DOI));
        assertEquals(Optional.of("8"), bibEntry.getField(StandardField.VOLUME));
        assertEquals(Optional.of("Springer"), bibEntry.getField(StandardField.PUBLISHER));
        assertEquals(Optional.of("1992-09-01"), bibEntry.getField(StandardField.DATE));
    }

    @DisabledOnCIServer("Disable on CI Server to not hit the API call limit")
    @Test
    void searchByEmptyQueryFindsNothing() throws Exception {
        assertEquals(Collections.emptyList(), fetcher.performSearch(""));
    }
}
