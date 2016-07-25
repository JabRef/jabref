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
package net.sf.jabref.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.exporter.SavePreferences;

/**
 * This importer exists only to enable `--importToOpen someEntry.bib`
 *
 * It is NOT intended to import a bib file. This is done via the option action, which treats the metadata fields
 * The metadata is not required to be read here, as this class is NOT called at --import
 */
public class BibtexImporter extends ImportFormat {

    // Signature written at the top of the .bib file in earlier versions.
    private static final String SIGNATURE = "This file was created with JabRef";

    /**
     * @return true as we have no effective way to decide whether a file is in bibtex format or not. See
     *         https://github.com/JabRef/jabref/pull/379#issuecomment-158685726 for more details.
     */
    @Override
    public boolean isRecognizedFormat(BufferedReader reader) {
        Objects.requireNonNull(reader);
        return true;
    }

    @Override
    public ParserResult importDatabase(Path filePath, Charset defaultEncoding) throws IOException {
        // We want to check if there is a JabRef signature in the file, because that would tell us
        // which character encoding is used. However, to read the signature we must be using a compatible
        // encoding in the first place. Since the signature doesn't contain any fancy characters, we can
        // read it regardless of encoding, with either UTF-8 or UTF-16. That's the hypothesis, at any rate.
        // 8 bit is most likely, so we try that first:
        Optional<Charset> suppliedEncoding;
        try (BufferedReader utf8Reader = getUTF8Reader(filePath)) {
            suppliedEncoding = getSuppliedEncoding(utf8Reader);
        }
        // Now if that didn't get us anywhere, we check with the 16 bit encoding:
        if (!suppliedEncoding.isPresent()) {
            try (BufferedReader utf16Reader = getUTF16Reader(filePath)) {
                suppliedEncoding = getSuppliedEncoding(utf16Reader);
            }
        }

        if(suppliedEncoding.isPresent()) {
            return super.importDatabase(filePath, suppliedEncoding.get());
        } else {
            return super.importDatabase(filePath, defaultEncoding);
        }
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        return BibtexParser.parse(reader);
    }

    @Override
    public String getFormatName() {
        return "BibTeX";
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList(".bib");
    }

    @Override
    public String getDescription() {
        return "This importer exists only to enable `--importToOpen someEntry.bib`\n" +
                "It is NOT intended to import a bib file. This is done via the option action, which treats the metadata fields.\n" +
                "The metadata is not required to be read here, as this class is NOT called at --import.";
    }

    /**
     * Searches the file for "Encoding: myEncoding" and returns the found supplied encoding.
     */
    private static Optional<Charset> getSuppliedEncoding(BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Line does not start with %, so there are no comment lines for us and we can stop parsing
                if (!line.startsWith("%")) {
                    return Optional.empty();
                }

                // Only keep the part after %
                line = line.substring(1).trim();

                if (line.startsWith(BibtexImporter.SIGNATURE)) {
                    // Signature line, so keep reading and skip to next line
                } else if (line.startsWith(SavePreferences.ENCODING_PREFIX)) {
                    // Line starts with "Encoding: ", so the rest of the line should contain the name of the encoding
                    // Except if there is already a @ symbol signaling the starting of a BibEntry
                    Integer atSymbolIndex = line.indexOf('@');
                    String encoding;
                    if (atSymbolIndex > 0) {
                        encoding = line.substring(SavePreferences.ENCODING_PREFIX.length(), atSymbolIndex);
                    } else {
                        encoding = line.substring(SavePreferences.ENCODING_PREFIX.length());
                    }

                    return Optional.of(Charset.forName(encoding));
                } else {
                    // Line not recognized so stop parsing
                    return Optional.empty();
                }
            }
        } catch (IOException ignored) {
            // Ignored
        }
        return Optional.empty();
    }
}
