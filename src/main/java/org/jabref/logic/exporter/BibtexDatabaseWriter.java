package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.InvalidFieldValueException;
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

    public BibtexDatabaseWriter(BibWriter bibWriter, GeneralPreferences generalPreferences, SavePreferences savePreferences, BibEntryTypesManager entryTypesManager) {
        super(bibWriter, generalPreferences, savePreferences, entryTypesManager);
    }

    public BibtexDatabaseWriter(Writer writer, String newline, GeneralPreferences generalPreferences, SavePreferences savePreferences, BibEntryTypesManager entryTypesManager) {
        super(new BibWriter(writer, newline), generalPreferences, savePreferences, entryTypesManager);
    }

    @Override
    protected void writeEpilogue(String epilogue) throws IOException {
        if (!StringUtil.isNullOrEmpty(epilogue)) {
            bibWriter.write(epilogue);
            bibWriter.finishBlock();
        }
    }

    @Override
    protected void writeMetaDataItem(Map.Entry<String, String> metaItem) throws IOException {
        bibWriter.write(COMMENT_PREFIX + "{");
        bibWriter.write(MetaData.META_FLAG);
        bibWriter.write(metaItem.getKey());
        bibWriter.write(":");
        bibWriter.write(metaItem.getValue());
        bibWriter.write("}");
        bibWriter.finishBlock();
    }

    @Override
    protected void writePreamble(String preamble) throws IOException {
        if (!StringUtil.isNullOrEmpty(preamble)) {
            bibWriter.write(PREAMBLE_PREFIX + "{");
            bibWriter.write(preamble);
            bibWriter.writeLine("}");
            bibWriter.finishBlock();
        }
    }

    @Override
    protected void writeString(BibtexString bibtexString, int maxKeyLength) throws IOException {
        // If the string has not been modified, write it back as it was
        if (!savePreferences.shouldReformatFile() && !bibtexString.hasChanged()) {
            bibWriter.write(bibtexString.getParsedSerialization());
            return;
        }

        // Write user comments
        String userComments = bibtexString.getUserComments();
        if (!userComments.isEmpty()) {
            bibWriter.writeLine(userComments);
        }

        bibWriter.write(STRING_PREFIX + "{" + bibtexString.getName() + StringUtil
                .repeatSpaces(maxKeyLength - bibtexString.getName().length()) + " = ");
        if (bibtexString.getContent().isEmpty()) {
            bibWriter.write("{}");
        } else {
            try {
                String formatted = new FieldWriter(savePreferences.getFieldWriterPreferences())
                        .write(InternalField.BIBTEX_STRING, bibtexString.getContent()
                        );
                bibWriter.write(formatted);
            } catch (InvalidFieldValueException ex) {
                throw new IOException(ex);
            }
        }

        bibWriter.writeLine("}");
    }

    @Override
    protected void writeEntryTypeDefinition(BibEntryType customType) throws IOException {
        bibWriter.write(COMMENT_PREFIX + "{");
        bibWriter.write(BibEntryTypesManager.serialize(customType));
        bibWriter.writeLine("}");
        bibWriter.finishBlock();
    }

    @Override
    protected void writeProlog(BibDatabaseContext bibDatabaseContext, Charset encoding) throws IOException {
        if ((encoding == null) || (encoding == StandardCharsets.UTF_8)) {
            return;
        }

        // Writes the file encoding information.
        bibWriter.write("% ");
        bibWriter.writeLine(SavePreferences.ENCODING_PREFIX + encoding);
    }

    @Override
    protected void writeDatabaseID(String sharedDatabaseID) throws IOException {
        bibWriter.write("% ");
        bibWriter.write(DATABASE_ID_PREFIX);
        bibWriter.write(" ");
        bibWriter.writeLine(sharedDatabaseID);
    }

    @Override
    protected void writeEntry(BibEntry entry, BibDatabaseMode mode) throws IOException {
        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new FieldWriter(savePreferences.getFieldWriterPreferences()), entryTypesManager);
        bibtexEntryWriter.write(entry, bibWriter, mode, savePreferences.shouldReformatFile());
    }
}
