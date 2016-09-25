package net.sf.jabref.logic.importer.util;

import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class JSONEntryParserTest {

    private final JSONEntryParser jc = new JSONEntryParser();


    @Test
    public void testBibJSONConverter() {
        String jsonString = "{\n\"title\": \"Design of Finite Word Length Linear-Phase FIR Filters in the Logarithmic Number System Domain\",\n"
                + "\"journal\": {\n\"publisher\": \"Hindawi Publishing Corporation\",\n\"language\": ["
                + "\"English\"],\n\"title\": \"VLSI Design\",\"country\": \"US\",\"volume\": \"2014\""
                + "},\"author\":[{\"name\": \"Syed Asad Alam\"},{\"name\": \"Oscar Gustafsson\""
                + "}\n],\n\"link\":[{\"url\": \"http://dx.doi.org/10.1155/2014/217495\","
                + "\"type\": \"fulltext\"}],\"year\":\"2014\",\"identifier\":[{"
                + "\"type\": \"pissn\",\"id\": \"1065-514X\"},\n{\"type\": \"eissn\","
                + "\"id\": \"1563-5171\"},{\"type\": \"doi\",\"id\": \"10.1155/2014/217495\""
                + "}],\"created_date\":\"2014-05-09T19:38:31Z\"}\"";
        JSONObject jsonObject = new JSONObject(jsonString);
        BibEntry bibEntry = jc.parseBibJSONtoBibtex(jsonObject, ',');

        Assert.assertEquals("article", bibEntry.getType());
        Assert.assertEquals(Optional.of("VLSI Design"), bibEntry.getField("journal"));
        Assert.assertEquals(Optional.of("10.1155/2014/217495"), bibEntry.getField("doi"));
        Assert.assertEquals(Optional.of("Syed Asad Alam and Oscar Gustafsson"), bibEntry.getField("author"));
        Assert.assertEquals(
                Optional.of(
                        "Design of Finite Word Length Linear-Phase FIR Filters in the Logarithmic Number System Domain"),
                bibEntry.getField("title"));
        Assert.assertEquals(Optional.of("2014"), bibEntry.getField("year"));
    }

    @Test
    public void testSpringerJSONToBibtex() {
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
        BibEntry bibEntry = JSONEntryParser.parseSpringerJSONtoBibtex(jsonObject);
        Assert.assertEquals(Optional.of("1992"), bibEntry.getField("year"));
        Assert.assertEquals(Optional.of("5"), bibEntry.getField("number"));
        Assert.assertEquals(Optional.of("#sep#"), bibEntry.getField("month"));
        Assert.assertEquals(Optional.of("10.1007/BF01201962"), bibEntry.getField("doi"));
        Assert.assertEquals(Optional.of("8"), bibEntry.getField("volume"));
        Assert.assertEquals(Optional.of("Springer"), bibEntry.getField("publisher"));
        Assert.assertEquals(Optional.of("1992-09-01"), bibEntry.getField("date"));
    }

}
