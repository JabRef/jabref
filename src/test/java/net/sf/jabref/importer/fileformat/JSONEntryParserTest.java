/*  Copyright (C) 2015 Oscar Gustafsson.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package net.sf.jabref.importer.fileformat;

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
        JSONObject jo = new JSONObject(jsonString);
        BibEntry be = jc.parseBibJSONtoBibtex(jo);

        Assert.assertEquals("article", be.getType());
        Assert.assertEquals("VLSI Design", be.getField("journal"));
        Assert.assertEquals("10.1155/2014/217495", be.getField("doi"));
        Assert.assertEquals("Syed Asad Alam and Oscar Gustafsson", be.getField("author"));
        Assert.assertEquals(
                "Design of Finite Word Length Linear-Phase FIR Filters in the Logarithmic Number System Domain",
                be.getField("title"));
        Assert.assertEquals("2014", be.getField("year"));
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

        JSONObject jo = new JSONObject(jsonString);
        BibEntry be = JSONEntryParser.parseSpringerJSONtoBibtex(jo);
        Assert.assertEquals("1992", be.getField("year"));
        Assert.assertEquals("5", be.getField("number"));
        Assert.assertEquals("#sep#", be.getField("month"));
        Assert.assertEquals("10.1007/BF01201962", be.getField("doi"));
        Assert.assertEquals("8", be.getField("volume"));
        Assert.assertEquals("Springer", be.getField("publisher"));
        Assert.assertEquals("1992-09-01", be.getField("date"));
    }

}
