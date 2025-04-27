package org.jabref;

import java.nio.file.Path;
import java.util.List;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static picocli.CommandLine.Command;

@Command(name = "jabref", mixinStandardHelpOptions = true)
public class GuiCommandLine {
    @Parameters(paramLabel = "<FILE>", description = "file(s) to be imported")
    public List<Path> libraries;

    @Option(names = {"--importToOpen"}, description = "import to open library")
    public boolean append;

    @Option(names = {"-r", "--reset"}, description = "reset all preferences to default values")
    public boolean resetPreferences;

    @Option(names = {"-v", "--version"}, versionHelp = true, description = "display version info")
    public boolean versionInfoRequested;

    @Option(names = {"?", "-h", "--help"}, usageHelp = true, description = "display this help message")
    public boolean usageHelpRequested;

    @Option(names = {"--debug"}, description = "enable debug logging")
    public boolean debugLogging;

    @Option(names = {"--blank"}, description = "start with an empty library")
    public boolean blank;

    @Option(names = {"--jumpToKey"}, description = "jump to the entry of the given citation key")
    public String jumpToKey;
}
