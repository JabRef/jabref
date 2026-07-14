package org.jabref.logic.directorylibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.HayagrivaImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.io.GitIgnoreFileFilter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Builds an in-memory library from a directory tree: each Hayagriva `.yml`/`.yaml` file
/// contributes its entries, a PDF next to a sidecar of the same base name is linked to the
/// sidecar's (first) entry, and PDFs without a sidecar become entries with metadata extracted
/// from the PDF itself (see [PdfEntryFactory]). The directory itself is the library — the
/// resulting [BibDatabaseContext] has [org.jabref.logic.shared.DatabaseLocation#DIRECTORY] and
/// no database path; linked files are stored relative to the root, which is registered as the
/// library-specific file directory.
// [impl->req~directory-library.scan~2]
@NullMarked
public class DirectoryLibraryScanner {

    /// Everything a directory scan produces: the ready-to-open context, the entry-to-file
    /// catalog (consumed by the file synchronization in later steps), and user-facing warnings
    /// about files that looked like Hayagriva but could not be parsed.
    public record ScanResult(BibDatabaseContext databaseContext, DirectoryLibraryCatalog catalog, List<String> warnings) {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryLibraryScanner.class);

    private static final Set<String> YAML_EXTENSIONS = Set.of("yml", "yaml");
    private static final String PDF_EXTENSION = "pdf";

    private final HayagrivaImporter importer = new HayagrivaImporter();
    private final PdfEntryFactory pdfEntryFactory;

    public DirectoryLibraryScanner(PdfEntryFactory pdfEntryFactory) {
        this.pdfEntryFactory = pdfEntryFactory;
    }

    public ScanResult scan(Path root) throws IOException {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.convertToDirectoryLibrary(root);
        // Makes the relative PDF links resolvable although the context has no database path
        databaseContext.getMetaData().setLibrarySpecificFileDirectory(root.toAbsolutePath().toString());

        DirectoryLibraryCatalog catalog = new DirectoryLibraryCatalog();
        List<String> warnings = new ArrayList<>();

        List<Path> yamlFiles = new ArrayList<>();
        List<Path> pdfFiles = new ArrayList<>();
        collectFiles(root, yamlFiles, pdfFiles);

        List<BibEntry> entries = new ArrayList<>();
        Set<Path> pairedPdfs = new HashSet<>();
        for (Path yamlFile : yamlFiles) {
            // A directory may contain arbitrary YAML (CI configs, ...) — only files recognized
            // as Hayagriva become entries; others are silently ignored
            if (!looksLikeHayagriva(yamlFile)) {
                continue;
            }
            ParserResult parserResult = importer.importDatabase(yamlFile);
            List<BibEntry> fileEntries = parserResult.getDatabase().getEntries();
            if (parserResult.isInvalid() || fileEntries.isEmpty()) {
                warnings.add(Localization.lang("Could not parse the Hayagriva file '%0'.", yamlFile.toString()));
                continue;
            }
            fileEntries.forEach(entry -> catalog.register(entry, yamlFile, entry.getCitationKey().orElse("")));
            findPairedPdf(yamlFile).ifPresent(pdf -> {
                pairedPdfs.add(pdf);
                linkPdf(fileEntries.getFirst(), root, pdf);
            });
            entries.addAll(fileEntries);
        }

        List<BibEntry> pdfEntries = new ArrayList<>();
        for (Path pdf : pdfFiles) {
            if (pairedPdfs.contains(pdf)) {
                continue;
            }
            // Metadata comes from the PDF itself; a sidecar is still only written once the
            // user edits the entry
            pdfEntries.add(pdfEntryFactory.createEntry(pdf, root, databaseContext));
        }
        entries.addAll(pdfEntries);

        databaseContext.getDatabase().insertEntries(entries);
        // After insertion, so the uniqueness check sees all scanned entries
        pdfEntries.forEach(entry -> pdfEntryFactory.generateCitationKeyIfMissing(entry, databaseContext));
        return new ScanResult(databaseContext, catalog, warnings);
    }

    private void collectFiles(Path root, List<Path> yamlFiles, List<Path> pdfFiles) throws IOException {
        GitIgnoreFileFilter gitIgnoreFilter = new GitIgnoreFileFilter(root);
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                if (!directory.equals(root) && (isHidden(directory) || !gitIgnoreFilter.accept(directory))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                if (isHidden(file) || !gitIgnoreFilter.accept(file)) {
                    return FileVisitResult.CONTINUE;
                }
                String extension = FileUtil.getFileExtension(file).orElse("");
                if (YAML_EXTENSIONS.contains(extension)) {
                    yamlFiles.add(file);
                } else if (PDF_EXTENSION.equals(extension)) {
                    pdfFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exception) {
                LOGGER.debug("Skipping unreadable path {}", file, exception);
                return FileVisitResult.CONTINUE;
            }
        });
        yamlFiles.sort(Path::compareTo);
        pdfFiles.sort(Path::compareTo);
    }

    private static boolean isHidden(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().startsWith(".");
    }

    /// Uses the lookahead-based recognition (a `type:` line naming a Hayagriva entry type)
    /// instead of [org.jabref.logic.importer.Importer#isRecognizedFormat(Path)], which fully
    /// parses the YAML: a syntactically broken sidecar must surface as a warning, not be
    /// silently skipped as "not Hayagriva".
    private boolean looksLikeHayagriva(Path yamlFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(yamlFile, StandardCharsets.UTF_8)) {
            return importer.isRecognizedFormat(reader);
        }
    }

    /// The sidecar convention: `X.yml` next to `X.pdf` (same directory, same base name).
    private Optional<Path> findPairedPdf(Path yamlFile) {
        Path parent = yamlFile.getParent();
        if (parent == null) {
            return Optional.empty();
        }
        String baseName = FileUtil.getBaseName(yamlFile);
        for (String candidate : List.of(baseName + ".pdf", baseName + ".PDF")) {
            Path pdf = parent.resolve(candidate);
            if (Files.exists(pdf)) {
                return Optional.of(pdf);
            }
        }
        return Optional.empty();
    }

    private void linkPdf(BibEntry entry, Path root, Path pdf) {
        if (entry.getFiles().isEmpty()) {
            entry.addFile(new LinkedFile("", root.relativize(pdf), StandardFileType.PDF.getName()));
        }
    }
}
