package org.jabref.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.exporter.EmbeddedBibFilePdfExporter;
import org.jabref.logic.exporter.XmpPdfExporter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "pdf", description = "Manage PDF metadata.",
        subcommands = {
                Pdf.PdfUpdate.class
                // RemoveComments.class
                // RemoveEmbedded.class
                // RemovePrivateFields.class
        })
class Pdf implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Pdf.class);

    @ParentCommand
    protected ArgumentProcessor argumentProcessor;

    @Override
    public void run() {
        System.out.println("Specify a subcommand (write-xmp, update).");
    }

    @Command(name = "update", description = "Update linked PDFs with XMP and/or embedded BibTeX.")
    class PdfUpdate implements Callable<Integer> {
        @Option(names = "--format", description = "Format to update (xmp, bibtex-attachment)", split = ",")
        List<String> formats = List.of("xmp", "bibtex-attachment"); // ToDO: default value?

        @Option(names = {"-k", "--citation-key"}, description = "Citation keys", required = true)
        List<String> citationKeys = List.of(); // ToDo: check dedault value

        @Option(names = "--input", description = "Input file", required = true)
        Path inputFile;

        @Option(names = "--input-format", description = "Input format of the file", required = true)
        String inputFormat = "*"; // ToDO: default value?

        @Option(names = "--update-linked-files", description = "Update linked files automatically.")
        boolean updateLinkedFiles;

        @Override
        public Integer call() {
            if (formats.contains("xmp") || formats.contains("bibtex-attachment")) {
                if (inputFile != null) {
                    writeMetadataToPdf(List.of(ArgumentProcessor.importFile(argumentProcessor.cliPreferences, inputFile, inputFormat).get()),
                            List.of(inputFile),
                            citationKeys,
                            argumentProcessor.cliPreferences.getXmpPreferences(),
                            argumentProcessor.cliPreferences.getFilePreferences(),
                            argumentProcessor.cliPreferences.getLibraryPreferences().getDefaultBibDatabaseMode(),
                            argumentProcessor.cliPreferences.getCustomEntryTypesRepository(),
                            argumentProcessor.cliPreferences.getFieldPreferences(),
                            Injector.instantiateModelOrService(JournalAbbreviationRepository.class),
                            formats.contains("xmp"),
                            formats.contains("bibtex-attachment"));
                }
            } else {
                System.out.println("The format option must contain either 'xmp' or 'bibtex-attachment'.");
            }
            return 0;
        }

        private static void writeMetadataToPdf(List<ParserResult> loaded,
                                               List<Path> files,
                                               List<String> citationKeys,
                                               XmpPreferences xmpPreferences,
                                               FilePreferences filePreferences,
                                               BibDatabaseMode databaseMode,
                                               BibEntryTypesManager entryTypesManager,
                                               FieldPreferences fieldPreferences,
                                               JournalAbbreviationRepository abbreviationRepository,
                                               boolean writeXMP,
                                               boolean embeddBibfile) {
            if (loaded.isEmpty()) {
                LOGGER.error("The write xmp option depends on a valid import option.");
                return;
            }
            ParserResult pr = loaded.getLast();
            BibDatabaseContext databaseContext = pr.getDatabaseContext();

            XmpPdfExporter xmpPdfExporter = new XmpPdfExporter(xmpPreferences);
            EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter = new EmbeddedBibFilePdfExporter(databaseMode, entryTypesManager, fieldPreferences);

            if (citationKeys.contains("all")) {
                for (BibEntry entry : databaseContext.getEntries()) {
                    writeMetadataToPDFsOfEntry(
                            databaseContext,
                            entry.getCitationKey().orElse("<no cite key defined>"),
                            entry,
                            filePreferences,
                            xmpPdfExporter,
                            embeddedBibFilePdfExporter,
                            abbreviationRepository,
                            writeXMP,
                            embeddBibfile);
                }
                return;
            }

            writeMetadataToPdfByCitekey(
                    databaseContext,
                    citationKeys,
                    filePreferences,
                    xmpPdfExporter,
                    embeddedBibFilePdfExporter,
                    abbreviationRepository,
                    writeXMP,
                    embeddBibfile);
            writeMetadataToPdfByFileNames(
                    databaseContext,
                    files,
                    filePreferences,
                    xmpPdfExporter,
                    embeddedBibFilePdfExporter,
                    abbreviationRepository,
                    writeXMP,
                    embeddBibfile);
        }

        private static void writeMetadataToPDFsOfEntry(BibDatabaseContext databaseContext,
                                                       String citeKey,
                                                       BibEntry entry,
                                                       FilePreferences filePreferences,
                                                       XmpPdfExporter xmpPdfExporter,
                                                       EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter,
                                                       JournalAbbreviationRepository abbreviationRepository,
                                                       boolean writeXMP,
                                                       boolean embedBibfile) {
            try {
                if (writeXMP) {
                    if (xmpPdfExporter.exportToAllFilesOfEntry(databaseContext, filePreferences, entry, List.of(entry), abbreviationRepository)) {
                        System.out.printf("Successfully written XMP metadata on at least one linked file of %s%n", citeKey);
                    } else {
                        System.err.printf("Cannot write XMP metadata on any linked files of %s. Make sure there is at least one linked file and the path is correct.%n", citeKey);
                    }
                }
                if (embedBibfile) {
                    if (embeddedBibFilePdfExporter.exportToAllFilesOfEntry(databaseContext, filePreferences, entry, List.of(entry), abbreviationRepository)) {
                        System.out.printf("Successfully embedded metadata on at least one linked file of %s%n", citeKey);
                    } else {
                        System.out.printf("Cannot embed metadata on any linked files of %s. Make sure there is at least one linked file and the path is correct.%n", citeKey);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed writing metadata on a linked file of {}.", citeKey);
            }
        }

        private static void writeMetadataToPdfByCitekey(BibDatabaseContext databaseContext,
                                                        List<String> citeKeys,
                                                        FilePreferences filePreferences,
                                                        XmpPdfExporter xmpPdfExporter,
                                                        EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter,
                                                        JournalAbbreviationRepository abbreviationRepository,
                                                        boolean writeXMP,
                                                        boolean embeddBibfile) {
            for (String citeKey : citeKeys) {
                List<BibEntry> bibEntryList = databaseContext.getDatabase().getEntriesByCitationKey(citeKey);
                if (bibEntryList.isEmpty()) {
                    System.err.printf("Skipped - Cannot find %s in library.%n", citeKey);
                    continue;
                }
                for (BibEntry entry : bibEntryList) {
                    writeMetadataToPDFsOfEntry(databaseContext, citeKey, entry, filePreferences, xmpPdfExporter, embeddedBibFilePdfExporter, abbreviationRepository, writeXMP, embeddBibfile);
                }
            }
        }

        private static void writeMetadataToPdfByFileNames(BibDatabaseContext databaseContext,
                                                          List<Path> pdfs,
                                                          FilePreferences filePreferences,
                                                          XmpPdfExporter xmpPdfExporter,
                                                          EmbeddedBibFilePdfExporter embeddedBibFilePdfExporter,
                                                          JournalAbbreviationRepository abbreviationRepository,
                                                          boolean writeXMP,
                                                          boolean embeddBibfile) {
            for (Path filePath : pdfs) {
                if (!filePath.isAbsolute()) { // ToDo: Fix toString
                    filePath = FileUtil.find(filePath.toString(), databaseContext.getFileDirectories(filePreferences)).orElse(
                            FileUtil.find(filePath.toString(), List.of(Path.of("").toAbsolutePath())).orElse(filePath));
                }
                if (Files.exists(filePath)) {
                    try {
                        if (writeXMP) {
                            if (xmpPdfExporter.exportToFileByPath(databaseContext, filePreferences, filePath, abbreviationRepository)) {
                                System.out.printf("Successfully written XMP metadata of at least one entry to %s%n", filePath);
                            } else {
                                System.out.printf("File %s is not linked to any entry in database.%n", filePath);
                            }
                        }
                        if (embeddBibfile) {
                            if (embeddedBibFilePdfExporter.exportToFileByPath(databaseContext, filePreferences, filePath, abbreviationRepository)) {
                                System.out.printf("Successfully embedded XMP metadata of at least one entry to %s%n", filePath);
                            } else {
                                System.out.printf("File %s is not linked to any entry in database.%n", filePath);
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error accessing file '{}'.", filePath);
                    } catch (Exception e) {
                        LOGGER.error("Error writing entry to {}.", filePath);
                    }
                } else {
                    LOGGER.error("Skipped - PDF {} does not exist", filePath);
                }
            }
        }
    }
}
