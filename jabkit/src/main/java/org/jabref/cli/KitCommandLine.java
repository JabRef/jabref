package org.jabref.cli;

import java.util.concurrent.Callable;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

@Command(name = "jabkit",
        mixinStandardHelpOptions = true,
        subcommands = {
                GenerateCitationKeys.class,
                CheckConsistency.class,
                CheckIntegrity.class,
                Fetch.class,
                Search.class,
                Convert.class,
                GenerateBibFromAux.class,
                Preferences.class,
                Pdf.class
        })
public class KitCommandLine implements Callable<Integer> {
    protected final CliPreferences cliPreferences;
    protected final FileUpdateMonitor fileUpdateMonitor;
    protected final BibEntryTypesManager entryTypesManager;

    @Option(names = "--debug", description = "Enable debug output")
    boolean debug;

    @Option(names = "--porcelain", description = "Enable script-friendly output")
    boolean porcelain;

    @Option(names = {"-v", "--version"}, versionHelp = true, description = "display version info")
    boolean versionInfoRequested; // VersionProvider?

    @Option(names = {"?", "-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    public KitCommandLine(CliPreferences cliPreferences, FileUpdateMonitor fileUpdateMonitor, BibEntryTypesManager entryTypesManager) {
        this.cliPreferences = cliPreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
    }

    @Override
    public Integer call() {
        // Todo: Implement
        return 0;
    }
}
