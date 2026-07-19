package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import org.jabref.logic.cleanup.AbbreviateJournalCleanup;
import org.jabref.logic.cleanup.DoiCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupMapper;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.texparser.DefaultLatexParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.LatexParserResult;
import org.jabref.toolkit.exception.CliException;
import org.jabref.toolkit.exception.ExportServiceException;
import org.jabref.toolkit.exception.ImportServiceException;
import org.jabref.toolkit.service.ExportService;
import org.jabref.toolkit.service.ImportService;

import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

/// Shortens the reference list cited by a LaTeX document until the compiled paper reaches a target
/// page count. Applies increasingly aggressive, information-reducing cleanups to the cited entries —
/// first author minification, then journal abbreviation, then DOI normalization — recompiling with
/// latexmk after each and stopping as soon as the target is met. The referenced `.bib` file(s) are
/// rewritten with the smallest set of cleanups that reaches the target.
@Command(name = "shorten", description = "Shorten a paper's references (via latexmk) until it fits a page count.")
class Shorten implements Callable<Integer> {

    @ParentCommand
    private JabKit jabKit;

    @Mixin
    private JabKit.SharedOptions sharedOptions;

    @Mixin
    private InputOption inputOption = new InputOption();

    @Option(names = {"--pages"}, description = "Target page count. Default: one page fewer than the current length.")
    private Integer pages;

    @Option(names = {"--output"}, description = "Write the shortened .bib here instead of overwriting the referenced file (single .bib only).")
    private Path outputFile;

    /// Pairs a loaded database with the temp file it is compiled from and the cited entries to shorten.
    private record LoadedBib(BibDatabase database, Path tempPath, List<BibEntry> citedEntries) {
    }

    @Override
    public Integer call() throws ImportServiceException, CliException {
        Path texFile = inputOption.getInputFile();

        // Work on a copy so the loop never touches the user's tree until a validated result exists,
        // and so latexmk's aux/pdf/log artifacts land in a throwaway directory.
        Path workDir = copyTexDirectory(texFile);
        Path workTex = workDir.resolve(texFile.getFileName());

        LatexParserResult parsed = new DefaultLatexParser().parse(workTex)
                                                           .orElseThrow(() -> new CliException(
                                                                   "Could not read '%s'".formatted(texFile),
                                                                   Localization.lang("Could not read '%0'.", texFile.toString()),
                                                                   CommandLine.ExitCode.USAGE));

        if (parsed.getBibFiles().isEmpty()) {
            throw new CliException(
                    "No bibliography file found for '%s'".formatted(texFile),
                    Localization.lang("No bibliography file found for '%0'.", texFile.toString()),
                    CommandLine.ExitCode.USAGE);
        }

        Set<String> citedKeys = parsed.getCitations().keySet();
        List<LoadedBib> loadedBibs = loadBibs(parsed.getBibFiles(), citedKeys);
        // Internal temp writes stay quiet; only the final write-back to the user's file is announced.
        ExportService tempExport = ExportService.create(jabKit.cliPreferences, true);

        List<ReferenceShortener.Step> steps = buildSteps(loadedBibs);

        LatexCompiler compiler = new LatexCompiler(workTex);
        if (compiler.usesDocker() && !sharedOptions.porcelain) {
            System.out.println(Localization.lang("No local TeX found; compiling with the Island of TeX Docker image."));
        }

        IntSupplier measure = () -> {
            try {
                for (LoadedBib bib : loadedBibs) {
                    tempExport.saveDatabase(bib.database(), bib.tempPath());
                }
                return compiler.compileAndCountPages();
            } catch (CliException e) {
                throw new MeasureFailure(e);
            }
        };

        ReferenceShortener.Result result;
        try {
            result = new ReferenceShortener(steps, measure).shorten(
                    pages == null ? OptionalInt.empty() : OptionalInt.of(pages));
        } catch (MeasureFailure failure) {
            throw failure.cause();
        }

        return report(texFile, loadedBibs, result);
    }

    private List<ReferenceShortener.Step> buildSteps(List<LoadedBib> loadedBibs) {
        JournalAbbreviationRepository journalRepository = JournalAbbreviationLoader.loadBuiltInRepository();
        return List.of(
                new ReferenceShortener.Step(Localization.lang("shorten authors to the first author"),
                        () -> loadedBibs.forEach(bib -> FieldFormatterCleanupMapper.applyFormatters("author[minify_name_list]", bib.citedEntries()))),
                new ReferenceShortener.Step(Localization.lang("abbreviate journal names"),
                        () -> loadedBibs.forEach(bib -> bib.citedEntries()
                                                           .forEach(new AbbreviateJournalCleanup(bib.database(), journalRepository, AbbreviationType.DEFAULT, false)::cleanup))),
                new ReferenceShortener.Step(Localization.lang("clean up DOIs"),
                        () -> forEachCitedEntry(loadedBibs, new DoiCleanup()::cleanup)));
    }

    private Integer report(Path texFile, List<LoadedBib> loadedBibs, ReferenceShortener.Result result) throws ExportServiceException {
        if (result.appliedSteps().isEmpty()) {
            if (!sharedOptions.porcelain) {
                System.out.println(Localization.lang("'%0' already fits within %1 pages; nothing to shorten.",
                        texFile.toString(), Integer.toString(result.baselinePages())));
            }
            return CommandLine.ExitCode.OK;
        }

        writeBack(texFile, loadedBibs);

        String appliedSteps = String.join(", ", result.appliedSteps());
        if (result.reachedTarget()) {
            if (!sharedOptions.porcelain) {
                System.out.println(Localization.lang("Shortened '%0' from %1 to %2 pages by applying: %3.",
                        texFile.toString(), Integer.toString(result.baselinePages()), Integer.toString(result.finalPages()), appliedSteps));
            }
            return CommandLine.ExitCode.OK;
        }

        System.err.println(Localization.lang("Could not reach the target of %0 pages; best effort is %1 pages after applying: %2.",
                Integer.toString(result.targetPages()), Integer.toString(result.finalPages()), appliedSteps));
        return CommandLine.ExitCode.OK;
    }

    private void writeBack(Path texFile, List<LoadedBib> loadedBibs) throws ExportServiceException {
        ExportService writeBackExport = ExportService.create(jabKit.cliPreferences, sharedOptions.porcelain);
        boolean redirect = outputFile != null && loadedBibs.size() == 1;
        for (LoadedBib bib : loadedBibs) {
            Path destination = redirect
                               ? outputFile
                               : texFile.toAbsolutePath().getParent().resolve(bib.tempPath().getFileName());
            writeBackExport.saveDatabase(bib.database(), destination);
        }
    }

    private List<LoadedBib> loadBibs(List<Path> bibFiles, Set<String> citedKeys) throws ImportServiceException {
        List<LoadedBib> loadedBibs = new ArrayList<>();
        for (Path bibFile : bibFiles) {
            ParserResult parserResult = ImportService.importBibTexFile(bibFile, jabKit.cliPreferences, true);
            BibDatabase database = parserResult.getDatabase();
            // No cite keys, or the \nocite{*} wildcard -> every entry may be printed, so shorten all.
            boolean shortenAll = citedKeys.isEmpty() || citedKeys.contains("*");
            List<BibEntry> citedEntries = database.getEntries().stream()
                                                  .filter(entry -> shortenAll
                                                          || entry.getCitationKey().map(citedKeys::contains).orElse(false))
                                                  .toList();
            loadedBibs.add(new LoadedBib(database, bibFile, citedEntries));
        }
        return loadedBibs;
    }

    private static void forEachCitedEntry(List<LoadedBib> loadedBibs, Consumer<BibEntry> action) {
        loadedBibs.forEach(bib -> bib.citedEntries().forEach(action));
    }

    private static Path copyTexDirectory(Path texFile) throws CliException {
        Path source = texFile.toAbsolutePath().getParent();
        try {
            Path target = Files.createTempDirectory("jabkit-shorten");
            try (var paths = Files.walk(source)) {
                for (Path path : (Iterable<Path>) paths::iterator) {
                    Path destination = target.resolve(source.relativize(path));
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.createDirectories(destination.getParent());
                        Files.copy(path, destination);
                    }
                }
            }
            return target;
        } catch (IOException e) {
            throw new CliException(
                    "Could not stage '%s' for compilation: %s".formatted(texFile, e.getMessage()),
                    Localization.lang("Could not read '%0'.", texFile.toString()),
                    e, CommandLine.ExitCode.SOFTWARE);
        }
    }

    /// Carries a checked [CliException] out of the [IntSupplier] used by [ReferenceShortener].
    private static final class MeasureFailure extends RuntimeException {
        private final CliException cause;

        private MeasureFailure(CliException cause) {
            this.cause = cause;
        }

        private CliException cause() {
            return cause;
        }
    }
}
