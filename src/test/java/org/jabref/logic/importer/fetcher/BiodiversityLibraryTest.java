package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("checkstyle:EmptyLineSeparator")
@FetcherTest
public class BiodiversityLibraryTest {
    private final BiodiversityLibrary fetcher = new BiodiversityLibrary();
    private final String BASE_URL = "https://www.biodiversitylibrary.org/api3?";
    private final String RESPONSE_FORMAT = "&format=json";
    private final BuildInfo buildInfo = new BuildInfo();

    @Test
    public void testGetName() {
        assertEquals("Biodiversity Heritage", fetcher.getName());
        assertNotEquals("Biodiversity Heritage Library", fetcher.getName());
        assertNotEquals("Biodiversity Library", fetcher.getName());
    }

    @Test
    public void biodiversityHeritageApiKeyIsNotEmpty() {
        BuildInfo buildInfo = new BuildInfo();
        assertNotNull(buildInfo.biodiversityHeritageApiKey);
    }

    @Test
    public void baseURLConstruction() throws MalformedURLException, URISyntaxException {

        String baseURL = fetcher.getBaseURL().toString();
        assertEquals(BASE_URL
                        .concat("apikey=")
                        .concat(buildInfo.biodiversityHeritageApiKey)
                        .concat(RESPONSE_FORMAT),
                baseURL);

    }

    @ParameterizedTest
    @ValueSource(strings = {"1234", "331", "121"})
    public void getPartMetadaUrl(String id) throws MalformedURLException, URISyntaxException {

        String expected_base = (BASE_URL
                .concat("apikey=")
                .concat(buildInfo.biodiversityHeritageApiKey)
                .concat(RESPONSE_FORMAT)
                .concat("&op=GetPartMetadata&pages=f&names=f")
                .concat("&id=")
        );

        assertEquals(expected_base.concat(id), fetcher.getPartMetadataURL(id).toString());

    }

    @Test
    public void getItemMetadaUrl() throws MalformedURLException, URISyntaxException {
        String id = "1234";
        String expected_base = (BASE_URL
                .concat("apikey=")
                .concat(buildInfo.biodiversityHeritageApiKey)
                .concat(RESPONSE_FORMAT)
                .concat("&op=GetItemMetadata&pages=f&ocr=f&ocr=f")
                .concat("&id=")
        );
        String expected = expected_base.concat(id);

        assertEquals(expected, fetcher.getItemMetadataURL(id).toString());

        id = "4321";
        expected = expected_base.concat(id);
        assertEquals(expected, fetcher.getItemMetadataURL(id).toString());

        id = "331";
        expected = expected_base.concat(id);
        assertEquals(expected, fetcher.getItemMetadataURL(id).toString());

    }

    @Test
    public void jsonResultToBibEntry() {
        JSONObject input = new JSONObject("{\n\"BHLType\": \"Part\",\n\"FoundIn\": \"Metadata\",\n\"Volume\": \"3\",\n\"Authors\": [\n{\n\"Name\": \"Dimmock, George,\"\n}\n],\n\"PartUrl\": \"https://www.biodiversitylibrary.org/part/181199\",\n\"PartID\": \"181199\",\n\"Genre\": \"Article\",\n\"Title\": \"The Cocoons of Cionus Scrophulariae\",\n\"ContainerTitle\": \"Psyche.\",\n\"Date\": \"1882\",\n\"PageRange\": \"411--413\"\n}");
        BibEntry expect = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "The Cocoons of Cionus Scrophulariae")
                .withField(StandardField.AUTHOR, "Dimmock, George, ")
                .withField(StandardField.PAGES, "411--413")
                .withField(StandardField.DATE, "1882")
                .withField(StandardField.JOURNALTITLE, "Psyche.")
                .withField(StandardField.VOLUME, "3");

        assertEquals(expect, fetcher.jsonResultToBibEntry(input));

         input = new JSONObject("{\n" +
                 "            \"BHLType\": \"Item\",\n" +
                 "            \"FoundIn\": \"Metadata\",\n" +
                 "            \"ItemID\": \"174333\",\n" +
                 "            \"TitleID\": \"96205\",\n" +
                 "            \"ItemUrl\": \"https://www.biodiversitylibrary.org/item/174333\",\n" +
                 "            \"TitleUrl\": \"https://www.biodiversitylibrary.org/bibliography/96205\",\n" +
                 "            \"MaterialType\": \"Published material\",\n" +
                 "            \"PublisherPlace\": \"Salisbury\",\n" +
                 "            \"PublisherName\": \"Frederick A. Blake,\",\n" +
                 "            \"PublicationDate\": \"1861\",\n" +
                 "            \"Authors\": [\n" +
                 "                {\n" +
                 "                    \"Name\": \"George, George\"\n" +
                 "                }\n" +
                 "            ],\n" +
                 "            \"Genre\": \"Book\",\n" +
                 "            \"Title\": \"Potatoes : the poor man's own crop : illustrated with plates, showing the decay and disease of the potatoe [sic] : with hints to improve the land and life of the poor man : published to aid the Industrial Marlborough Exhibition\"\n" +
                 "        }");
         expect = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Potatoes : the poor man's own crop : illustrated with plates, showing the decay and disease of the potatoe [sic] : with hints to improve the land and life of the poor man : published to aid the Industrial Marlborough Exhibition")
                .withField(StandardField.AUTHOR, "George, George ")
                .withField(StandardField.YEAR, "1861")
                .withField(StandardField.PUBSTATE, "Salisbury")
                .withField(StandardField.PUBLISHER, "Frederick A. Blake,");
        assertEquals(expect, fetcher.jsonResultToBibEntry(input));

        input = new JSONObject("{\n" +
                "            \"BHLType\": \"Item\",\n" +
                "            \"FoundIn\": \"Metadata\",\n" +
                "            \"ItemID\": \"200116\",\n" +
                "            \"TitleID\": \"115108\",\n" +
                "            \"ItemUrl\": \"https://www.biodiversitylibrary.org/item/200116\",\n" +
                "            \"TitleUrl\": \"https://www.biodiversitylibrary.org/bibliography/115108\",\n" +
                "            \"MaterialType\": \"Published material\",\n" +
                "            \"PublisherPlace\": \"Washington\",\n" +
                "            \"PublisherName\": \"Government Prining Office,\",\n" +
                "            \"PublicationDate\": \"1911\",\n" +
                "            \"Authors\": [\n" +
                "                {\n" +
                "                    \"Name\": \"Whitaker, George M. (George Mason)\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"Genre\": \"Book\",\n" +
                "            \"Title\": \"The extra cost of producing clean milk.\"\n" +
                "        }");
        expect = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "The extra cost of producing clean milk.")
                .withField(StandardField.AUTHOR, "Whitaker, George M. (George Mason) ")
                .withField(StandardField.YEAR, "1911")
                .withField(StandardField.PUBSTATE, "Washington")
                .withField(StandardField.PUBLISHER, "Government Prining Office,");
        assertEquals(expect, fetcher.jsonResultToBibEntry(input));

    }

}
