package org.jabref.cli;

import java.io.File;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Parameters;

@Command(name = "preferences",
        description = "Manage Jabkit preferences.",
        subcommands = {
                Preferences.PreferencesReset.class,
                Preferences.PreferencesImport.class,
                Preferences.PreferencesExport.class
        })
class Preferences implements Runnable {
    @Override
    public void run() {
        System.out.println("Specify a subcommand (reset, import, export).");
    }

    @Command(name = "reset", description = "Reset preferences.")
    class PreferencesReset implements Callable<Integer> {
        @Override
        public Integer call() {
            // Logic to reset
            return 0;
        }
    }

    @Command(name = "import", description = "Import preferences from a file.")
    class PreferencesImport implements Callable<Integer> {

        @Parameters(index = "0", description = "The file to import preferences from.")
        File file;

        @Override
        public Integer call() {
            // Logic
            return 0;
        }
    }

    @Command(name = "export", description = "Export preferences to a file.")
    class PreferencesExport implements Callable<Integer> {

        @Parameters(index = "0", description = "The file to export preferences to.")
        File file;

        @Override
        public Integer call() {
            // Logic
            return 0;
        }
    }
}
