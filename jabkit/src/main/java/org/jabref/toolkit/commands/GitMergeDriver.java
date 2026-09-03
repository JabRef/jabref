package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.io.GitFileWriter;
import org.jabref.logic.git.merge.execution.GitMergeApplier;
import org.jabref.logic.git.merge.planning.SemanticMergeAnalyzer;
import org.jabref.logic.git.model.MergeAnalysis;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
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

    /// Exit code telling Git that the file could not be merged cleanly.
    private static final int CONFLICT = 1;

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

        Optional<String> unmergeable = checkMergeable(base, current, other);
        if (unmergeable.isPresent()) {
            System.err.println(unmergeable.get());
            return CONFLICT;
        }

        MergeAnalysis analysis = SemanticMergeAnalyzer.analyze(base, current, other);
        GitMergeApplier.applyAutoPlan(current, analysis.autoPlan());
        List<ThreeWayEntryConflict> conflicts = analysis.conflicts();
        applyEntryTypeChanges(base, current, other, conflicts);

        try {
            GitFileWriter.write(currentFile, current, git.jabKit.cliPreferences.getImportFormatPreferences());
        } catch (IOException e) {
            LOGGER.error("Unable to write merge result to {}", currentFile, e);
            System.err.println(Localization.lang("Unable to write to %0.", currentFile));
            return CommandLine.ExitCode.SOFTWARE;
        }

        if (conflicts.isEmpty()) {
            if (!porcelain) {
                System.out.println(Localization.lang("Merged %0 without conflicts.", currentFile));
            }
            return CommandLine.ExitCode.OK;
        }

        System.err.println(Localization.lang("%0 entries could not be merged automatically:", conflicts.size()));
        for (ThreeWayEntryConflict conflict : conflicts) {
            System.err.println("  " + describe(conflict));
        }
        return CONFLICT;
    }

    /// The merge plan is keyed by citation key and only covers entries carrying one. Content the
    /// planner does not see has to be rejected instead of being silently dropped: duplicate citation
    /// keys (planning looks at the last entry, patching at the first one), and content taken from
    /// CURRENT as is - entries without a citation key, `@String`s, preamble, epilogue and metadata.
    private Optional<String> checkMergeable(BibDatabaseContext base, BibDatabaseContext current, BibDatabaseContext other) {
        List<Path> ambiguous = new ArrayList<>();
        if (hasDuplicateCitationKeys(base)) {
            ambiguous.add(baseFile);
        }
        if (hasDuplicateCitationKeys(current)) {
            ambiguous.add(currentFile);
        }
        if (hasDuplicateCitationKeys(other)) {
            ambiguous.add(otherFile);
        }
        if (!ambiguous.isEmpty()) {
            return Optional.of(Localization.lang("Cannot merge %0: citation keys must be unique.",
                    ambiguous.stream().map(Path::toString).collect(Collectors.joining(", "))));
        }

        List<Object> otherContent = nonEntryContent(other);
        if (!otherContent.equals(nonEntryContent(base)) && !otherContent.equals(nonEntryContent(current))) {
            return Optional.of(Localization.lang("Cannot merge %0: content outside of entries with a citation key changed on both sides.", currentFile));
        }
        return Optional.empty();
    }

    private static boolean hasDuplicateCitationKeys(BibDatabaseContext context) {
        Set<String> keys = new HashSet<>();
        return context.getDatabase().getEntries().stream()
                      .flatMap(entry -> entry.getCitationKey().stream())
                      .anyMatch(key -> !keys.add(key));
    }

    private static List<Object> nonEntryContent(BibDatabaseContext context) {
        BibDatabase database = context.getDatabase();
        return List.of(
                database.getEntries().stream().filter(entry -> entry.getCitationKey().isEmpty()).map(BibEntry::getFieldMap).toList(),
                database.getStringValues().stream().collect(Collectors.toMap(BibtexString::getName, BibtexString::getContent)),
                database.getPreamble().orElse(""),
                database.getEpilog(),
                context.getMetaData());
    }

    /// [MergePlan][org.jabref.logic.git.model.MergePlan] carries field values only, so an entry type
    /// changed in OTHER alone would not reach CURRENT.
    private static void applyEntryTypeChanges(BibDatabaseContext base, BibDatabaseContext current, BibDatabaseContext other, List<ThreeWayEntryConflict> conflicts) {
        Set<String> conflictingKeys = conflicts.stream().map(GitMergeDriver::citationKeyOf).collect(Collectors.toSet());
        Map<String, BibEntry> baseEntries = entriesByCitationKey(base);
        Map<String, BibEntry> currentEntries = entriesByCitationKey(current);

        for (BibEntry otherEntry : other.getDatabase().getEntries()) {
            otherEntry.getCitationKey()
                      .filter(key -> !conflictingKeys.contains(key))
                      .ifPresent(key -> {
                          BibEntry baseEntry = baseEntries.get(key);
                          BibEntry currentEntry = currentEntries.get(key);
                          if ((baseEntry != null) && (currentEntry != null)
                                  && baseEntry.getType().equals(currentEntry.getType())
                                  && !baseEntry.getType().equals(otherEntry.getType())) {
                              currentEntry.setType(otherEntry.getType());
                          }
                      });
        }
    }

    private static Map<String, BibEntry> entriesByCitationKey(BibDatabaseContext context) {
        return context.getDatabase().getEntries().stream()
                      .filter(entry -> entry.getCitationKey().isPresent())
                      .collect(Collectors.toMap(entry -> entry.getCitationKey().orElseThrow(), entry -> entry));
    }

    private static String citationKeyOf(ThreeWayEntryConflict conflict) {
        return Optional.ofNullable(conflict.local())
                       .or(() -> Optional.ofNullable(conflict.remote()))
                       .or(() -> Optional.ofNullable(conflict.base()))
                       .flatMap(BibEntry::getCitationKey)
                       .orElse("?");
    }

    private static String describe(ThreeWayEntryConflict conflict) {
        String citationKey = citationKeyOf(conflict);
        if (Optional.ofNullable(conflict.local()).isEmpty()) {
            return Localization.lang("%0: deleted in CURRENT, changed in OTHER", citationKey);
        }
        if (Optional.ofNullable(conflict.remote()).isEmpty()) {
            return Localization.lang("%0: changed in CURRENT, deleted in OTHER", citationKey);
        }
        if (Optional.ofNullable(conflict.base()).isEmpty()) {
            return Localization.lang("%0: added on both sides with different content", citationKey);
        }
        return Localization.lang("%0: changed on both sides with different content", citationKey);
    }
}
