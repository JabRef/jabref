package org.jabref.logic.git.merge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.io.GitFileWriter;
import org.jabref.logic.git.merge.execution.GitMergeApplier;
import org.jabref.logic.git.merge.planning.SemanticMergeAnalyzer;
import org.jabref.logic.git.model.MergeAnalysis;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.jspecify.annotations.NullMarked;

/// Merges three versions of a `.bib` file the way a Git merge driver needs it: the merge result
/// replaces `current`, and the caller learns whether the files were merged and which entries
/// remained in conflict.
///
/// The result is `current + (other - base)`, computed entry by entry: [SemanticMergeAnalyzer]
/// plans the field changes of `other` that do not collide with `current`, [EntryPropertyMerge]
/// does the same for the entry type and the comment above an entry. Entries changed on both
/// sides in different ways are reported as conflicts and keep their `current` version.
///
/// The result is written by JabRef's `.bib` writer, which reproduces only what the parser
/// understood. [MergePreconditions] therefore refuses the merge when writing would lose content,
/// and `current` is left untouched then.
@NullMarked
public final class BibFileMerger {

    private final ImportFormatPreferences importFormatPreferences;

    public BibFileMerger(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    /// @param base    the common ancestor of both sides (Git's `%O`)
    /// @param current the version the result is written to (Git's `%A`)
    /// @param other   the version being merged in (Git's `%B`)
    /// @throws IOException if a file cannot be read, or if the result cannot be written
    public MergeOutcome merge(Path base, Path current, Path other) throws IOException {
        ParserResult baseVersion = parse(base);
        ParserResult currentVersion = parse(current);
        ParserResult otherVersion = parse(other);

        List<Refusal> refusals = MergePreconditions.check(baseVersion, currentVersion, otherVersion,
                importFormatPreferences.citationKeyPatternPreferences().getKeyPatterns());
        if (!refusals.isEmpty()) {
            return new MergeOutcome.Refused(refusals);
        }

        List<ThreeWayEntryConflict> conflicts = applyChangesOfOther(
                baseVersion.getDatabaseContext(),
                currentVersion.getDatabaseContext(),
                otherVersion.getDatabaseContext());
        write(current, currentVersion);
        return new MergeOutcome.Merged(conflicts);
    }

    private ParserResult parse(Path file) throws IOException {
        return new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor()).importDatabase(file);
    }

    /// @return the entries that keep their `current` version, because the sides changed them differently
    private static List<ThreeWayEntryConflict> applyChangesOfOther(BibDatabaseContext base, BibDatabaseContext current, BibDatabaseContext other) {
        MergeAnalysis fields = SemanticMergeAnalyzer.analyze(base, current, other);
        List<ThreeWayEntryConflict> propertyConflicts = EntryPropertyMerge.merge(base, current, other, citationKeys(fields.conflicts()));
        GitMergeApplier.applyAutoPlan(current, fields.autoPlan().without(citationKeys(propertyConflicts)));
        return Stream.concat(fields.conflicts().stream(), propertyConflicts.stream()).toList();
    }

    private static Set<String> citationKeys(List<ThreeWayEntryConflict> conflicts) {
        return conflicts.stream().map(ThreeWayEntryConflict::citationKey).collect(Collectors.toSet());
    }

    private void write(Path file, ParserResult version) throws IOException {
        BibDatabaseContext context = version.getDatabaseContext();
        // The writer emits the custom entry type definitions it knows about, not the ones the parser found
        BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();
        entryTypesManager.addCustomOrModifiedTypes(List.copyOf(version.getEntryTypes()), context.getMode());
        GitFileWriter.write(file, context, importFormatPreferences, entryTypesManager);
    }
}
