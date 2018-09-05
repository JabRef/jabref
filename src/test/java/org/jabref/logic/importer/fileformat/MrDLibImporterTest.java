package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class MrDLibImporterTest {

    private MrDLibImporter importer;
    private BufferedReader input;
    private String testInput;

    @BeforeEach
    public void setUp() {
        importer = new MrDLibImporter();
        testInput = "{    \"label\": {        \"label-language\": \"en\",        \"label-text\": \"Related Items\"    },    \"recommendation-set-id\": \"1\",    \"recommendations\": {        \"74021358\": {            \"abstract\": \"The theme of diploma is the preservation of the rural lands with the spatial development strategy on the case of the Hrastnik commune. The rural land with the potential production which is appropriate for the rural use is very important natural source. The natural source is very important for the quality of living and the spatial development. For this reason the assurance of the validity of the usage of the rural lands and to preserve the rural land is one of the basic ways of the strategy of the spatial development of Slovenia. The task is compose into two parts. The first part is theoretical. It informed us with the situations and achievements of the spatial development in Slovenia. In brief it shows the professional starting-point, which are for the use of the legally regime to preserve the rural lands and have been made by Dr. A. Stritar and they are still actual. In the review of the new and old rural land legislation it shows the adaptation of the rural lands through the periods until today. The second part treats the practical case of the preservation of the rural lands on the case of the Hrastnik commune. Based on the professional foundation for the classification of the rural lands presents the spatial analyses with reference to the rural lands and the category of the rural lands of the commune. At the end of the chapter it shows the part of Brnica. Here the commune of Hrastnik in the procedure of the preparation of new spatial-act wants to change the purpose of the best rural lands with the high production potential for the enlargement of the settlement and adaptation of the cemetery\",            \"authors\": [                \"Sajovic, Marija\"         ],            \"date_published\": \"21-06-2006\",            \"item_id_original\": \"12088644\",            \"keywords\": [                \"visoko\\u0161olski program Geodezija - smer Prostorska informatika\"            ],            \"language_provided\": \"sl\",            \"recommendation_id\": \"1\",            \"title\": \"The protection of rural lands with the spatial development strategy on the case of Hrastnik commune\",            \"url\": \"http://drugg.fgg.uni-lj.si/701/1/GEV_0199_Sajovic.pdf\"        },        \"82005804\": {            \"abstract\": \"The theme of my diploma thesis is the engagement of the volunteers in the solution to the accidents in the South-Moravia region. In my diploma thesis I am involved in the issue of the engagement of the volunteers in the solution to the accidents in the South-Moravia region. I assume that the utilization of the potential of the volunteers is one of the possibilities of the acquisition of the forces, which could be used for the recovery of the consequences, post-traumatic care to struck persons and other activities within the solution to the accidents, all parallelly with the actions of the professional teams of the integrated rescue system bodies. The needs of the population in case of the accidents are rather differentiated, and the engagement of the volunteers completes the appointed forces and means of the integrated rescue system bodies. At present time, the adequate legal framework, modifying the engagement of the volunteers in the solution to the accidents, does not exist in the Czech Republic. Non-governmental non-profit organizations are united in the South-Moravia region into the Panel of the South-Moravia Region, which establishes the platform and enables the South-Moravia Region more effective engagement of the non-governmental non-profit organizations. The volunteers, organized in the Panel of the South-Moravia Region, are engaged in the solution to the accidents and the trainings organized in the level of the region, municipalities with widened competence and the integrated rescue system bodies\",            \"date_published\": null,            \"item_id_original\": \"30145702\",            \"language_provided\": null,            \"recommendation_id\": \"2\",            \"title\": \"Engagement of the volunteers in the solution to the accidents in the South-Moravia region\"        },        \"82149599\": {            \"abstract\": \"English Annotation (The work abstract) 5 key words: The Father, The Son, The Holy Spirit, The Holy Trinity, Saint John of the Cross \\\"The Only Father's Word\\\" The Relationship of the Father and the Son in documents of Saint John of the Cross The author presented the life of the saint in the first part of the work with the intention to put some key events of his life to the connection with the testimony of his texts. In the next part she analyzed those texts in the documents which are devoted to the relationship of the Father and the Son. Here we mainly talk about the twenty-second chapter of the second book, the Ascent of Mount Carmel, in which \\\" The Son is the only Father's word\\\". The father is active only in one way - begetting the Son; the begetting itself is Love, which is The Holy Spirit. The Son loves the Father and through this love he makes the Father love, he isn't passive. In the Romances, Saint John shows how the relationship of the Father and the Son in the Spirit is realized in the creation and incarnation. The poem the Source deepens our understanding of Son and Holy Spirit's proceeding from the Father and his acting in the Eucharist. In the third and the last part of the work, she made the synthesis of the knowledge gotten by the analysis of the given texts and she came to a conclusion that the..\",            \"date_published\": null,            \"item_id_original\": \"97690763\",            \"language_provided\": null,            \"recommendation_id\": \"3\",            \"title\": \"\\\"The only Father's word\\\". The relationship of the Father and the Son in the documents of saint John of the Cross\",            \"url\": \"http://www.nusl.cz/ntk/nusl-285711\"        },        \"84863921\": {            \"abstract\": \"This thesis provides an analytical presentation of the situation of the Greek Church of Cyprus, the Morea and Constantinople during the earlier part of the Frankish Era (1196-1303). It examines the establishment of the Latin Church in Constantinople, Cyprus and Achaea and it attempts to answer questions relating to the reactions of the Greek Church to the Latin conquests. It considers the similarities and differences in the establishment in Constantinople, the Morea and Cyprus, the diocesan structure, agreements regarding the fate of the Greek ecclesiastical properties, the payment of tithes and the agreements of 1220-1222. Moreover it analyses the relations of the Greek Church of Cyprus, the Greek Church of Constantinople and the Morea with the Latin Church. For instance it details the popes' involvement in the affairs of the Church in these three significant areas, the ecclesiastical differences between the Greek and the Latin Church, the behaviour of the Greek patriarchs, archbishops and bishops within the Greek Church, the reaction of the Greeks towards the establishment of the Latin Church, and significant events such as the martyrdom of the thirteen monks of Kantara and the promulgation of the Bulla Cypria. The third topic area pertains to the relationship of the Greek Church of the Morea, Constantinople and Cyprus with the secular authority. It discusses the attitude of the king of Cyprus, the rulers of the Morea and the emperor of Constantinople towards the problems between the Latin and Greeks, the relationship of the Latin nobility with the Greeks, and the involvement of the crown regarding the ecclesiastical property and possible explanations for the attitude of the Latin crown towards the Greeks\",            \"authors\": [                \"Kaffa, Elena\"            ],            \"date_published\": null,            \"item_id_original\": \"19397104\",            \"keywords\": [                \"BX\",                \"D111\"            ],            \"language_provided\": \"en\",            \"recommendation_id\": \"4\",            \"title\": \"Greek Church of Cyprus, the Morea and Constantinople during the Frankish Era (1196-1303)\"        },        \"88950992\": {            \"abstract\": \"1. The phylogeny map is the 4th-dimensional extension of a protoplast in which the generations repeated continuously. One generation in the phylogeny is the three-dimensional extension of the protoplast in certain period which is limited by a resting stage accompanying the reproductive phase. 2. It is considered that the ancestor of the seed plants was a hygrophytic multinuclear thallus having the structure like the cotyledonary ring including the hypocotyl in the plants of the present age. 3. In the second stage of the development in the phylogeny the thallus became uninuclear multicellular one, and the water storage cells differentiated in the body which gave the aerophytic habit for the plant. 4. Aerophytic habit gave the great change to the plant not only morphologically but also in the physiological natures. The epidermis having stomata differentiated. The mesophyll became to serve for the photosynthesis instead of the epidermis. The arrest of the escape of the metabolites from the surface of the body, the accumulation of the photosynthetic products, and the evaporation of water from the body surface lead the\\ndifferentiation of conductive tissues, the phloem and xylem. 5. The transfer of the nutrients and also the metabolites, especially such as the activator of the nuclear division, influenced to differentiate the secondary meristem. The growing point of the root and the primordium of the first node of the stem including the leaves and internode produced from the meristems. The embryo and bud formation are considered. 6. The appearance of the bud lead the formation of the complicated reproductive organ called the flower. The qualitative differences between the MiSI, and MaSI, and the migration of the SI were considered. 7. The ancestor of the Pteridophyte must be entirely different from that of the seed plants. It was a uninuclear multicellular plant having the growing point at the distal end of the body\",            \"authors\": [                \"Yasui, Kono\"            ],            \"date_published\": null,            \"item_id_original\": \"38763657\",            \"language_provided\": null,            \"recommendation_id\": \"5\",            \"title\": \"A Phylogenetic Consideration on the Vascular Plants, Cotyledonary Node Including Hypocotyl Being Taken as the Ancestral Form : A Preliminary Note\"        }    }}";
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
    public void testImportDatabaseIsHtmlSetCorrectly() throws IOException {
        ParserResult parserResult = importer.importDatabase(input);

        List<BibEntry> resultList = parserResult.getDatabase().getEntries();

        assertEquals(
                     "<a href='http://drugg.fgg.uni-lj.si/701/1/GEV_0199_Sajovic.pdf.'><font color=#000000 size=4 face=Arial, Helvetica, sans-serif>The protection of rural lands with the spatial development strategy on the case of Hrastnik commune</font></a>. <font color=#000000 size=4 face=Arial, Helvetica, sans-serif>Sajovic, Marija. <i></i> 21-06-2006</font>",
                resultList.get(0).getField("html_representation").get());
    }

    @Test
    public void testImportDatabaseIsYearSetCorrectly() throws IOException {
        ParserResult parserResult = importer.importDatabase(input);

        List<BibEntry> resultList = parserResult.getDatabase().getEntries();

        assertEquals("21-06-2006",
                resultList.get(0).getLatexFreeField(FieldName.YEAR).get());
    }

    @Test
    public void testImportDatabaseIsTitleSetCorrectly() throws IOException {
        ParserResult parserResult = importer.importDatabase(input);

        List<BibEntry> resultList = parserResult.getDatabase().getEntries();

        assertEquals("The protection of rural lands with the spatial development strategy on the case of Hrastnik commune",
                resultList.get(0).getLatexFreeField(FieldName.TITLE).get());
    }

    @Test
    public void testImportDatabaseMin() throws IOException {
        ParserResult parserResult = importer.importDatabase(input);

        List<BibEntry> resultList = parserResult.getDatabase().getEntries();

        assertSame(5, resultList.size());
    }
}
