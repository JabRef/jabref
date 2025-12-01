package org.jabref.cli;

import java.nio.file.Path;
import java.util.List;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static picocli.CommandLine.Command;

@Command(name = "jabref", mixinStandardHelpOptions = true)
public class GuiCommandLine {
    @Parameters(paramLabel = "<FILE>", description = "File(s) to be imported.")
    public List<Path> libraries;

    @Option(names = {"-a", "--add"}, description = "Add to currently opened library.")
    public boolean append;

    /// @deprecated used by the browser extension
    @Deprecated
    @Option(names = {"--importBibtex"}, description = "Import BibTeX string.")
    public String importBibtex;

    /// @deprecated used by the browser extension
    @Deprecated
    @Option(names = {"-importToOpen", "--importToOpen"}, description = "Same as --import, but will be imported to the opened tab.")
    public String importToOpen;

    @Option(names = {"--reset"}, description = "Reset all preferences to default values.")
    public boolean resetPreferences;

    @Option(names = {"-v", "--version"}, versionHelp = true, description = "Display version info.")
    public boolean versionInfoRequested;

    @Option(names = {"?", "-h", "--help"}, usageHelp = true, description = "Display this help message.")
    public boolean usageHelpRequested;

    @Option(names = {"--debug"}, description = "Enable debug logging.")
    public boolean debugLogging;

    @Option(names = {"-b", "--blank"}, description = "Start with an empty library.")
    public boolean blank;

    @Option(names = {"-j", "--jumpToKey"}, description = "Jump to the entry of the given citation key.")
    public String jumpToKey;
}
