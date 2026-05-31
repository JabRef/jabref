package org.jabref.toolkit.commands;

import java.util.concurrent.Callable;

import org.jabref.logic.l10n.Localization;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.ParentCommand;

@Command(name = "pdf", description = "Manage PDF metadata.",
        subcommands = {
                PdfUpdate.class
                // RemoveComments.class
                // RemoveEmbedded.class
                // RemovePrivateFields.class
        })
class Pdf implements Callable<Integer> {
    @ParentCommand
    protected JabKit argumentProcessor;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    @Override
    public Integer call() {
        System.err.println(Localization.lang("Specify a subcommand (write-xmp, update)."));
        return 2;
    }
}
