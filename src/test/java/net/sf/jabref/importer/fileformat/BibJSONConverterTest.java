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

import org.json.JSONObject;
import org.junit.Test;
import org.junit.Assert;

import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

public class BibJSONConverterTest {

    @Test
    public void testBibJSONConverter() {
        String jsonString = new String(
                "{\n\"title\": \"Design of Finite Word Length Linear-Phase FIR Filters in the Logarithmic Number System Domain\",\n"
                        + "\"journal\": {\n\"publisher\": \"Hindawi Publishing Corporation\",\n\"language\": ["
                        + "\"English\"],\n\"title\": \"VLSI Design\",\"country\": \"US\",\"volume\": \"2014\""
                        + "},\"author\":[{\"name\": \"Syed Asad Alam\"},{\"name\": \"Oscar Gustafsson\""
                        + "}\n],\n\"link\":[{\"url\": \"http://dx.doi.org/10.1155/2014/217495\","
                        + "\"type\": \"fulltext\"}],\"year\":\"2014\",\"identifier\":[{"
                        + "\"type\": \"pissn\",\"id\": \"1065-514X\"},\n{\"type\": \"eissn\","
                        + "\"id\": \"1563-5171\"},{\"type\": \"doi\",\"id\": \"10.1155/2014/217495\""
                        + "}],\"created_date\":\"2014-05-09T19:38:31Z\"}\"");
        JSONObject jo = new JSONObject(jsonString);
        BibtexEntry be = BibJSONConverter.BibJSONtoBibtex(jo);

        Assert.assertEquals(BibtexEntryTypes.ARTICLE, be.getType());
        Assert.assertEquals("VLSI Design", be.getField("journal"));
        Assert.assertEquals("10.1155/2014/217495", be.getField("doi"));
        Assert.assertEquals("Syed Asad Alam and Oscar Gustafsson", be.getField("author"));
        Assert.assertEquals(
                "Design of Finite Word Length Linear-Phase FIR Filters in the Logarithmic Number System Domain",
                be.getField("title"));
        Assert.assertEquals("2014", be.getField("year"));
    }
}
