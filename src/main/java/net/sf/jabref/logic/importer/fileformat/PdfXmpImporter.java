/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

import net.sf.jabref.gui.FileExtensions;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.xmp.XMPPreferences;
import net.sf.jabref.logic.xmp.XMPUtil;

/**
 * Wraps the XMPUtility function to be used as an ImportFormat.
 */
public class PdfXmpImporter extends ImportFormat {

    private final XMPPreferences xmpPreferences;


    public PdfXmpImporter(XMPPreferences xmpPreferences) {
        this.xmpPreferences = xmpPreferences;
    }

    @Override
    public String getFormatName() {
        return Localization.lang("XMP-annotated PDF");
    }

    @Override
    public FileExtensions getExtensions() {
        return FileExtensions.XMP;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException(
                "PdfXmpImporter does not support importDatabase(BufferedReader reader)."
                        + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public ParserResult importDatabase(Path filePath, Charset defaultEncoding) {
        Objects.requireNonNull(filePath);
        try {
            return new ParserResult(XMPUtil.readXMP(filePath, xmpPreferences));
        } catch (IOException exception) {
            return ParserResult.fromErrorMessage(exception.getLocalizedMessage());
        }
    }

    @Override
    protected boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException(
                "PdfXmpImporter does not support isRecognizedFormat(BufferedReader reader)."
                        + "Instead use isRecognizedFormat(Path filePath, Charset defaultEncoding).");
    }

    /**
     * Returns whether the given stream contains data that is a.) a pdf and b.)
     * contains at least one BibEntry.
     */
    @Override
    public boolean isRecognizedFormat(Path filePath, Charset defaultEncoding) throws IOException {
        Objects.requireNonNull(filePath);
        return XMPUtil.hasMetadata(filePath, xmpPreferences);
    }

    @Override
    public String getId() {
        return "xmp";
    }

    @Override
    public String getDescription() {
        return "Wraps the XMPUtility function to be used as an ImportFormat.";
    }

}
