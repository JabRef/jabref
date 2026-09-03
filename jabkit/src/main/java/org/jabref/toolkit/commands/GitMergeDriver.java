package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jabref.logic.exporter.MetaDataSerializer;
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
import org.jspecify.annotations.Nullable;
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

        List<String> unmergeable = checkMergeable(base, current, other);
        if (!unmergeable.isEmpty()) {
            unmergeable.forEach(System.err::println);
            return CONFLICT;
        }

        MergeAnalysis analysis = SemanticMergeAnalyzer.analyze(base, current, other);
        List<ThreeWayEntryConflict> conflicts = new ArrayList<>(analysis.conflicts());
        List<ThreeWayEntryConflict> typeConflicts = mergeEntryTypes(base, current, other, conflicts);
        conflicts.addAll(typeConflicts);
        if (!typeConflicts.isEmpty()) {
            // The field-level plan cannot be applied to some entries only, and CURRENT already is the
            // version the conflicting entries have to keep - so it is left untouched altogether.
            reportConflicts(conflicts);
            return CONFLICT;
        }

        GitMergeApplier.applyAutoPlan(current, analysis.autoPlan());

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

        reportConflicts(conflicts);
        return CONFLICT;
    }

    private static void reportConflicts(List<ThreeWayEntryConflict> conflicts) {
        System.err.println(Localization.lang("%0 entries could not be merged automatically:", conflicts.size()));
        for (ThreeWayEntryConflict conflict : conflicts) {
            System.err.println("  " + describe(conflict));
        }
    }

    /// The merge plan is keyed by citation key and only covers entries carrying one. Content the
    /// planner does not see has to be rejected instead of being silently dropped: duplicate citation
    /// keys (planning looks at the last entry, patching at the first one), and content taken from
    /// CURRENT as is - entries without a citation key, `@String`s, preamble, epilogue and metadata.
    ///
    /// @return one message per reason why the files cannot be merged; empty if they can
    private List<String> checkMergeable(BibDatabaseContext base, BibDatabaseContext current, BibDatabaseContext other) {
        List<String> reasons = new ArrayList<>();

        List<Path> duplicates = new ArrayList<>();
        if (hasDuplicateCitationKeys(base)) {
            duplicates.add(baseFile);
        }
        if (hasDuplicateCitationKeys(current)) {
            duplicates.add(currentFile);
        }
        if (hasDuplicateCitationKeys(other)) {
            duplicates.add(otherFile);
        }
        if (!duplicates.isEmpty()) {
            reasons.add(Localization.lang("Cannot merge %0: citation keys must be unique.",
                    duplicates.stream().map(Path::toString).collect(Collectors.joining(", "))));
        }

        List<Object> otherContent = nonEntryContent(other);
        if (!otherContent.equals(nonEntryContent(base)) && !otherContent.equals(nonEntryContent(current))) {
            reasons.add(Localization.lang("Cannot merge %0: content outside of entries with a citation key changed on both sides.", currentFile));
        }
        return reasons;
    }

    private static boolean hasDuplicateCitationKeys(BibDatabaseContext context) {
        Set<String> keys = new HashSet<>();
        return context.getDatabase().getEntries().stream()
                      .flatMap(entry -> entry.getCitationKey().stream())
                      .anyMatch(key -> !keys.add(key));
    }

    /// Everything that is written to the file but never looked at by the merge planner. Metadata is
    /// compared in its serialized form, because [MetaData#equals] ignores unknown metadata items.
    private List<Object> nonEntryContent(BibDatabaseContext context) {
        BibDatabase database = context.getDatabase();
        return List.of(
                database.getEntries().stream()
                        .filter(entry -> entry.getCitationKey().isEmpty())
                        .map(entry -> List.of(entry.getType(), entry.getFieldMap()))
                        .toList(),
                database.getStringValues().stream().collect(Collectors.toMap(BibtexString::getName, BibtexString::getContent)),
                database.getPreamble().orElse(""),
                database.getEpilog(),
                MetaDataSerializer.getSerializedStringMap(context.getMetaData(),
                        git.jabKit.cliPreferences.getImportFormatPreferences().citationKeyPatternPreferences().getKeyPatterns()));
    }

    /// [ConflictRules][org.jabref.logic.git.merge.planning.util.ConflictRules] compares field maps,
    /// which do not contain the entry type, and [MergePlan][org.jabref.logic.git.model.MergePlan]
    /// cannot carry a type change. Entry types are therefore merged here: a type changed in OTHER
    /// alone is applied to CURRENT, every other divergence is a conflict.
    ///
    /// @return the entries whose type cannot be merged automatically
    private static List<ThreeWayEntryConflict> mergeEntryTypes(BibDatabaseContext base,
                                                               BibDatabaseContext current,
                                                               BibDatabaseContext other,
                                                               List<ThreeWayEntryConflict> knownConflicts) {
        Set<String> knownConflictKeys = knownConflicts.stream().map(GitMergeDriver::citationKeyOf).collect(Collectors.toSet());
        Map<String, BibEntry> baseEntries = entriesByCitationKey(base);
        Map<String, BibEntry> currentEntries = entriesByCitationKey(current);
        Map<String, BibEntry> otherEntries = entriesByCitationKey(other);

        Set<String> keys = new LinkedHashSet<>(baseEntries.keySet());
        keys.addAll(currentEntries.keySet());
        keys.addAll(otherEntries.keySet());

        List<ThreeWayEntryConflict> typeConflicts = new ArrayList<>();
        for (String key : keys) {
            if (knownConflictKeys.contains(key)) {
                continue;
            }
            @Nullable BibEntry baseEntry = baseEntries.get(key);
            @Nullable BibEntry currentEntry = currentEntries.get(key);
            @Nullable BibEntry otherEntry = otherEntries.get(key);

            if (baseEntry == null) {
                // added on both sides - the planner unions their fields, but cannot union their types
                if ((currentEntry != null) && (otherEntry != null) && !currentEntry.getType().equals(otherEntry.getType())) {
                    typeConflicts.add(new ThreeWayEntryConflict(null, currentEntry, otherEntry));
                }
                continue;
            }
            if (currentEntry == null) {
                // deleted in CURRENT: accepting the deletion would drop a type change of OTHER
                if ((otherEntry != null) && !baseEntry.getType().equals(otherEntry.getType())) {
                    typeConflicts.add(new ThreeWayEntryConflict(baseEntry, null, otherEntry));
                }
                continue;
            }
            if (otherEntry == null) {
                if (!baseEntry.getType().equals(currentEntry.getType())) {
                    typeConflicts.add(new ThreeWayEntryConflict(baseEntry, currentEntry, null));
                }
                continue;
            }
            boolean changedInCurrent = !baseEntry.getType().equals(currentEntry.getType());
            boolean changedInOther = !baseEntry.getType().equals(otherEntry.getType());
            if (changedInCurrent && changedInOther) {
                if (!currentEntry.getType().equals(otherEntry.getType())) {
                    typeConflicts.add(new ThreeWayEntryConflict(baseEntry, currentEntry, otherEntry));
                }
            } else if (changedInOther) {
                currentEntry.setType(otherEntry.getType());
            }
        }
        return typeConflicts;
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
