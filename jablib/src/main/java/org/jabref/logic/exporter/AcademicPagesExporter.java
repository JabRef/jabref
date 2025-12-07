package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.HTMLChars;
import org.jabref.logic.layout.format.RemoveLatexCommandsFormatter;
import org.jabref.logic.layout.format.Replace;
import org.jabref.logic.layout.format.SafeFileName;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.SelfContainedSaveOrder;

import org.jspecify.annotations.NonNull;

/**
 * A custom exporter to write multiple bib entries as AcademicPages Markdown format.
 */
public class AcademicPagesExporter extends Exporter {
    private final String layoutFileFileName;
    private final String directory;
    private final LayoutFormatterPreferences layoutPreferences;
    private final SelfContainedSaveOrder saveOrder;

    private final Replace replaceFormatter = new Replace();
    private final RemoveLatexCommandsFormatter commandsFormatter = new RemoveLatexCommandsFormatter();
    private final HTMLChars htmlFormatter = new HTMLChars();
    private final SafeFileName safeFormatter = new SafeFileName();

    private TemplateExporter academicPagesTemplate;

    /**
     * Initialize another export format based on templates stored in directory.
     *
     */
    public AcademicPagesExporter(LayoutFormatterPreferences layoutPreferences, SelfContainedSaveOrder saveOrder) {
        super("academicpages", "academic pages markdowns", StandardFileType.MARKDOWN);
        this.layoutFileFileName = "academicpages";
        this.directory = "academicpages";
        this.layoutPreferences = layoutPreferences;
        this.saveOrder = saveOrder;
        String consoleName = "academicpages";
        this.academicPagesTemplate = new TemplateExporter("academicpages", consoleName, layoutFileFileName, directory, StandardFileType.MARKDOWN, layoutPreferences, saveOrder);
    }

    @Override
    public void export(@NonNull final BibDatabaseContext databaseContext,
                       @NonNull final Path exportDirectory,
                       @NonNull List<BibEntry> entries) throws SaveException {
        export(databaseContext, exportDirectory, entries, List.of(exportDirectory), JournalAbbreviationLoader.loadBuiltInRepository());
    }

    /**
     * The method that performs the export of all entries by iterating on the entries.
     *
     * @param databaseContext the database to export from
     * @param file            the directory to write to
     * @param entries         a list containing all entries that should be exported
     * @param abbreviationRepository the built-in repository
     * @throws SaveException   Exception thrown if saving goes wrong
     */
    @Override
    public void export(@NonNull final BibDatabaseContext databaseContext,
                       @NonNull final Path file,
                       @NonNull List<BibEntry> entries,
                       List<Path> fileDirForDataBase,
                       JournalAbbreviationRepository abbreviationRepository) throws SaveException {
        if (entries.isEmpty()) { // Only export if entries exist
            return;
        }
        try {
            // convert what the ExportCommand gives as a file parameter to a directory
            Path baseDir = file;
            String exportDirectoryString = FileUtil.getBaseName(file);
            Path exportDirectory = baseDir.getParent().resolve(exportDirectoryString);

            // Ensure the directory exists. This is important: AtomicFileWriter will fail if parent dirs are missing.
            Files.createDirectories(exportDirectory);

            for (BibEntry entry : entries) {
                // formatting the title of each entry to match the file names format demanded by academic pages (applying the same formatters applied to the title in the academicpages.layout)
                Path path = getPath(entry, exportDirectory);

                List<BibEntry> individualEntry = new ArrayList<>();
                individualEntry.add(entry);
                academicPagesTemplate.export(databaseContext, path, individualEntry, fileDirForDataBase, abbreviationRepository);
            }
        } catch (IOException e) {
            throw new SaveException("could not export", e);
        }
    }

    private @NonNull Path getPath(BibEntry entry, Path exportDirectory) throws SaveException {
        replaceFormatter.setArgument(" ,-"); // expects an expression that has the character to remove and the replacement character separated by a comma.
        String title = entry.getTitle().orElseThrow(() -> new SaveException("Entry is missing a title"));
        String formattedTitle = commandsFormatter.format(htmlFormatter.format(replaceFormatter.format(title)));
        String safeTitle = safeFormatter.format(formattedTitle);
        return exportDirectory.resolve(safeTitle + ".md");
    }
}
