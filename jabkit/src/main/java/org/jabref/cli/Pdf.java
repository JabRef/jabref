package org.jabref.cli;

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
    protected JabKitArgumentProcessor argumentProcessor;

    @Mixin
    private JabKitArgumentProcessor.SharedOptions sharedOptions = new JabKitArgumentProcessor.SharedOptions();

    @Override
    public void run() {
        System.out.println("Specify a subcommand (write-xmp, update).");
    }
}
