package org.jabref.cli;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class ArgumentProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentProcessor.class);

    private final CommandLine cli;

    public ArgumentProcessor(CliPreferences cliPreferences,
                             BibEntryTypesManager entryTypesManager) {

        KitCommandLine kitCli = new KitCommandLine(cliPreferences, entryTypesManager);
        cli = new CommandLine(kitCli);
    }

    public void processArguments(String[] args) {
        cli.execute(args);
    }
}
