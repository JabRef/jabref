package org.jabref.toolkit.commands;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntryTypesManager;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;

@Command(
        name = "jabkit",
        mixinStandardHelpOptions = true,
        // sorted alphabetically
        subcommands = {
                Check.class,
                CitationKeys.class,
                Convert.class,
                DoiToBibtex.class,
                Fetch.class,
                GenerateBibFromAux.class,
                GetCitedWorks.class,
                GetCitingWorks.class,
                Pdf.class,
                Preferences.class,
                Pseudonymize.class,
                Search.class
        })
public class JabKit implements Runnable {

    protected final CliPreferences cliPreferences;
    protected final BibEntryTypesManager entryTypesManager;

    /// Left uninitialized so picocli's factory supplies the same shared instance
    /// used by every (sub)command in the tree; see JabKitLauncher.
    @Mixin
    private SharedOptions sharedOptions;

    @Option(names = {"-v", "--version"}, versionHelp = true, description = "display version info")
    private boolean versionInfoRequested;

    public JabKit(CliPreferences cliPreferences, BibEntryTypesManager entryTypesManager) {
        this.cliPreferences = cliPreferences;
        this.entryTypesManager = entryTypesManager;
    }

    @Override
    public void run() {
        if (versionInfoRequested) {
            System.out.println(new BuildInfo().version);
            return;
        }
        System.out.printf(BuildInfo.JABREF_BANNER + "%n", new BuildInfo().version);
    }

    public static class SharedOptions {
        @Option(names = {"-d", "--debug"}, description = "Enable debug output")
        boolean debug;

        @Option(names = {"-p", "--porcelain"}, description = "Enable script-friendly output")
        boolean porcelain;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
        private boolean usageHelpRequested = true;
    }
}
