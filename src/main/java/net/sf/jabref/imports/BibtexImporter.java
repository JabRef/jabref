/*
 *  This file is part of JabRef.
 *
 *  JabRef is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  JabRef is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with JAbRef.  If not, see <http://www.gnu.org/licenses/>. 
 */

package net.sf.jabref.imports;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.OutputPrinter;

/**
 * Imported requried to support --importToOpen someEntry.bib 
 */
public class BibtexImporter extends ImportFormat {

    /**
     * @return false as that does not cause any harm in the current implementation of JabRef
     */
    @Override
    public boolean isRecognizedFormat(InputStream in) throws IOException {
        return BibtexParser.isRecognizedFormat(new InputStreamReader(in));
    }

    /**
     * Parses the given input stream.
     * Only plain bibtex entries are returned.
     * That especially means that metadata is ignored.
     * 
     * @param in the inputStream to read from
     * @param status the OutputPrinter to put status to
     * @return a list of BibTeX entries contained in the given inputStream
     */
    @Override
    public List<BibtexEntry> importEntries(InputStream in, OutputPrinter status)
            throws IOException {
        ParserResult pr = BibtexParser.parse(new InputStreamReader(in));
        return new ArrayList<BibtexEntry>(pr.getDatabase().getEntries());
    }

    @Override
    public String getFormatName() {
        return "BibTeX";
    }

    @Override
    public String getExtensions() {
        return "bib";
    }

}
