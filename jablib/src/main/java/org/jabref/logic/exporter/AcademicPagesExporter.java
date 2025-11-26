package org.jabref.logic.exporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.nio.file.Paths;
import java.util.Map;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.layout.format.Number;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A custom exporter to write multiple bib entries as AcademicPages Markdown format.
 */
public class AcademicPagesExporter extends Exporter {
    private static final String BLANK_LINE_PATTERN = "\\r\\n|\\n";
    private static final String LAYOUT_PREFIX = "/resource/layout/";
    private static final String LAYOUT_EXTENSION = ".layout";
    private static final String FORMATTERS_EXTENSION = ".formatters";
    private static final String BEGIN_INFIX = ".begin";
    private static final String END_INFIX = ".end";

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateExporter.class);

    private final String lfFileName;
    private final String directory;
    private final LayoutFormatterPreferences layoutPreferences;
    private final SelfContainedSaveOrder saveOrder;
    private boolean customExport;
    private List<BibEntry> entries;
    private TemplateExporter academicPagesTemplate;

    /**
     * Initialize another export format based on templates stored in dir with layoutFile lfFilename.
     *
     */
    public AcademicPagesExporter(LayoutFormatterPreferences layoutPreferences, SelfContainedSaveOrder saveOrder) {
        super("academicpages", "academicpages", StandardFileType.MARKDOWN);
        this.lfFileName = "academicpages";
        this.directory = "academicpages";
        this.layoutPreferences = layoutPreferences;
        this.saveOrder = saveOrder;
        String consoleName = "academicpages";
        this.academicPagesTemplate = new TemplateExporter("academicpages", consoleName, lfFileName, directory, StandardFileType.MARKDOWN, layoutPreferences, saveOrder);
    }

    @Override
    public void export(@NonNull final BibDatabaseContext databaseContext,
                       final Path exportDirectory,
                       @NonNull List<BibEntry> entries) throws SaveException {
        export(databaseContext, exportDirectory, entries, List.of(), JournalAbbreviationLoader.loadBuiltInRepository());
    }

    /**
     * The method that performs the export of all entries by iterating on the entries.
     *
     * @param databaseContext the database to export from
     * @param exportDirectory            the directory to write to
     * @param entries         a list containing all entries that should be exported
     * @param abbreviationRepository the built-in repository
     * @throws SaveException   Exception thrown if saving goes wrong
     */
    @Override
    public void export(@NonNull final BibDatabaseContext databaseContext,
                       final Path exportDirectory,
                       @NonNull List<BibEntry> entries,
                       List<Path> fileDirForDataBase,
                       JournalAbbreviationRepository abbreviationRepository) throws SaveException {
        if (entries.isEmpty()) { // Only export if entries exist
            return;
        }
        try {
            Integer iterator = 1;
            for (BibEntry entry : entries) {
                Path path = Paths.get(exportDirectory.toString(), iterator.toString());
                iterator += 1;
                academicPagesTemplate.export(databaseContext, path, entries, fileDirForDataBase, abbreviationRepository);
            }
        } catch (IOException e) {
            return;
        }
    }
}
