/*  Copyright (C) 2016 JabRef contributors.
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
package net.sf.jabref.logic.journals;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.sf.jabref.logic.util.OS;

/**
 * This class provides handy static methodes to save abbreviations to the file system.
 */
public class AbbreviationWriter {

    /**
     * This method will write the list of abbreviations to a file on the file system specified by the given path.
     * If the file already exists its content will be overridden, otherwise a new file will be created.
     *
     * @param path to a file (doesn't have to exist just yet)
     * @param abbreviations as a list specifying which entries should be written
     * @throws IOException
     */
    public static void writeOrCreate(Path path, List<Abbreviation> abbreviations, Charset encoding) throws IOException {
        try (OutputStream outStream = Files.newOutputStream(path);
                OutputStreamWriter writer = new OutputStreamWriter(outStream, encoding)) {
            for (Abbreviation entry : abbreviations) {
                writer.write(entry.getName());
                writer.write(" = ");
                writer.write(entry.getAbbreviation());
                writer.write(OS.NEWLINE);
            }
        }

    }

}
