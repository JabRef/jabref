/*  Copyright (C) 2003-2011 JabRef contributors.
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
import java.util.List;
import java.util.Objects;

import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.xmp.XMPUtil;

/**
 * Wraps the XMPUtility function to be used as an ImportFormat.
 */
public class PdfXmpImporter extends ImportFormat {

    @Override
    public String getFormatName() {
        return Localization.lang("XMP-annotated PDF");
    }

    @Override
    public List<String> getExtensions() {
        return null;
    }

    @Override
    public ParserResult importDatabase(InputStream in) throws IOException {
        Objects.requireNonNull(in);
        try {
            return new ParserResult(XMPUtil.readXMP(in));
        } catch (IOException exception) {
            return ParserResult.fromErrorMessage(exception.getLocalizedMessage());
        }
    }

    /**
     * Returns whether the given stream contains data that is a.) a pdf and b.)
     * contains at least one BibEntry.
     */
    @Override
    public boolean isRecognizedFormat(InputStream in) throws IOException {
        Objects.requireNonNull(in);
        return XMPUtil.hasMetadata(in);
    }

    @Override
    public String getId() {
        return "xmp";
    }

    @Override
    public String getDescription() {
        return null;
    }

}
