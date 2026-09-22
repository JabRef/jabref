package org.jabref.toolkit.commands;

import org.jabref.logic.importer.ImportFormatPreferences;

import org.jspecify.annotations.NullMarked;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.ParentCommand;

/// Groups the Git subcommands. Not executable itself: picocli reports a missing subcommand.
@Command(name = "git", description = "Git integration for .bib files.",
        subcommands = {
                GitMergeDriver.class
        })
@NullMarked
class Git {

    @ParentCommand
    private JabKit jabKit;

    @Mixin
    private JabKit.SharedOptions sharedOptions;

    ImportFormatPreferences importFormatPreferences() {
        return jabKit.cliPreferences.getImportFormatPreferences();
    }
}
