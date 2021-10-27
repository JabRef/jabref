package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
class SpringerFetcherTest implements SearchBasedFetcherCapabilityTest, PagedSearchFetcherTest {

    SpringerFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new SpringerFetcher();
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry firstArticle = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.AUTHOR, "Steinmacher, Igor and Balali, Sogol and Trinkenreich, Bianca and Guizani, Mariam and Izquierdo-Cortazar, Daniel and Cuevas Zambrano, Griselda G. and Gerosa, Marco Aurelio and Sarma, Anita")
            .withField(StandardField.DATE, "2021-09-09")
            .withField(StandardField.DOI, "10.1186/s13174-021-00140-z")
            .withField(StandardField.ISSN, "1867-4828")
            .withField(StandardField.JOURNAL, "Journal of Internet Services and Applications")
            .withField(StandardField.MONTH, "#sep#")
            .withField(StandardField.PAGES, "1--33")
            .withField(StandardField.NUMBER, "1")
            .withField(StandardField.VOLUME, "12")
            .withField(StandardField.PUBLISHER, "Springer")
            .withField(StandardField.TITLE, "Being a Mentor in open source projects")
            .withField(StandardField.YEAR, "2021")
            .withField(StandardField.FILE, ":https\\://www.biomedcentral.com/openurl/pdf?id=doi\\:10.1186/s13174-021-00140-z:PDF")
            .withField(StandardField.ABSTRACT, "Mentoring is a well-known way to help newcomers to Open Source Software (OSS) projects overcome initial contribution barriers. Through mentoring, newcomers learn to acquire essential technical, social, and organizational skills. Despite the importance of OSS mentors, they are understudied in the literature. Understanding who OSS project mentors are, the challenges they face, and the strategies they use can help OSS projects better support mentors’ work. In this paper, we employ a two-stage study to comprehensively investigate mentors in OSS. First, we identify the characteristics of mentors in the Apache Software Foundation, a large OSS community, using an online survey. We found that less experienced volunteer contributors are less likely to take on the mentorship role. Second, through interviews with OSS mentors (n=18), we identify the challenges that mentors face and how they mitigate them. In total, we identified 25 general mentorship challenges and 7 sub-categories of challenges regarding task recommendation. We also identified 13 strategies to overcome the challenges related to task recommendation. Our results provide insights for OSS communities, formal mentorship programs, and tool builders who design automated support for task assignment and internship.");

        BibEntry secondArticle = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Steinmacher, Igor and Gerosa, Marco and Conte, Tayana U. and Redmiles, David F.")
                .withField(StandardField.DATE, "2019-04-15")
                .withField(StandardField.DOI, "10.1007/s10606-018-9335-z")
                .withField(StandardField.ISSN, "0925-9724")
                .withField(StandardField.JOURNAL, "Computer Supported Cooperative Work (CSCW)")
                .withField(StandardField.MONTH, "#apr#")
                .withField(StandardField.PAGES, "247--290")
                .withField(StandardField.NUMBER, "1-2")
                .withField(StandardField.VOLUME, "28")
                .withField(StandardField.PUBLISHER, "Springer")
                .withField(StandardField.TITLE, "Overcoming Social Barriers When Contributing to Open Source Software Projects")
                .withField(StandardField.YEAR, "2019")
                .withField(StandardField.FILE, ":http\\://link.springer.com/openurl/pdf?id=doi\\:10.1007/s10606-018-9335-z:PDF")
                .withField(StandardField.ABSTRACT, "An influx of newcomers is critical to the survival, long-term success, and continuity of many Open Source Software (OSS) community-based projects. However, newcomers face many barriers when making their first contribution, leading in many cases to dropouts. Due to the collaborative nature of community-based OSS projects, newcomers may be susceptible to social barriers, such as communication breakdowns and reception issues. In this article, we report a two-phase study aimed at better understanding social barriers faced by newcomers. In the first phase, we qualitatively analyzed the literature and data collected from practitioners to identify barriers that hinder newcomers’ first contribution. We designed a model composed of 58 barriers, including 13 social barriers. In the second phase, based on the barriers model, we developed FLOSScoach, a portal to support newcomers making their first contribution. We evaluated the portal in a diary-based study and found that the portal guided the newcomers and reduced the need for communication. Our results provide insights for communities that want to support newcomers and lay a foundation for building better onboarding tools. The contributions of this paper include identifying and gathering empirical evidence of social barriers faced by newcomers; understanding how social barriers can be reduced or avoided by using a portal that organizes proper information for newcomers (FLOSScoach); presenting guidelines for communities and newcomers on how to reduce or avoid social barriers; and identifying new streams of research.");

        List<BibEntry> fetchedEntries = fetcher.performSearch("JabRef Social Barriers Steinmacher");
        assertEquals(List.of(firstArticle, secondArticle), fetchedEntries);
    }

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

    @Test
    void searchByEmptyQueryFindsNothing() throws Exception {
        assertEquals(Collections.emptyList(), fetcher.performSearch(""));
    }

    @Test
    @Disabled("Year search is currently broken, because the API returns mutliple years.")
    @Override
    public void supportsYearSearch() throws Exception {
    }

    @Test
    @Disabled("Year range search is not natively supported by the API, but can be emulated by multiple single year searches.")
    @Override
    public void supportsYearRangeSearch() throws Exception {
    }

    @Test
    public void supportsPhraseSearch() throws Exception {
        // Normal search should match due to Redmiles, Elissa M., phrase search on the other hand should not find it.
        BibEntry expected = new BibEntry(StandardEntryType.InCollection)
                .withField(StandardField.AUTHOR, "Booth, Kayla M. and Dosono, Bryan and Redmiles, Elissa M. and Morales, Miraida and Depew, Michael and Farzan, Rosta and Herman, Everett and Trahan, Keith and Tananis, Cindy")
                .withField(StandardField.DATE, "2018-01-01")
                .withField(StandardField.DOI, "10.1007/978-3-319-78105-1_75")
                .withField(StandardField.ISBN, "978-3-319-78104-4")
                .withField(StandardField.MONTH, "#jan#")
                .withField(StandardField.PUBLISHER, "Springer")
                .withField(StandardField.BOOKTITLE, "Transforming Digital Worlds")
                .withField(StandardField.TITLE, "Diversifying the Next Generation of Information Scientists: Six Years of Implementation and Outcomes for a Year-Long REU Program")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.FILE, ":http\\://link.springer.com/openurl/pdf?id=doi\\:10.1007/978-3-319-78105-1_75:PDF")
                .withField(StandardField.ABSTRACT, "The iSchool Inclusion Institute (i3) is a Research Experience for Undergraduates (REU) program in the US designed to address underrepresentation in the information sciences. i3 is a year-long, cohort-based program that prepares undergraduate students for graduate school in information science and is rooted in a research and leadership development curriculum. Using data from six years of i3 cohorts, we present in this paper a qualitative and quantitative evaluation of the program in terms of student learning, research production, and graduate school enrollment. We find that students who participate in i3 report significant learning gains in information-science- and graduate-school-related areas and that 52% of i3 participants enroll in graduate school, over 2 $$\\times $$ × the national average. Based on these and additional results, we distill recommendations for future implementations of similar programs to address underrepresentation in information science.");

        List<BibEntry> resultPhrase = fetcher.performSearch("author:\"Redmiles David\"");
        List<BibEntry> result = fetcher.performSearch("author:Redmiles David");

        // Phrase search should be a subset of the normal search result.
        Assertions.assertTrue(result.containsAll(resultPhrase));
        result.removeAll(resultPhrase);
        Assertions.assertEquals(Collections.singletonList(expected), result);
    }

    @Test
    public void supportsBooleanANDSearch() throws Exception {
        List<BibEntry> resultJustByAuthor = fetcher.performSearch("author:\"Redmiles, David\"");
        List<BibEntry> result = fetcher.performSearch("author:\"Redmiles, David\" AND journal:\"Computer Supported Cooperative Work\"");

        Assertions.assertTrue(resultJustByAuthor.containsAll(result));
        List<BibEntry> allEntriesFromCSCW = result.stream()
                                                  .filter(bibEntry -> bibEntry.getField(StandardField.JOURNAL).orElse("").equals("Computer Supported Cooperative Work (CSCW)"))
                                                  .collect(Collectors.toList());
        allEntriesFromCSCW.stream()
                          .map(bibEntry -> bibEntry.getField(StandardField.AUTHOR))
                          .filter(Optional::isPresent)
                          .map(Optional::get).forEach(authorField -> assertTrue(authorField.contains("Redmiles")));
    }

    @Override
    public SearchBasedFetcher getFetcher() {
        return fetcher;
    }

    @Override
    public List<String> getTestAuthors() {
        return List.of("Steinmacher, Igor", "Gerosa, Marco", "Conte, Tayana U.");
    }

    @Override
    public String getTestJournal() {
        return "Clinical Research in Cardiology";
    }

    @Override
    public PagedSearchBasedFetcher getPagedFetcher() {
        return fetcher;
    }
}
