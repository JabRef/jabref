package org.jabref.toolkit.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "citationkeys",
        subcommands = {
                GenerateCitationKeys.class
        }
)
public class CitationKeyCommands implements Runnable {

    @ParentCommand
    private JabKit parentTop;

    @Override
    public void run() {
    }

    public JabKit getParent() {
        return parentTop;
    }
}
