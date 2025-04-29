package org.jabref.cli;

import java.util.List;
import java.util.Objects;

import javafx.util.Pair;

import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Holds the command line options. It parses it using Apache Commons CLI.
 */
public class CliOptions {
    private static final int WIDTH = 100; // Number of characters per line before a line break must be added.
    private static final String WRAPPED_LINE_PREFIX = ""; // If a line break is added, this prefix will be inserted at the beginning of the next line
    private static final String STRING_TABLE_DELIMITER = " : ";

    private final CommandLine commandLine;
    private final List<String> leftOver;

    public CliOptions(String[] args) throws ParseException {
        Options options = getOptions();
        this.commandLine = new DefaultParser().parse(options, args, true);
        this.leftOver = commandLine.getArgList();
    }

    public boolean isHelp() {
        return commandLine.hasOption("help");
    }

    public boolean isShowVersion() {
        return commandLine.hasOption("version");
    }

    public boolean isBlank() {
        return commandLine.hasOption("blank");
    }

    public boolean isFileImport() {
        return commandLine.hasOption("import");
    }

    public String getFileImport() {
        return commandLine.getOptionValue("import");
    }

    public boolean isImportToOpenBase() {
        return commandLine.hasOption("importToOpen");
    }

    public String getImportToOpenBase() {
        return commandLine.getOptionValue("importToOpen");
    }

    public boolean isDebugLogging() {
        return commandLine.hasOption("debug");
    }

    public boolean isPreferencesReset() {
        return commandLine.hasOption("resetPreferences");
    }

    public String getJumpToKey() {
        return commandLine.getOptionValue("jumpToKey");
    }

    public boolean isJumpToKey() {
        return commandLine.hasOption("jumpToKey");
    }

    private static Options getOptions() {
        Options options = new Options();

        // boolean options
        options.addOption("h", "help", false, Localization.lang("Display help on command line options"));
        options.addOption("b", "blank", false, Localization.lang("Do not open any files at startup"));
        options.addOption("v", "version", false, Localization.lang("Display version"));
        options.addOption(null, "debug", false, Localization.lang("Show debug level messages"));

        options.addOption(Option
                .builder("i")
                .longOpt("import")
                .desc("%s: '%s'".formatted(Localization.lang("Import file"), "-i library.bib")) // Import to a new tab
                .hasArg()
                .argName("FILE[,FORMAT]")
                .build());

        options.addOption(Option // Required for browser plugin
                .builder()
                .longOpt("importToOpen")
                .desc(Localization.lang("Same as --import, but will be imported to the opened tab"))
                .hasArg()
                .argName("FILE[,FORMAT]")
                .build());

        // Reset preferences new (all)

        options.addOption(Option
                .builder("j")
                .longOpt("jumpToKey")
                .desc("%s: '%s'".formatted(Localization.lang("Jump to the entry of the given citation key."), "-j key"))
                .hasArg()
                .argName("CITATIONKEY")
                .build());

        return options;
    }

    public void displayVersion() {
        System.out.println(getVersionInfo());
    }

    public static void printUsage(CliPreferences preferences) {
        String header = "";

        ImportFormatReader importFormatReader = new ImportFormatReader(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getCitationKeyPatternPreferences(),
                new DummyFileUpdateMonitor()
        );
        List<Pair<String, String>> importFormats = importFormatReader
                .getImportFormats().stream()
                .map(format -> new Pair<>(format.getName(), format.getId()))
                .toList();
        String importFormatsIntro = Localization.lang("Available import formats");
        String importFormatsList = "%s:%n%s%n".formatted(importFormatsIntro, alignStringTable(importFormats));

        String footer = '\n' + importFormatsList + "\nPlease report issues at https://github.com/JabRef/jabref/issues.";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(WIDTH, "jabref [OPTIONS] [BIBTEX_FILE]\n\nOptions:", header, getOptions(), footer, true);
    }

    private String getVersionInfo() {
        return "JabRef %s".formatted(new BuildInfo().version);
    }

    public List<String> getLeftOver() {
        return leftOver;
    }

    protected static String alignStringTable(List<Pair<String, String>> table) {
        StringBuilder sb = new StringBuilder();

        int maxLength = table.stream()
                             .mapToInt(pair -> Objects.requireNonNullElse(pair.getKey(), "").length())
                             .max().orElse(0);

        for (Pair<String, String> pair : table) {
            int padding = Math.max(0, maxLength - pair.getKey().length());
            sb.append(WRAPPED_LINE_PREFIX);
            sb.append(pair.getKey());

            sb.append(StringUtil.repeatSpaces(padding));

            sb.append(STRING_TABLE_DELIMITER);
            sb.append(pair.getValue());
            sb.append(OS.NEWLINE);
        }

        return sb.toString();
    }
}
