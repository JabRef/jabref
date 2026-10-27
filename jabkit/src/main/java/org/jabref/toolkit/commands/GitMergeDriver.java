package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.logic.git.merge.BibFileMerger;
import org.jabref.logic.git.merge.MergeOutcome;
import org.jabref.logic.git.merge.MergeOutcome.Merged;
import org.jabref.logic.git.merge.MergeOutcome.Refused;
import org.jabref.logic.git.merge.Refusal;
import org.jabref.logic.l10n.Localization;
import org.jabref.toolkit.converter.CygWinPathConverter;

import org.jspecify.annotations.NullMarked;
import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Parameters;
import static picocli.CommandLine.ParentCommand;

/// Git merge driver performing JabRef's semantic three-way merge of `.bib` files.
///
/// Git calls the driver with the base (`%O`), current (`%A`) and other (`%B`) version of the file
/// and expects the result in the current version's file. Exit code 0 tells Git that the merge is
/// clean; any other exit code marks the file as conflicted. This class translates between that
/// contract and [BibFileMerger], which does the merging.
@Command(name = "merge-driver",
        description = "Git merge driver: semantic three-way merge of .bib files. Writes the result to CURRENT.",
        footer = {
                "",
                "Setup:",
                "  git config --global merge.jabref.name \"JabRef semantic .bib merge\"",
                // picocli runs the help text through String.format, so the Git placeholders have to be escaped
                "  git config --global merge.jabref.driver \\",
                "    \"jabkit git merge-driver --porcelain %%O %%A %%B\"",
                "  echo \"*.bib merge=jabref\" >> .gitattributes",
                "",
                "Exit code 0: merged cleanly. 1: semantic conflicts (CURRENT keeps its version of conflicting entries)."
        })
@NullMarked
class GitMergeDriver implements Callable<Integer> {

    /// Any exit code other than 0 tells Git that the file needs manual conflict resolution
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
    // [impl->feat~jabkit.git.merge-driver~1]
    public Integer call() throws JabRefException {
        MergeOutcome outcome;
        try {
            outcome = new BibFileMerger(git.importFormatPreferences()).merge(baseFile, currentFile, otherFile);
        } catch (IOException e) {
            throw new JabRefException("Unable to merge " + currentFile, Localization.lang("Unable to merge %0.", currentFile), e);
        }

        return switch (outcome) {
            case Refused refused -> {
                refused.refusals().forEach(refusal -> System.err.println(describe(refusal)));
                yield CONFLICT;
            }
            case Merged merged when !merged.conflicts().isEmpty() -> {
                System.err.println(Localization.lang("%0 entries could not be merged automatically:", merged.conflicts().size()));
                merged.conflicts().forEach(conflict -> System.err.println("  " + describe(conflict)));
                yield CONFLICT;
            }
            case Merged _ -> {
                if (!sharedOptions.porcelain) {
                    System.out.println(Localization.lang("Merged %0 without conflicts.", currentFile));
                }
                yield CommandLine.ExitCode.OK;
            }
        };
    }

    private static String describe(Refusal refusal) {
        Path file = refusal.file();
        return switch (refusal.reason()) {
            case DUPLICATE_CITATION_KEYS ->
                    Localization.lang("Cannot merge %0: citation keys must be unique.", file);
            case PARSER_WARNINGS ->
                    Localization.lang("Cannot merge %0: the file was not parsed without warnings.", file);
            case EMPTY_ENTRY ->
                    Localization.lang("Cannot merge %0: an entry without fields is not preserved.", file);
            case UNUSED_CUSTOM_ENTRY_TYPE ->
                    Localization.lang("Cannot merge %0: a custom entry type without entries is not preserved.", file);
            case UNATTACHED_COMMENT ->
                    Localization.lang("Cannot merge %0: a comment in front of @Comment or @Preamble is not preserved.", file);
            case NON_ENTRY_CONTENT_CHANGED_IN_OTHER ->
                    Localization.lang("Cannot merge %0: content outside of entries with a citation key changed in OTHER.", file);
        };
    }

    private static String describe(ThreeWayEntryConflict conflict) {
        String citationKey = conflict.citationKey();
        if (conflict.local() == null) {
            return Localization.lang("%0: deleted in CURRENT, changed in OTHER", citationKey);
        }
        if (conflict.remote() == null) {
            return Localization.lang("%0: changed in CURRENT, deleted in OTHER", citationKey);
        }
        if (conflict.base() == null) {
            return Localization.lang("%0: added on both sides with different content", citationKey);
        }
        return Localization.lang("%0: changed on both sides with different content", citationKey);
    }
}
