/*
 * Copyright (C) 2003-2016 JabRef contributors.
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

package net.sf.jabref.exporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StringSaveSession extends SaveSession {

    private static final Log LOGGER = LogFactory.getLog(StringSaveSession.class);

    private final ByteArrayOutputStream outputStream;

    public StringSaveSession(Charset encoding, boolean backup) {
        this(encoding, backup, new ByteArrayOutputStream());
    }

    private StringSaveSession(Charset encoding, boolean backup, ByteArrayOutputStream outputStream) {
        super(encoding, backup, new VerifyingWriter(outputStream, encoding));
        this.outputStream = outputStream;
    }

    public String getStringValue() {
        try {
            return outputStream.toString(encoding.name());
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn(e);
            return "";
        }
    }

    @Override
    public void commit(Path file) throws SaveException {
        try {
            Files.write(file, outputStream.toByteArray());
        } catch (IOException e) {
            throw new SaveException(e);
        }
    }

    @Override
    public void cancel() {
        try {
            outputStream.close();
        } catch (IOException e) {
            LOGGER.warn(e);
        }
    }
}
