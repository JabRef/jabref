package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class MrDLibImporterTest {

    private MrDLibImporter importer;
    private BufferedReader input;

    @BeforeEach
    public void setUp() {
        importer = new MrDLibImporter();
        String testInput = "{\"label\": {\"label-description\": \"The following articles are similar to the document have currently selected.\", \"label-language\": \"en\", \"label-text\": \"Related Articles\"},    \"recommendation_set_id\": \"1\",    \"recommendations\": {        \"74021358\": {            \"abstract\": \"abstract\",            \"authors\":\"Sajovic, Marija\",            \"published_year\": \"2006\",            \"item_id_original\": \"12088644\",            \"keywords\": [                \"visoko\\u0161olski program Geodezija - smer Prostorska informatika\"            ],            \"language_provided\": \"sl\",            \"recommendation_id\": \"1\",            \"title\": \"The protection of rural lands with the spatial development strategy on the case of Hrastnik commune\",            \"url\": \"http://drugg.fgg.uni-lj.si/701/1/GEV_0199_Sajovic.pdf\"        },        \"82005804\": {            \"abstract\": \"abstract\",            \"year_published\": null,            \"item_id_original\": \"30145702\",            \"language_provided\": null,            \"recommendation_id\": \"2\",            \"title\": \"Engagement of the volunteers in the solution to the accidents in the South-Moravia region\"        },        \"82149599\": {            \"abstract\": \"abstract\",            \"year_published\": null,            \"item_id_original\": \"97690763\",            \"language_provided\": null,            \"recommendation_id\": \"3\",            \"title\": \"\\\"The only Father's word\\\". The relationship of the Father and the Son in the documents of saint John of the Cross\",            \"url\": \"http://www.nusl.cz/ntk/nusl-285711\"        },        \"84863921\": {            \"abstract\": \"abstract\",            \"authors\":\"Kaffa, Elena\",            \"year_published\": null,            \"item_id_original\": \"19397104\",            \"keywords\": [                \"BX\",                \"D111\"            ],            \"language_provided\": \"en\",            \"recommendation_id\": \"4\",            \"title\": \"Greek Church of Cyprus, the Morea and Constantinople during the Frankish Era (1196-1303)\"        },        \"88950992\": {            \"abstract\": \"abstract\",            \"authors\":\"Yasui, Kono\",            \"year_published\": null,            \"item_id_original\": \"38763657\",            \"language_provided\": null,            \"recommendation_id\": \"5\",            \"title\": \"A Phylogenetic Consideration on the Vascular Plants, Cotyledonary Node Including Hypocotyl Being Taken as the Ancestral Form : A Preliminary Note\"        }    }}";
        input = new BufferedReader(new StringReader(testInput));
    }

    @Test
    public void testGetDescription() {
        assertEquals("Takes valid JSON documents from the Mr. DLib API and parses them into a BibEntry", importer.getDescription());
    }

    @Test
    public void testGetName() {
        assertEquals("MrDLibImporter", importer.getName());
    }

    @Test
    public void testGetFileExtention() {
        assertEquals(StandardFileType.JSON, importer.getFileType());
    }

    @Test
    public void testImportDatabaseIsYearSetCorrectly() throws IOException {
        ParserResult parserResult = importer.importDatabase(input);

        List<BibEntry> resultList = parserResult.getDatabase().getEntries();

        assertEquals("2006",
                resultList.get(0).getLatexFreeField(StandardField.YEAR).get());
    }

    @Test
    public void testImportDatabaseIsTitleSetCorrectly() throws IOException {
        ParserResult parserResult = importer.importDatabase(input);

        List<BibEntry> resultList = parserResult.getDatabase().getEntries();

        assertEquals("The protection of rural lands with the spatial development strategy on the case of Hrastnik commune",
                resultList.get(0).getLatexFreeField(StandardField.TITLE).get());
    }

    @Test
    public void testImportDatabaseMin() throws IOException {
        ParserResult parserResult = importer.importDatabase(input);

        List<BibEntry> resultList = parserResult.getDatabase().getEntries();

        assertSame(5, resultList.size());
    }
}
