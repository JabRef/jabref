package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;

import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.strings.StringUtil;

public class BibtexDatabaseWriter extends BibDatabaseWriter {

    public static final String DATABASE_ID_PREFIX = "DBID:";
    private static final String STRING_PREFIX = "@String";
    private static final String COMMENT_PREFIX = "@Comment";
    private static final String PREAMBLE_PREFIX = "@Preamble";

    public BibtexDatabaseWriter(Writer writer) {
        super(writer);
    }

    @Override
    protected void writeEpilogue(String epilogue) throws IOException {
        if (!StringUtil.isNullOrEmpty(epilogue)) {
            getWriter().write(OS.NEWLINE);
            getWriter().write(epilogue);
            getWriter().write(OS.NEWLINE);
        }
    }

    @Override
    protected void writeMetaDataItem(Map.Entry<String, String> metaItem) throws IOException {
        getWriter().write(OS.NEWLINE);
        getWriter().write(COMMENT_PREFIX + "{");
        getWriter().write(MetaData.META_FLAG);
        getWriter().write(metaItem.getKey());
        getWriter().write(":");
        getWriter().write(metaItem.getValue());
        getWriter().write("}");
        getWriter().write(OS.NEWLINE);
    }

    @Override
    protected void writePreamble(String preamble) throws IOException {
        if (!StringUtil.isNullOrEmpty(preamble)) {
            getWriter().write(OS.NEWLINE);
            getWriter().write(PREAMBLE_PREFIX + "{");
            getWriter().write(preamble);
            getWriter().write('}' + OS.NEWLINE);
        }
    }

    @Override
    protected void writeString(BibtexString bibtexString, boolean isFirstString, int maxKeyLength, Boolean reformatFile,
                               LatexFieldFormatterPreferences latexFieldFormatterPreferences) throws IOException {
        // If the string has not been modified, write it back as it was
        if (!reformatFile && !bibtexString.hasChanged()) {
            getWriter().write(bibtexString.getParsedSerialization());
            return;
        }

        // Write user comments
        String userComments = bibtexString.getUserComments();
        if (!userComments.isEmpty()) {
            getWriter().write(userComments + OS.NEWLINE);
        }

        if (isFirstString) {
            getWriter().write(OS.NEWLINE);
        }

        getWriter().write(STRING_PREFIX + "{" + bibtexString.getName() + StringUtil
                .repeatSpaces(maxKeyLength - bibtexString.getName().length()) + " = ");
        if (bibtexString.getContent().isEmpty()) {
            getWriter().write("{}");
        } else {
            try {
                String formatted = new LatexFieldFormatter(latexFieldFormatterPreferences)
                        .format(bibtexString.getContent(),
                                LatexFieldFormatter.BIBTEX_STRING);
                getWriter().write(formatted);
            } catch (InvalidFieldValueException ex) {
                throw new IOException(ex);
            }
        }

        getWriter().write("}" + OS.NEWLINE);
    }

    @Override
    protected void writeEntryTypeDefinition(CustomEntryType customType) throws IOException {
        getWriter().write(OS.NEWLINE);
        getWriter().write(COMMENT_PREFIX + "{");
        getWriter().write(customType.getAsString());
        getWriter().write("}");
        getWriter().write(OS.NEWLINE);
    }

    @Override
    protected void writePrelogue(BibDatabaseContext bibDatabaseContext, Charset encoding) throws IOException {
        if (encoding == null) {
            return;
        }

        // Writes the file encoding information.
        getWriter().write("% ");
        getWriter().write(SavePreferences.ENCODING_PREFIX + encoding);
        getWriter().write(OS.NEWLINE);
    }

    @Override
    protected void writeDatabaseID(String sharedDatabaseID) throws IOException {
        getWriter().write("% " +
                DATABASE_ID_PREFIX +
                " " +
                sharedDatabaseID +
                OS.NEWLINE);
    }

    @Override
    protected void writeEntry(BibEntry entry, BibDatabaseMode mode, Boolean isReformatFile,
                              LatexFieldFormatterPreferences latexFieldFormatterPreferences) throws IOException {
        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(
                new LatexFieldFormatter(latexFieldFormatterPreferences), true);
        bibtexEntryWriter.write(entry, getWriter(), mode, isReformatFile);
    }
}
