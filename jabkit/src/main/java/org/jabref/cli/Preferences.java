package org.jabref.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.prefs.BackingStoreException;

import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Parameters;
import static picocli.CommandLine.ParentCommand;

@Command(name = "preferences", description = "Manage JabKit preferences.",
        subcommands = {
                Preferences.PreferencesReset.class,
                Preferences.PreferencesImport.class,
                Preferences.PreferencesExport.class
        })
class Preferences implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Preferences.class);

    @ParentCommand
    protected ArgumentProcessor argumentProcessor;

    @Mixin
    private JabKitArgumentProcessor.SharedOptions sharedOptions = new JabKitArgumentProcessor.SharedOptions();

    @Override
    public void run() {
        System.out.println("Specify a subcommand (reset, import, export).");
    }

    @Command(name = "reset", description = "Reset preferences.")
    class PreferencesReset implements Callable<Integer> {
        @Override
        public Integer call() {
            try {
                System.out.println(Localization.lang("Setting all preferences to default values."));
                argumentProcessor.cliPreferences.clear();
                new SharedDatabasePreferences().clear();
            } catch (BackingStoreException e) {
                System.err.println(Localization.lang("Unable to clear preferences."));
                LOGGER.error("Unable to clear preferences", e);
            }
            return 0;
        }
    }

    @Command(name = "import", description = "Import preferences from a file.")
    class PreferencesImport implements Callable<Integer> {

        @Parameters(index = "0", arity = "1", description = "The file to import preferences from.")
        Path inputFile;

        @Override
        public Integer call() {
            try {
                argumentProcessor.cliPreferences.importPreferences(inputFile);
                argumentProcessor.cliPreferences.flush();
            } catch (JabRefException ex) {
                LOGGER.error("Cannot import preferences", ex);
            }
            return 0;
        }
    }

    @Command(name = "export", description = "Export preferences to a file.")
    class PreferencesExport implements Callable<Integer> {

        @Parameters(index = "0", arity = "1", description = "The file to export preferences to.")
        Path outputFile;

        @Override
        public Integer call() {
            try {
                argumentProcessor.cliPreferences.exportPreferences(outputFile);
            } catch (JabRefException ex) {
                LOGGER.error("Cannot export preferences", ex);
            }
            return 0;
        }
    }
}
