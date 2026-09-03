package org.jabref.toolkit.commands;

import java.util.concurrent.Callable;

import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;
import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.ParentCommand;
import static picocli.CommandLine.Spec;

@Command(name = "git", description = "Git integration for .bib files.",
        subcommands = {
                GitMergeDriver.class
        })
@NullMarked
class Git implements Callable<Integer> {

    @ParentCommand
    protected JabKit jabKit;

    @Mixin
    private JabKit.SharedOptions sharedOptions;

    @Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {
        System.err.println(Localization.lang("Specify a subcommand (merge-driver)."));
        spec.commandLine().usage(System.err);
        return CommandLine.ExitCode.USAGE;
    }
}
