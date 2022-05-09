package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class BiodiversityLibraryTest {
    private final String BASE_URL = "https://www.biodiversitylibrary.org/api3?";
    private final String RESPONSE_FORMAT = "&format=json";
    private final BuildInfo buildInfo = new BuildInfo();

    private BiodiversityLibrary fetcher;

    @BeforeEach
    void setUp() {
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        fetcher = new BiodiversityLibrary(importerPreferences);
    }

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
        String expected = fetcher
                .getTestUrl()
                .concat(buildInfo.biodiversityHeritageApiKey)
                .concat(RESPONSE_FORMAT);

        assertEquals(expected, fetcher.getBaseURL().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234", "331", "121"})
    public void getPartMetadaUrl(String id) throws MalformedURLException, URISyntaxException {
        String expected = fetcher
                .getTestUrl()
                .concat(buildInfo.biodiversityHeritageApiKey)
                .concat(RESPONSE_FORMAT)
                .concat("&op=GetPartMetadata&pages=f&names=f")
                .concat("&id=");

        assertEquals(expected.concat(id), fetcher.getPartMetadataURL(id).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234", "4321", "331"})
    public void getItemMetadaUrl(String id) throws MalformedURLException, URISyntaxException {
        String expected = fetcher
                .getTestUrl()
                .concat(buildInfo.biodiversityHeritageApiKey)
                .concat(RESPONSE_FORMAT)
                .concat("&op=GetItemMetadata&pages=f&ocr=f&ocr=f")
                .concat("&id=");

        assertEquals(expected.concat(id), fetcher.getItemMetadataURL(id).toString());
    }

    @Test
    public void testPerformSearch() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Book)
            .withField(StandardField.AUTHOR, "Parkinson, William,  and Temporary Home for Lost and Starving Dogs ")
            .withField(StandardField.EDITOR, "Brigham Young University")
            .withField(StandardField.LANGUAGE, "English")
            .withField(StandardField.PUBLISHER, "Harold B. Lee Library (archive.org)")
            .withField(StandardField.LOCATION, "London")
            .withField(StandardField.TITLE, "Lively sallies : after Punch")
            .withField(StandardField.URL, "https://www.biodiversitylibrary.org/item/253874")
            .withField(StandardField.YEAR, "1870");

        assertEquals(List.of(expected), fetcher.performSearch("dogs"));
    }

    @Test
    public void jsonResultToBibEntry() {
        JSONObject input = new JSONObject("{\n\"BHLType\": \"Part\",\n\"FoundIn\": \"Metadata\",\n\"Volume\": \"3\",\n\"Authors\": [\n{\n\"Name\": \"Dimmock, George,\"\n}\n],\n\"PartUrl\": \"https://www.biodiversitylibrary.org/part/181199\",\n\"PartID\": \"181199\",\n\"Genre\": \"Article\",\n\"Title\": \"The Cocoons of Cionus Scrophulariae\",\n\"ContainerTitle\": \"Psyche.\",\n\"Date\": \"1882\",\n\"PageRange\": \"411--413\"\n}");
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "The Cocoons of Cionus Scrophulariae")
                .withField(StandardField.AUTHOR, "Dimmock, George, ")
                .withField(StandardField.PAGES, "411--413")
                .withField(StandardField.DATE, "1882")
                .withField(StandardField.JOURNALTITLE, "Psyche.")
                .withField(StandardField.VOLUME, "3");

        assertEquals(expected, fetcher.jsonResultToBibEntry(input));

        input = new JSONObject("""
                {
                            "BHLType": "Item",
                            "FoundIn": "Metadata",
                            "ItemID": "174333",
                            "TitleID": "96205",
                            "ItemUrl": "https://www.biodiversitylibrary.org/item/174333",
                            "TitleUrl": "https://www.biodiversitylibrary.org/bibliography/96205",
                            "MaterialType": "Published material",
                            "PublisherPlace": "Salisbury",
                            "PublisherName": "Frederick A. Blake,",
                            "PublicationDate": "1861",
                            "Authors": [
                                {
                                    "Name": "George, George"
                                }
                            ],
                            "Genre": "Book",
                            "Title": "Potatoes : the poor man's own crop : illustrated with plates, showing the decay and disease of the potatoe [sic] : with hints to improve the land and life of the poor man : published to aid the Industrial Marlborough Exhibition"
                        }""");
         expected = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Potatoes : the poor man's own crop : illustrated with plates, showing the decay and disease of the potatoe [sic] : with hints to improve the land and life of the poor man : published to aid the Industrial Marlborough Exhibition")
                .withField(StandardField.AUTHOR, "George, George ")
                .withField(StandardField.YEAR, "1861")
                .withField(StandardField.PUBSTATE, "Salisbury")
                .withField(StandardField.PUBLISHER, "Frederick A. Blake,");
        assertEquals(expected, fetcher.jsonResultToBibEntry(input));

        input = new JSONObject("""
                {
                            "BHLType": "Item",
                            "FoundIn": "Metadata",
                            "ItemID": "200116",
                            "TitleID": "115108",
                            "ItemUrl": "https://www.biodiversitylibrary.org/item/200116",
                            "TitleUrl": "https://www.biodiversitylibrary.org/bibliography/115108",
                            "MaterialType": "Published material",
                            "PublisherPlace": "Washington",
                            "PublisherName": "Government Prining Office,",
                            "PublicationDate": "1911",
                            "Authors": [
                                {
                                    "Name": "Whitaker, George M. (George Mason)"
                                }
                            ],
                            "Genre": "Book",
                            "Title": "The extra cost of producing clean milk."
                        }""");
        expected = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "The extra cost of producing clean milk.")
                .withField(StandardField.AUTHOR, "Whitaker, George M. (George Mason) ")
                .withField(StandardField.YEAR, "1911")
                .withField(StandardField.PUBSTATE, "Washington")
                .withField(StandardField.PUBLISHER, "Government Prining Office,");
        assertEquals(expected, fetcher.jsonResultToBibEntry(input));
    }
}
