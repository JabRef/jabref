package org.jabref.toolkit.commands;

import org.jabref.toolkit.ArgumentProcessor;

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
class Pdf implements Runnable {
    @ParentCommand
    protected ArgumentProcessor argumentProcessor;

    @Mixin
    private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

    @Override
    public void run() {
        System.out.println("Specify a subcommand (write-xmp, update).");
    }
}
