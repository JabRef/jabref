package org.jabref.toolkit.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "citationkeys",
        subcommands = {
                GenerateCitationKeys.class
        })
public class CitationKeys {

    @ParentCommand
    private JabKit parentCommand;

    public JabKit getParent() {
        return parentCommand;
    }
}
