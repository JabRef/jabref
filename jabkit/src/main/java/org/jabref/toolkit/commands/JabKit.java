package org.jabref.toolkit.commands;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntryTypesManager;

import picocli.CommandLine;

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

    /// Every (sub)command mixes in [SharedOptions] to allow `-p`/`-d`/`-h` at any command depth
    /// (e.g. both `jabkit -p check consistency` and `jabkit check consistency -p`). picocli
    /// resolves each mixin site independently, so without this factory each site would get its
    /// own, disconnected `SharedOptions` instance. Use the returned factory for every
    /// `CommandLine` built over a `JabKit` command tree, so all sites share one instance instead.
    public static CommandLine.IFactory createFactory() {
        SharedOptions sharedOptions = new SharedOptions();
        return new CommandLine.IFactory() {
            @Override
            public <K> K create(Class<K> cls) throws Exception {
                if (cls == SharedOptions.class) {
                    return cls.cast(sharedOptions);
                }
                return CommandLine.defaultFactory().create(cls);
            }
        };
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
