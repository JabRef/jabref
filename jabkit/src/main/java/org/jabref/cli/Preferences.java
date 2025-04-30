package org.jabref.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.prefs.BackingStoreException;

import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Parameters;
import static picocli.CommandLine.ParentCommand;

@Command(name = "preferences",
        description = "Manage Jabkit preferences.",
        subcommands = {
                Preferences.PreferencesReset.class,
                Preferences.PreferencesImport.class,
                Preferences.PreferencesExport.class
        })
class Preferences implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Preferences.class);

    @ParentCommand
    protected KitCommandLine kitCommandLine;

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
                    kitCommandLine.cliPreferences.clear();
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

        @Parameters(index = "0", description = "The file to import preferences from.")
        File file;

        @Override
        public Integer call() {
            try {
                kitCommandLine.cliPreferences.importPreferences(Path.of(kitCommandLine.cliPreferences.getPreferencesImport()));
                Injector.setModelOrService(BibEntryTypesManager.class, kitCommandLine.cliPreferences.getCustomEntryTypesRepository()); // ToDo
            } catch (JabRefException ex) {
                LOGGER.error("Cannot import preferences", ex);
            }
            return 0;
        }
    }

    @Command(name = "export", description = "Export preferences to a file.")
    class PreferencesExport implements Callable<Integer> {

        @Parameters(index = "0", description = "The file to export preferences to.")
        File file;

        @Override
        public Integer call() {
            // Logic // TODO
            return 0;
        }
    }
}
