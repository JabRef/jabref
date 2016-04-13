/*  Copyright (C) 2003-2015 JabRef contributors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.ParserResult;

/**
 * This importer exists only to enable `--importToOpen someEntry.bib`
 *
 * It is NOT intended to import a bib file. This is done via the option action, which treats the metadata fields
 * The metadata is not required to be read here, as this class is NOT called at --import
 */
public class BibtexImporter extends ImportFormat {

    /**
     * @return true as we have no effective way to decide whether a file is in bibtex format or not. See
     *         https://github.com/JabRef/jabref/pull/379#issuecomment-158685726 for more details.
     */
    @Override
    public boolean isRecognizedFormat(InputStream in) {
        Objects.requireNonNull(in);
        return true;
    }

    @Override
    public ParserResult importDatabase(InputStream in) throws IOException {
        return BibtexParser.parse(ImportFormatReader.getReaderDefaultEncoding(in));
    }

    @Override
    public String getFormatName() {
        return "BibTeX";
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("bib");
    }

    @Override
    public String getDescription() {
        return null;
    }

}
