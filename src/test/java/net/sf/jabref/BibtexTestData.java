/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref;

import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;
import org.junit.Assert;

import java.io.StringReader;

public class BibtexTestData {
    public static BibtexEntry getBibtexEntry() {
        BibtexDatabase database = getBibtexDatabase();
        return database.getEntriesByKey("HipKro03")[0];
    }

    public static BibtexDatabase getBibtexDatabase() {
        StringReader reader = new StringReader(
                "@ARTICLE{HipKro03,\n"
                        + "  author = {Eric von Hippel and Georg von Krogh},\n"
                        + "  title = {Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science},\n"
                        + "  journal = {Organization Science},\n"
                        + "  year = {2003},\n"
                        + "  volume = {14},\n"
                        + "  pages = {209--223},\n"
                        + "  number = {2},\n"
                        + "  address = {Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA},\n"
                        + "  doi = {http://dx.doi.org/10.1287/orsc.14.2.209.14992}," + "\n"
                        + "  issn = {1526-5455}," + "\n" + "  publisher = {INFORMS}\n" + "}"
        );

        BibtexParser parser = new BibtexParser(reader);
        ParserResult result = null;
        try {
            result = parser.parse();
        } catch (Exception e) {
            Assert.fail();
        }
        return result.getDatabase();
    }
}
