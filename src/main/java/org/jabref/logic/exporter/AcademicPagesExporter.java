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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.FileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcademicPagesExporter extends Exporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcademicPagesExporter.class);
    private static final String LAYOUT_PREFIX = "/resource/layout/";
    private static final String LAYOUT_EXTENSION = ".layout";
    private static final String BLANK_LINE_PATTERN = "\\r\\n|\\n";
    private BlankLineBehaviour blankLineBehaviour;


    private final SelfContainedSaveOrder saveOrder;
    private final String directory;
    private final String lfFileName;
    private final LayoutFormatterPreferences layoutPreferences;
    private boolean customExport;

    /**
     * Initialize another export format based on templates stored in dir with layoutFile lfFilename.
     *
     * @param displayName       Name to display to the user.
     * @param lfFileName        Name of the main layout file.
     * @param extension         Should contain the . (for instance .txt).
     * @param layoutPreferences Preferences for layout
     * @param blankLineBehaviour how to behave regarding blank lines.
     */
    public AcademicPagesExporter(String displayName,
                                 String consoleName,
                                 String lfFileName,
                                 FileType extension,
                                 LayoutFormatterPreferences layoutPreferences,
                                 SelfContainedSaveOrder saveOrder,
                                 BlankLineBehaviour blankLineBehaviour,
                                 String directory) {
        super(consoleName, displayName, extension);
        this.saveOrder = saveOrder == null ? SaveOrder.getDefaultSaveOrder() : saveOrder;
        this.directory = directory;
        this.lfFileName = lfFileName;
        this.layoutPreferences = layoutPreferences;
        this.blankLineBehaviour = blankLineBehaviour;
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries) throws Exception {
        export(databaseContext, file, entries, Collections.emptyList(), JournalAbbreviationLoader.loadBuiltInRepository());
    }

    @Override
    public void export(final BibDatabaseContext databaseContext,
                       final Path file,
                       List<BibEntry> entries,
                       List<Path> fileDirForDatabase,
                       JournalAbbreviationRepository abbreviationRepository) throws Exception {
        String userProvidedName = file.getFileName().toString();
        Path parentDirectory = file.getParent();

        // Extract the base name and extension of the user-provided file name
        String baseName = userProvidedName.substring(0, userProvidedName.lastIndexOf('.')); // e.g., "test"
        String extension = userProvidedName.substring(userProvidedName.lastIndexOf('.')); // e.g., ".md"

        Charset encodingToUse = StandardCharsets.UTF_8;

        for (int i = 0; i < entries.size(); i++) {
            String modifiedFileName = baseName + "_" + (i + 1) + extension;
            // Resolve the modified file path
            Path modifiedFilePath = parentDirectory.resolve(modifiedFileName);

            try (AtomicFileWriter ps = new AtomicFileWriter(modifiedFilePath, encodingToUse)) {
                System.out.println("Writing file: " + modifiedFileName);

                /*
                 * Write database entries; entries will be sorted as they appear on the
                 * screen, or sorted by author, depending on Preferences.
                 */
                List<BibEntry> sorted = BibDatabaseWriter.getSortedEntries(entries, saveOrder);

                // Load default layout
                Layout defLayout;
                LayoutHelper layoutHelper;
                try (Reader reader = getReader(lfFileName + LAYOUT_EXTENSION)) {
                    layoutHelper = new LayoutHelper(reader, fileDirForDatabase, layoutPreferences, abbreviationRepository);
                    defLayout = layoutHelper.getLayoutFromText();
                }

                Map<EntryType, Layout> layouts = new HashMap<>();
                Layout layout;

                // Get the layout
                EntryType type = sorted.get(i).getType();
                if (layouts.containsKey(type)) {
                    layout = layouts.get(type);
                } else {
                    try (Reader reader = getReader(lfFileName + '.' + type.getName() + LAYOUT_EXTENSION)) {
                        // We try to get a type-specific layout for this entry.
                        layoutHelper = new LayoutHelper(reader, fileDirForDatabase, layoutPreferences, abbreviationRepository);
                        layout = layoutHelper.getLayoutFromText();
                        layouts.put(type, layout);
                    } catch (IOException ex) {
                        // The exception indicates that no type-specific layout
                        // exists, so we
                        // go with the default one.
                        layout = defLayout;
                    }
                }
                // Write the entry
                if (layout != null) {
                    if (blankLineBehaviour == BlankLineBehaviour.DELETE_BLANKS) {
                        String[] lines = layout.doLayout(sorted.get(i), databaseContext.getDatabase()).split(BLANK_LINE_PATTERN);
                        for (String line : lines) {
                            if (!line.isBlank() && !line.isEmpty()) {
                                ps.write(line + OS.NEWLINE);
                            }
                        }
                    } else {
                        ps.write(layout.doLayout(sorted.get(i), databaseContext.getDatabase()));
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error writing file: " + modifiedFileName, e);
            }
        }
    }

    /**
     * This method should return a reader from which the given layout file can be read.
     * <p>
     * Subclasses of TemplateExporter are free to override and provide their own implementation.
     *
     * @param filename the filename
     * @return a newly created reader
     * @throws IOException if the reader could not be created (e.g., file is not found)
     */
    private Reader getReader(String filename) throws IOException {
        // If this is a custom export, just use the given filename:
        String dir;
        if (customExport) {
            dir = "";
        } else {
            dir = LAYOUT_PREFIX + (directory == null ? "" : directory + '/');
        }

        // Attempt to get a Reader for the file path given, either by
        // loading it as a resource (from within JAR), or as a normal file. If
        // unsuccessful (e.g. file not found), an IOException is thrown.

        String name = dir + filename;

        Path path = Path.of(name);
        if (Files.exists(path)) {
            return Files.newBufferedReader(path, StandardCharsets.UTF_8);
        }

        InputStream inputStream = TemplateExporter.class.getResourceAsStream(name);
        if (inputStream == null) {
            throw new IOException("Cannot find layout file: '" + name + "'.");
        }

        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }
}
