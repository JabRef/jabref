package org.jabref.toolkit.commands;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.ParentCommand;

@Command(name = "check", description = "Check the integrity and consistency of a library.",
        subcommands = {
                CheckConsistency.class,
                CheckIntegrity.class
        })
class Check implements Runnable {

    @ParentCommand
    protected JabKit jabKit;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    @Override
    public void run() {
        System.out.println("Specify a subcommand (consistency, integrity).");
    }
}
