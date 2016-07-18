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
package net.sf.jabref.logic.exporter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.bibtex.BibEntryWriter;
import net.sf.jabref.logic.bibtex.LatexFieldFormatter;
import net.sf.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.CustomEntryType;

public class BibtexDatabaseWriter<E extends SaveSession> extends BibDatabaseWriter<E> {

    private static final String STRING_PREFIX = "@String";
    private static final String COMMENT_PREFIX = "@Comment";
    private static final String PREAMBLE_PREFIX = "@Preamble";

    private final JabRefPreferences jabRefPreferences;
    public BibtexDatabaseWriter(SaveSessionFactory<E> saveSessionFactory, JabRefPreferences jabRefPreferences) {
        super(saveSessionFactory);
        this.jabRefPreferences = jabRefPreferences;
    }

    @Override
    protected void writeEpilogue(String epilogue) throws SaveException {
        if (!StringUtil.isNullOrEmpty(epilogue)) {
            try {
                getWriter().write(Globals.NEWLINE);
                getWriter().write(epilogue);
                getWriter().write(Globals.NEWLINE);
            } catch (IOException e) {
                throw new SaveException(e);
            }
        }
    }

    @Override
    protected void writeMetaDataItem(Map.Entry<String, String> metaItem) throws SaveException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Globals.NEWLINE);
        stringBuilder.append(COMMENT_PREFIX + "{").append(MetaData.META_FLAG).append(metaItem.getKey()).append(":");
        stringBuilder.append(metaItem.getValue());
        stringBuilder.append("}");
        stringBuilder.append(Globals.NEWLINE);

        try {
            getWriter().write(stringBuilder.toString());
        } catch (IOException e) {
            throw new SaveException(e);
        }
    }

    @Override
    protected void writePreamble(String preamble) throws SaveException {
        if (!StringUtil.isNullOrEmpty(preamble)) {
            try {
                getWriter().write(Globals.NEWLINE);
                getWriter().write(PREAMBLE_PREFIX + "{");
                getWriter().write(preamble);
                getWriter().write('}' + Globals.NEWLINE);
            } catch (IOException e) {
                throw new SaveException(e);
            }
        }
    }

    @Override
    protected void writeString(BibtexString bibtexString, boolean isFirstString, int maxKeyLength, Boolean reformatFile) throws SaveException {
        try {
            // If the string has not been modified, write it back as it was
            if (!reformatFile && !bibtexString.hasChanged()) {
                getWriter().write(bibtexString.getParsedSerialization());
                return;
            }

            // Write user comments
            String userComments = bibtexString.getUserComments();
            if(!userComments.isEmpty()) {
                getWriter().write(userComments + Globals.NEWLINE);
            }

            if (isFirstString) {
                getWriter().write(Globals.NEWLINE);
            }

            getWriter().write(STRING_PREFIX + "{" + bibtexString.getName() + StringUtil
                    .repeatSpaces(maxKeyLength - bibtexString.getName().length()) + " = ");
            if (bibtexString.getContent().isEmpty()) {
                getWriter().write("{}");
            } else {
                try {
                    String formatted = new LatexFieldFormatter(
                            LatexFieldFormatterPreferences.fromPreferences(jabRefPreferences))
                                    .format(bibtexString.getContent(),
                            LatexFieldFormatter.BIBTEX_STRING);
                    getWriter().write(formatted);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException(
                            "The # character is not allowed in BibTeX strings unless escaped as in '\\#'.\n" + "Before saving, please edit any strings containing the # character.");
                }
            }

            getWriter().write("}" + Globals.NEWLINE);
        } catch (IOException e) {
            throw new SaveException(e);
        }
    }

    @Override
    protected void writeEntryTypeDefinition(CustomEntryType customType) throws SaveException {
        try {
            getWriter().write(Globals.NEWLINE);
            getWriter().write(COMMENT_PREFIX + "{");
            getWriter().write(customType.getAsString());
            getWriter().write("}");
            getWriter().write(Globals.NEWLINE);
        } catch (IOException e) {
            throw new SaveException(e);
        }
    }

    @Override
    protected void writePrelogue(BibDatabaseContext bibDatabaseContext, Charset encoding) throws SaveException {
        if(encoding == null) {
            return;
        }

        // Writes the file encoding information.
        try {
            getWriter().write("% ");
            getWriter().write(Globals.ENCODING_PREFIX + encoding);
            getWriter().write(Globals.NEWLINE);
        } catch (IOException e) {
            throw new SaveException(e);
        }
    }

    @Override
    protected void writeEntry(BibEntry entry, BibDatabaseMode mode, Boolean isReformatFile) throws SaveException {
        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(
                new LatexFieldFormatter(LatexFieldFormatterPreferences.fromPreferences(jabRefPreferences)), true);
        try {
            bibtexEntryWriter.write(entry, getWriter(), mode, isReformatFile);
        } catch (IOException e) {
            throw new SaveException(e, entry);
        }
    }

    private Writer getWriter() {
        return getActiveSession().getWriter();
    }
}
