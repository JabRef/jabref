package net.sf.jabref.logic.exporter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;

import net.sf.jabref.logic.bibtex.BibEntryWriter;
import net.sf.jabref.logic.bibtex.LatexFieldFormatter;
import net.sf.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.model.metadata.MetaData;
import net.sf.jabref.model.strings.StringUtil;

public class BibtexDatabaseWriter<E extends SaveSession> extends BibDatabaseWriter<E> {

    private static final String STRING_PREFIX = "@String";
    private static final String COMMENT_PREFIX = "@Comment";
    private static final String PREAMBLE_PREFIX = "@Preamble";

    public static final String DATABASE_ID_PREFIX = "DBID:";

    public BibtexDatabaseWriter(SaveSessionFactory<E> saveSessionFactory) {
        super(saveSessionFactory);
    }

    @Override
    protected void writeEpilogue(String epilogue) throws SaveException {
        if (!StringUtil.isNullOrEmpty(epilogue)) {
            try {
                getWriter().write(OS.NEWLINE);
                getWriter().write(epilogue);
                getWriter().write(OS.NEWLINE);
            } catch (IOException e) {
                throw new SaveException(e);
            }
        }
    }

    @Override
    protected void writeMetaDataItem(Map.Entry<String, String> metaItem) throws SaveException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(OS.NEWLINE);
        stringBuilder.append(COMMENT_PREFIX + "{").append(MetaData.META_FLAG).append(metaItem.getKey()).append(":");
        stringBuilder.append(metaItem.getValue());
        stringBuilder.append("}");
        stringBuilder.append(OS.NEWLINE);

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
                getWriter().write(OS.NEWLINE);
                getWriter().write(PREAMBLE_PREFIX + "{");
                getWriter().write(preamble);
                getWriter().write('}' + OS.NEWLINE);
            } catch (IOException e) {
                throw new SaveException(e);
            }
        }
    }

    @Override
    protected void writeString(BibtexString bibtexString, boolean isFirstString, int maxKeyLength, Boolean reformatFile,
            LatexFieldFormatterPreferences latexFieldFormatterPreferences) throws SaveException {
        try {
            // If the string has not been modified, write it back as it was
            if (!reformatFile && !bibtexString.hasChanged()) {
                getWriter().write(bibtexString.getParsedSerialization());
                return;
            }

            // Write user comments
            String userComments = bibtexString.getUserComments();
            if(!userComments.isEmpty()) {
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
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException(
                            "The # character is not allowed in BibTeX strings unless escaped as in '\\#'.\n" + "Before saving, please edit any strings containing the # character.");
                }
            }

            getWriter().write("}" + OS.NEWLINE);
        } catch (IOException e) {
            throw new SaveException(e);
        }
    }

    @Override
    protected void writeEntryTypeDefinition(CustomEntryType customType) throws SaveException {
        try {
            getWriter().write(OS.NEWLINE);
            getWriter().write(COMMENT_PREFIX + "{");
            getWriter().write(customType.getAsString());
            getWriter().write("}");
            getWriter().write(OS.NEWLINE);
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
            getWriter().write(SavePreferences.ENCODING_PREFIX + encoding);
            getWriter().write(OS.NEWLINE);

        } catch (IOException e) {
            throw new SaveException(e);
        }
    }

    @Override
    protected void writeDatabaseID(String sharedDatabaseID) throws SaveException {
        try {
            StringBuilder stringBuilder = new StringBuilder()
                    .append("% ")
                    .append(DATABASE_ID_PREFIX)
                    .append(" ")
                    .append(sharedDatabaseID)
                    .append(OS.NEWLINE);
            getWriter().write(stringBuilder.toString());
        } catch (IOException e) {
            throw new SaveException(e);
        }
    }

    @Override
    protected void writeEntry(BibEntry entry, BibDatabaseMode mode, Boolean isReformatFile,
            LatexFieldFormatterPreferences latexFieldFormatterPreferences) throws SaveException {
        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(
                new LatexFieldFormatter(latexFieldFormatterPreferences), true);
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
