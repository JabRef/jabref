package org.jabref;

import java.nio.file.Path;
import java.util.List;

import picocli.CommandLine;

@CommandLine.Command(name = "jabref", mixinStandardHelpOptions = true)
public class GuiCommandLine {
    @picocli.CommandLine.Parameters(paramLabel = "<FILE>", description = "file(s) to be imported")
    protected List<Path> libraries;

    @picocli.CommandLine.Option(names = {"--importToOpen"}, description = "import to open library")
    protected boolean append;

    @picocli.CommandLine.Option(names = {"-r", "--reset"}, description = "reset all preferences to default values")
    protected boolean resetPreferences;

    @picocli.CommandLine.Option(names = {"-v", "--version"}, versionHelp = true, description = "display version info")
    protected boolean versionInfoRequested;

    @picocli.CommandLine.Option(names = {"?", "-h", "--help"}, usageHelp = true, description = "display this help message")
    protected boolean usageHelpRequested;

    @picocli.CommandLine.Option(names = {"--debug"}, description = "enable debug logging")
    protected boolean debugLogging;

    @picocli.CommandLine.Option(names = {"--blank"}, description = "start with an empty library")
    protected boolean blank;

    @picocli.CommandLine.Option(names = {"--jumpToKey"}, description = "jump to the entry of the given citation key")
    protected String jumpToKey;
}
