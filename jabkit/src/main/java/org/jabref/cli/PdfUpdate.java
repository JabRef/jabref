package org.jabref.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jabref.cli.converter.CygWinPathConverter;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.exporter.EmbeddedBibFilePdfExporter;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.XmpPdfExporter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
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
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "update", description = "Update linked PDFs with XMP and/or embedded BibTeX.")
class PdfUpdate implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfUpdate.class);

    @ParentCommand
    protected Pdf pdf;

    @Mixin
    private JabKitArgumentProcessor.SharedOptions sharedOptions = new JabKitArgumentProcessor.SharedOptions();

    @Option(names = "--format", description = "Format to update (xmp, bibtex-attachment)", split = ",")
    private List<String> formats = List.of("xmp", "bibtex-attachment"); // ToDO: default value?

    @Option(names = {"-k", "--citation-key"}, description = "Citation keys", required = true)
    private List<String> citationKeys = List.of(); // ToDo: check dedault value

    // [impl->req~jabkit.cli.input-flag~1]
    @Option(names = {"--input"}, converter = CygWinPathConverter.class, description = "Input BibTeX file", required = true)
    private Path inputFile;

    @Option(names = "--input-format", description = "Input format of the file", required = true)
    private String inputFormat = "*";

    @Option(names = "--update-linked-files", description = "Update linked files automatically.")
    private boolean updateLinkedFiles;

    @Override
    public void run() {
        if (!formats.contains("xmp") && !formats.contains("bibtex-attachment")) {
            System.out.println("The format option must contain either 'xmp' or 'bibtex-attachment'.");
            return;
        }

        Optional<ParserResult> parserResult = JabKitArgumentProcessor.importFile(
                inputFile,
                inputFormat,
                pdf.argumentProcessor.cliPreferences,
                sharedOptions.porcelain);
        if (parserResult.isEmpty()) {
            System.out.println(Localization.lang("Unable to open file '%0'.", inputFile));
            return;
        }

        if (parserResult.get().isInvalid()) {
            System.out.println(Localization.lang("Input file '%0' is invalid and could not be parsed.", inputFile));
            return;
        }

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Updating PDF metadata."));
            System.out.flush();
        }

        writeMetadataToPdf(List.of(parserResult.get()),
                List.of(inputFile),
                citationKeys,
                pdf.argumentProcessor.cliPreferences.getXmpPreferences(),
                pdf.argumentProcessor.cliPreferences.getFilePreferences(),
                pdf.argumentProcessor.cliPreferences.getLibraryPreferences().getDefaultBibDatabaseMode(),
                pdf.argumentProcessor.cliPreferences.getCustomEntryTypesRepository(),
                pdf.argumentProcessor.cliPreferences.getFieldPreferences(),
                Injector.instantiateModelOrService(JournalAbbreviationRepository.class),
                formats.contains("xmp"),
                formats.contains("bibtex-attachment"));
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
                if (xmpPdfExporter.exportToAllFilesOfEntry(
                        databaseContext,
                        filePreferences,
                        entry,
                        List.of(entry),
                        abbreviationRepository)) {
                    System.out.println(Localization.lang("Successfully written XMP metadata on at least one linked file of %0.", citeKey));
                } else {
                    System.out.println(Localization.lang("Cannot write XMP metadata on any linked files of %0. Make sure there is at least one linked file and the path is correct.", citeKey));
                }
            }
            if (embedBibfile) {
                if (embeddedBibFilePdfExporter.exportToAllFilesOfEntry(
                        databaseContext,
                        filePreferences,
                        entry,
                        List.of(entry),
                        abbreviationRepository)) {
                    System.out.println(Localization.lang("Successfully embedded metadata on at least one linked file of %0.", citeKey));
                } else {
                    System.out.println(Localization.lang("Cannot embed metadata on any linked files of %s. Make sure there is at least one linked file and the path is correct.", citeKey));
                }
            }
        } catch (IOException
                 | ParserConfigurationException
                 | SaveException
                 | TransformerException e) {
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
                LOGGER.error("Skipped - Cannot find {} in library.", citeKey);
                continue;
            }
            for (BibEntry entry : bibEntryList) {
                writeMetadataToPDFsOfEntry(
                        databaseContext,
                        citeKey,
                        entry,
                        filePreferences,
                        xmpPdfExporter,
                        embeddedBibFilePdfExporter,
                        abbreviationRepository,
                        writeXMP,
                        embeddBibfile);
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
            if (!filePath.isAbsolute()) {
                filePath = FileUtil.find(filePath.toString(), databaseContext.getFileDirectories(filePreferences)).orElse(
                        FileUtil.find(filePath.toString(), List.of(Path.of("").toAbsolutePath())).orElse(filePath));
            }

            if (!Files.exists(filePath)) {
                LOGGER.error("Skipped - PDF {} does not exist", filePath);
                return;
            }

            try {
                if (writeXMP) {
                    if (xmpPdfExporter.exportToFileByPath(databaseContext, filePreferences, filePath, abbreviationRepository)) {
                        System.out.println(Localization.lang("Successfully written XMP metadata of at least one entry to %0.", filePath));
                    } else {
                        System.out.println(Localization.lang("File %0 is not linked to any entry in library.", filePath));
                    }
                }
                if (embeddBibfile) {
                    if (embeddedBibFilePdfExporter.exportToFileByPath(databaseContext, filePreferences, filePath, abbreviationRepository)) {
                        System.out.println(Localization.lang("Successfully embedded XMP metadata of at least one entry to %0.", filePath));
                    } else {
                        System.out.println(Localization.lang("File %0 is not linked to any entry in library.", filePath));
                    }
                }
            } catch (IOException
                     | ParserConfigurationException
                     | SaveException
                     | TransformerException e) {
                LOGGER.error("Error writing entry to {}.", filePath);
            }
        }
    }
}
