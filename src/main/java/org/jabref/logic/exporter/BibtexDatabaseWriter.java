package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;

import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.GeneralPreferences;

public class BibtexDatabaseWriter extends BibDatabaseWriter {

    public static final String DATABASE_ID_PREFIX = "DBID:";
    private static final String STRING_PREFIX = "@String";
    private static final String COMMENT_PREFIX = "@Comment";
    private static final String PREAMBLE_PREFIX = "@Preamble";

    public BibtexDatabaseWriter(Writer writer, GeneralPreferences generalPreferences, SavePreferences savePreferences, BibEntryTypesManager entryTypesManager) {
        super(writer, generalPreferences, savePreferences, entryTypesManager);
    }

    @Override
    protected void writeEpilogue(String epilogue) throws IOException {
        if (!StringUtil.isNullOrEmpty(epilogue)) {
            writer.write(OS.NEWLINE);
            writer.write(epilogue);
            writer.write(OS.NEWLINE);
        }
    }

    @Override
    protected void writeMetaDataItem(Map.Entry<String, String> metaItem) throws IOException {
        writer.write(OS.NEWLINE);
        writer.write(COMMENT_PREFIX + "{");
        writer.write(MetaData.META_FLAG);
        writer.write(metaItem.getKey());
        writer.write(":");
        writer.write(metaItem.getValue());
        writer.write("}");
        writer.write(OS.NEWLINE);
    }

    @Override
    protected void writePreamble(String preamble) throws IOException {
        if (!StringUtil.isNullOrEmpty(preamble)) {
            writer.write(OS.NEWLINE);
            writer.write(PREAMBLE_PREFIX + "{");
            writer.write(preamble);
            writer.write('}' + OS.NEWLINE);
        }
    }

    @Override
    protected void writeString(BibtexString bibtexString, boolean isFirstString, int maxKeyLength) throws IOException {
        // If the string has not been modified, write it back as it was
        if (!savePreferences.shouldReformatFile() && !bibtexString.hasChanged()) {
            writer.write(bibtexString.getParsedSerialization());
            return;
        }

        // Write user comments
        String userComments = bibtexString.getUserComments();
        if (!userComments.isEmpty()) {
            writer.write(userComments + OS.NEWLINE);
        }

        if (isFirstString) {
            writer.write(OS.NEWLINE);
        }

        writer.write(STRING_PREFIX + "{" + bibtexString.getName() + StringUtil
                .repeatSpaces(maxKeyLength - bibtexString.getName().length()) + " = ");
        if (bibtexString.getContent().isEmpty()) {
            writer.write("{}");
        } else {
            try {
                String formatted = new FieldWriter(savePreferences.getFieldWriterPreferences())
                        .write(InternalField.BIBTEX_STRING, bibtexString.getContent()
                        );
                writer.write(formatted);
            } catch (InvalidFieldValueException ex) {
                throw new IOException(ex);
            }
        }

        writer.write("}" + OS.NEWLINE);
    }

    @Override
    protected void writeEntryTypeDefinition(BibEntryType customType) throws IOException {
        writer.write(OS.NEWLINE);
        writer.write(COMMENT_PREFIX + "{");
        writer.write(BibEntryTypesManager.serialize(customType));
        writer.write("}");
        writer.write(OS.NEWLINE);
    }

    @Override
    protected void writePrelogue(BibDatabaseContext bibDatabaseContext, Charset encoding) throws IOException {
        if (encoding == null) {
            return;
        }

        // Writes the file encoding information.
        writer.write("% ");
        writer.write(SavePreferences.ENCODING_PREFIX + encoding);
        writer.write(OS.NEWLINE);
    }

    @Override
    protected void writeDatabaseID(String sharedDatabaseID) throws IOException {
        writer.write("% " +
                DATABASE_ID_PREFIX +
                " " +
                sharedDatabaseID +
                OS.NEWLINE);
    }

    @Override
    protected void writeEntry(BibEntry entry, BibDatabaseMode mode) throws IOException {
        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new FieldWriter(savePreferences.getFieldWriterPreferences()), entryTypesManager);
        bibtexEntryWriter.write(entry, writer, mode, savePreferences.shouldReformatFile());
    }
}
