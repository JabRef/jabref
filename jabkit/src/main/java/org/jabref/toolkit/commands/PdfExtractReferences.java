package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.CitationsFromPdf;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.toolkit.converter.ExtractionModeConverter;
import org.jabref.toolkit.exception.ExportServiceException;
import org.jabref.toolkit.exception.ImportServiceException;
import org.jabref.toolkit.service.ExportService;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Parameters;
import static picocli.CommandLine.ParentCommand;

@NullMarked
@Command(name = "extract-references", description = "Extract references from the \"References\" section of one or more PDFs and output them as BibTeX.")
class PdfExtractReferences implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfExtractReferences.class);

    @ParentCommand
    protected Pdf pdf;

    protected ExportService exportService;

    @Mixin
    private JabKit.SharedOptions sharedOptions;

    // [impl->req~jabkit.cli.input-url~2]
    @Parameters(paramLabel = "FILE", description = "PDF(s) to extract references from, each a file or an http(s)/ftp URL.", arity = "1..*")
    private List<String> inputFiles;

    @Option(names = "--mode", converter = ExtractionModeConverter.class,
            description = "Extraction mode: ${COMPLETION-CANDIDATES} (LLM is experimental). Defaults to GROBID if Grobid is enabled in preferences, otherwise RULE_BASED.")
    private @Nullable ExtractionMode mode;

    @Option(names = "--grobid-url", description = "Override the configured Grobid server URL for this call (only valid with --mode=GROBID).")
    private @Nullable String grobidUrl;

    @Option(names = "--output", description = "Write output to this file. Only valid when a single input file is given (e.g. --output=out.bib).")
    private @Nullable Path outputFile;

    @Option(names = "--output-dir", description = "Directory to write one .bib file per input PDF into (default: alongside each source PDF).")
    private @Nullable Path outputDir;

    @Option(names = "--output-format", description = "Output format (e.g. bibtex)", defaultValue = "bibtex")
    private String outputFormat;

    void initFields() {
        exportService = ExportService.create(pdf.argumentProcessor.cliPreferences, sharedOptions.porcelain);
    }

    @Override
    public Integer call() throws ImportServiceException, ExportServiceException {
        initFields();

        if (outputFile != null && inputFiles.size() > 1) {
            System.err.println(Localization.lang("--output can only be used with a single input file; use --output-dir for multiple files."));
            return CommandLine.ExitCode.USAGE;
        }

        CliPreferences preferences = pdf.argumentProcessor.cliPreferences;
        ExtractionMode effectiveMode = mode != null
                                       ? mode
                                       : (preferences.getGrobidPreferences().isGrobidEnabled() ? ExtractionMode.GROBID : ExtractionMode.RULE_BASED);

        if (grobidUrl != null && effectiveMode != ExtractionMode.GROBID) {
            System.err.println(Localization.lang("--grobid-url can only be used with --mode=GROBID."));
            return CommandLine.ExitCode.USAGE;
        }

        if (outputDir != null) {
            Integer errorCode = ensureDirectoryExists(outputDir);
            if (errorCode != null) {
                return errorCode;
            }
        }
        if (outputFile != null) {
            Path outputFileParent = outputFile.toAbsolutePath().getParent();
            if (outputFileParent != null) {
                Integer errorCode = ensureDirectoryExists(outputFileParent);
                if (errorCode != null) {
                    return errorCode;
                }
            }
        }

        List<String> failed = new ArrayList<>();
        for (String input : inputFiles) {
            Path inputFile;
            try {
                inputFile = InputOption.resolveInput(input);
            } catch (ImportServiceException e) {
                // One unreachable URL or malformed path must not abort the remaining inputs.
                LOGGER.error("Skipped - could not resolve input {}", displayName(input), e);
                failed.add(input);
                continue;
            }

            if (!Files.exists(inputFile)) {
                LOGGER.error("Skipped - PDF {} does not exist", inputFile);
                failed.add(input);
                continue;
            }

            if (!sharedOptions.porcelain) {
                System.out.println(Localization.lang("Extracting references from %0 (mode: %1).", displayName(input), effectiveMode));
            }

            ParserResult result = switch (effectiveMode) {
                case RULE_BASED -> CitationsFromPdf.extractCitationsUsingRuleBasedAlgorithm(preferences, inputFile);
                // Force Grobid enabled for this single call: the user explicitly asked for --mode=GROBID,
                // but the stored preference may have Grobid disabled, which would otherwise make
                // GrobidService throw instead of running the extraction.
                case GROBID -> CitationsFromPdf.extractCitationsUsingGrobid(
                        preferences,
                        inputFile,
                        grobidUrl != null ? grobidUrl : preferences.getGrobidPreferences().getGrobidURL());
                case LLM -> CitationsFromPdf.extractCitationsUsingLLM(preferences, LOGGER::info, inputFile);
            };

            if (result.isInvalid()) {
                LOGGER.error("Could not extract references from '{}': {}", displayName(input), result.getErrorMessage());
                failed.add(input);
                continue;
            }

            if (outputFile != null) {
                exportService.exportParserResultToFile(result, outputFile, outputFormat);
            } else if (outputDir != null) {
                exportService.exportParserResultToFile(result, outputDir.resolve(bibFileName(inputFile)), outputFormat);
            } else if (inputFiles.size() == 1) {
                exportService.printDatabaseContextToStdOut(result.getDatabaseContext());
            } else {
                exportService.exportParserResultToFile(result, defaultBibFile(input, inputFile), outputFormat);
            }
        }

        return failed.isEmpty() ? CommandLine.ExitCode.OK : CommandLine.ExitCode.SOFTWARE;
    }

    /// Creates `directory` (and any missing parents) if it doesn't exist yet.
    ///
    /// @return a non-null exit code on failure, or `null` on success
    private static @Nullable Integer ensureDirectoryExists(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            System.err.println(Localization.lang("Could not create output directory '%0': %1", directory, e.getLocalizedMessage()));
            return CommandLine.ExitCode.SOFTWARE;
        }
        return null;
    }

    /// The input as shown to the user: URLs are redacted so that embedded credentials do not end up
    /// on the console or in the log.
    private static String displayName(String input) {
        return InputOption.isUrl(input) ? FetcherException.getRedactedUrl(input) : input;
    }

    /// Target for the "one `.bib` next to each source PDF" default. A downloaded URL has no local
    /// source directory to sit next to, so its output goes into the working directory instead of
    /// silently into the temporary directory holding the download.
    private static Path defaultBibFile(String input, Path resolvedFile) {
        return InputOption.isUrl(input)
               ? Path.of(bibFileName(resolvedFile))
               : resolvedFile.toAbsolutePath().getParent().resolve(bibFileName(resolvedFile));
    }

    private static String bibFileName(Path inputFile) {
        String fileName = inputFile.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        return baseName + ".bib";
    }
}
