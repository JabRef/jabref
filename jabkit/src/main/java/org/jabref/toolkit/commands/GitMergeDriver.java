package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.io.GitFileWriter;
import org.jabref.logic.git.merge.execution.GitMergeApplier;
import org.jabref.logic.git.merge.planning.SemanticMergeAnalyzer;
import org.jabref.logic.git.model.MergeAnalysis;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.toolkit.converter.CygWinPathConverter;
import org.jabref.toolkit.exception.ImportServiceException;
import org.jabref.toolkit.service.ImportService;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/// Git merge driver performing JabRef's semantic three-way merge of `.bib` files.
///
/// Git calls the driver with the base (`%O`), current (`%A`) and other (`%B`) version of the file.
/// The merge result has to be written to the current version's file. Exit code 0 tells Git the
/// merge is clean; any other exit code marks the file as conflicted.
///
/// Non-conflicting changes of the other side are applied to the current version. Entries with
/// semantic conflicts keep the current side's version and are reported on stderr.
///
/// Setup:
///
/// ```
/// git config --global merge.jabref.name "JabRef semantic .bib merge"
/// git config --global merge.jabref.driver "jabkit git merge-driver --porcelain %O %A %B"
/// echo "*.bib merge=jabref" >> .gitattributes
/// ```
@Command(name = "merge-driver",
        description = "Git merge driver: semantic three-way merge of .bib files. Writes the result to CURRENT.",
        footer = {
                "",
                "Setup:",
                "  git config --global merge.jabref.name \"JabRef semantic .bib merge\"",
                "  git config --global merge.jabref.driver \"jabkit git merge-driver --porcelain %O %A %B\"",
                "  echo \"*.bib merge=jabref\" >> .gitattributes",
                "",
                "Exit code 0: merged cleanly. 1: semantic conflicts (CURRENT keeps its version of conflicting entries)."
        })
@NullMarked
class GitMergeDriver implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitMergeDriver.class);

    @ParentCommand
    private Git git;

    @Mixin
    private JabKit.SharedOptions sharedOptions;

    @Parameters(index = "0", paramLabel = "BASE", converter = CygWinPathConverter.class,
            description = "Common ancestor version (Git placeholder %%O)")
    private Path baseFile;

    @Parameters(index = "1", paramLabel = "CURRENT", converter = CygWinPathConverter.class,
            description = "Current version (Git placeholder %%A); the merge result is written here")
    private Path currentFile;

    @Parameters(index = "2", paramLabel = "OTHER", converter = CygWinPathConverter.class,
            description = "Other version being merged (Git placeholder %%B)")
    private Path otherFile;

    @Override
    // [impl->req~jabkit.git.merge-driver~1]
    public Integer call() throws ImportServiceException {
        boolean porcelain = sharedOptions.porcelain;
        BibDatabaseContext base = ImportService.importBibTexFile(baseFile, git.jabKit.cliPreferences, porcelain).getDatabaseContext();
        BibDatabaseContext current = ImportService.importBibTexFile(currentFile, git.jabKit.cliPreferences, porcelain).getDatabaseContext();
        BibDatabaseContext other = ImportService.importBibTexFile(otherFile, git.jabKit.cliPreferences, porcelain).getDatabaseContext();

        MergeAnalysis analysis = SemanticMergeAnalyzer.analyze(base, current, other);
        GitMergeApplier.applyAutoPlan(current, analysis.autoPlan());

        try {
            GitFileWriter.write(currentFile, current, git.jabKit.cliPreferences.getImportFormatPreferences());
        } catch (IOException e) {
            LOGGER.error("Unable to write merge result to {}", currentFile, e);
            System.err.println(Localization.lang("Unable to write to %0.", currentFile));
            return CommandLine.ExitCode.SOFTWARE;
        }

        List<ThreeWayEntryConflict> conflicts = analysis.conflicts();
        if (conflicts.isEmpty()) {
            if (!porcelain) {
                System.out.println(Localization.lang("Merged %0 without conflicts.", currentFile));
            }
            return CommandLine.ExitCode.OK;
        }

        System.err.println(Localization.lang("%0 entries could not be merged automatically:", conflicts.size()));
        for (ThreeWayEntryConflict conflict : conflicts) {
            System.err.println("  " + citationKeyOf(conflict) + ": " + describe(conflict));
        }
        return 1;
    }

    private static String citationKeyOf(ThreeWayEntryConflict conflict) {
        return Optional.ofNullable(conflict.local())
                       .or(() -> Optional.ofNullable(conflict.remote()))
                       .or(() -> Optional.ofNullable(conflict.base()))
                       .flatMap(BibEntry::getCitationKey)
                       .orElse("?");
    }

    private static String describe(ThreeWayEntryConflict conflict) {
        if (Optional.ofNullable(conflict.local()).isEmpty()) {
            return Localization.lang("deleted in CURRENT, changed in OTHER");
        }
        if (Optional.ofNullable(conflict.remote()).isEmpty()) {
            return Localization.lang("changed in CURRENT, deleted in OTHER");
        }
        if (Optional.ofNullable(conflict.base()).isEmpty()) {
            return Localization.lang("added on both sides with different content");
        }
        return Localization.lang("changed on both sides with different content");
    }
}
