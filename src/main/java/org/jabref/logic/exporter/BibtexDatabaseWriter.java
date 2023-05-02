package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.strings.StringUtil;

/**
 * Writes a .bib file following the BibTeX / BibLaTeX format using the provided {@link BibWriter}
 */
public class BibtexDatabaseWriter extends BibDatabaseWriter {

    public static final String DATABASE_ID_PREFIX = "DBID:";

    private static final String COMMENT_PREFIX = "@Comment";
    private static final String PREAMBLE_PREFIX = "@Preamble";
    private static final String STRING_PREFIX = "@String";

    private final FieldPreferences fieldPreferences;

    public BibtexDatabaseWriter(BibWriter bibWriter,
                                SaveConfiguration saveConfiguration,
                                FieldPreferences fieldPreferences,
                                CitationKeyPatternPreferences citationKeyPatternPreferences,
                                BibEntryTypesManager entryTypesManager) {
        super(bibWriter,
                saveConfiguration,
                citationKeyPatternPreferences,
                entryTypesManager);

        this.fieldPreferences = fieldPreferences;
    }

    public BibtexDatabaseWriter(Writer writer,
                                String newline,
                                SaveConfiguration saveConfiguration,
                                FieldPreferences fieldPreferences,
                                CitationKeyPatternPreferences citationKeyPatternPreferences,
                                BibEntryTypesManager entryTypesManager) {
        super(new BibWriter(writer, newline),
                saveConfiguration,
                citationKeyPatternPreferences,
                entryTypesManager);

        this.fieldPreferences = fieldPreferences;
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
        if (!saveConfiguration.shouldReformatFile() && !bibtexString.hasChanged()) {
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
                String formatted = new FieldWriter(fieldPreferences)
                        .write(InternalField.BIBTEX_STRING, bibtexString.getContent());
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
        bibWriter.write(MetaDataSerializer.serializeCustomEntryTypes(customType));
        bibWriter.writeLine("}");
        bibWriter.finishBlock();
    }

    @Override
    protected void writeProlog(BibDatabaseContext bibDatabaseContext, Charset encoding) throws IOException {
        // We write the encoding if
        //   - it is provided (!= null)
        //   - explicitly set in the .bib file OR not equal to UTF_8
        // Otherwise, we do not write anything and return
        if ((encoding == null) || (!bibDatabaseContext.getMetaData().getEncodingExplicitlySupplied() && (encoding.equals(StandardCharsets.UTF_8)))) {
            return;
        }

        // Writes the file encoding information.
        bibWriter.write("% ");
        bibWriter.writeLine(SaveConfiguration.ENCODING_PREFIX + encoding);
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
        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new FieldWriter(fieldPreferences), entryTypesManager);
        bibtexEntryWriter.write(entry, bibWriter, mode, saveConfiguration.shouldReformatFile());
    }
}
