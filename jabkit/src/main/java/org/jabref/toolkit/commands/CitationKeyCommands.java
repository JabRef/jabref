package org.jabref.toolkit.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "citationkeys",
        description = "Manage citation key operations.",
        subcommands = {
                GenerateCitationKeys.class
        }
)
public class CitationKeyCommands implements Runnable {

    @ParentCommand
    private JabKit parentTop;

    @Override
    public void run() {
        System.out.println("Testing");
    }

    public JabKit getParent() {
        return parentTop;
    }
}
