package org.jabref.cli;

import java.util.List;

import org.jabref.Globals;
import org.jabref.logic.l10n.Localization;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefCLI {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefCLI.class);
    private final CommandLine cl;
    private List<String> leftOver;

    public JabRefCLI(String[] args) {

        Options options = getOptions();

        try {
            this.cl = new DefaultParser().parse(options, args);
            this.leftOver = cl.getArgList();
        } catch (ParseException e) {
            LOGGER.warn("Problem parsing arguments", e);

            this.printUsage();
            throw new RuntimeException();
        }
    }

    public static String getExportMatchesSyntax() {
        return String.format("[%s]searchTerm,outputFile: %s[,%s]",
                Localization.lang("field"),
                Localization.lang("file"),
                Localization.lang("exportFormat"));
    }

    public boolean isHelp() {
        return cl.hasOption("help");
    }

    public boolean isShowVersion() {
        return cl.hasOption("version");
    }

    public boolean isBlank() {
        return cl.hasOption("blank");
    }

    public boolean isLoadSession() {
        return cl.hasOption("loads");
    }

    public boolean isDisableGui() {
        return cl.hasOption("nogui");
    }

    public boolean isPreferencesExport() {
        return cl.hasOption("prexp");
    }

    public String getPreferencesExport() {
        return cl.getOptionValue("prexp", "jabref_prefs.xml");
    }

    public boolean isPreferencesImport() {
        return cl.hasOption("primp");
    }

    public String getPreferencesImport() {
        return cl.getOptionValue("primp", "jabref_prefs.xml");
    }

    public boolean isPreferencesReset() {
        return cl.hasOption("prdef");
    }

    public String getPreferencesReset() {
        return cl.getOptionValue("prdef");
    }

    public boolean isFileExport() {
        return cl.hasOption("output");
    }

    public String getFileExport() {
        return cl.getOptionValue("output");
    }

    public boolean isBibtexImport() {
        return cl.hasOption("importBibtex");
    }

    public String getBibtexImport() {
        return cl.getOptionValue("importBibtex");
    }

    public boolean isFileImport() {
        return cl.hasOption("import");
    }

    public String getFileImport() {
        return cl.getOptionValue("import");
    }

    public boolean isAuxImport() {
        return cl.hasOption("aux");
    }

    public String getAuxImport() {
        return cl.getOptionValue("aux");
    }

    public boolean isImportToOpenBase() {
        return cl.hasOption("importToOpen");
    }

    public String getImportToOpenBase() {
        return cl.getOptionValue("importToOpen");
    }

    public boolean isDebugLogging() {
        return cl.hasOption("debug");
    }

    public boolean isFetcherEngine() {
        return cl.hasOption("fetch");
    }

    public String getFetcherEngine() {
        return cl.getOptionValue("fetch");
    }

    public boolean isExportMatches() {
        return cl.hasOption("exportMatches");
    }

    public String getExportMatches() {
        return cl.getOptionValue("exportMatches");
    }

    public boolean isGenerateBibtexKeys() { return cl.hasOption("generateBibtexKeys"); }

    public boolean isAutomaticallySetFileLinks() { return cl.hasOption("automaticallySetFileLinks"); }

    private Options getOptions() {
        Options options = new Options();

        // boolean options
        options.addOption("v", "version", false, Localization.lang("Display version"));
        options.addOption("n", "nogui", false, Localization.lang("No GUI. Only process command line options."));
        options.addOption("h", "help", false, Localization.lang("Display help on command line options"));
        options.addOption("b", "blank", false, Localization.lang("Do not open any files at startup"));
        options.addOption(null, "debug", false, Localization.lang("Show debug level messages"));

        // The "-console" option is handled by the install4j launcher
        options.addOption(null, "console", false, Localization.lang("Show console output (only necessary when the launcher is used)"));

        options.addOption(Option.builder("i").
                longOpt("import").
                desc(String.format("%s: %s[,import format]", Localization.lang("Import file"),
                        Localization.lang("filename"))).
                hasArg().
                argName("FILE").build());

        options.addOption(
                Option.builder("ib")
                      .longOpt("importBibtex")
                      .desc(String.format("%s: %s[,importBibtex bibtexString]", Localization.lang("Import") + " " + Localization.BIBTEX, Localization.lang("filename")))
                      .hasArg()
                      .argName("FILE")
                      .build());

        options.addOption(Option.builder("o").
                longOpt("output").
                desc(String.format("%s: %s[,export format]", Localization.lang("Output or export file"),
                        Localization.lang("filename"))).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder("x").
                longOpt("prexp").
                desc(Localization.lang("Export preferences to file")).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder("p").
                longOpt("primp").
                desc(Localization.lang("Import preferences from file")).
                hasArg().
                argName("FILE").
                build());
        options.addOption(Option.builder("d").
                longOpt("prdef").
                desc(Localization.lang("Reset preferences (key1,key2,... or 'all')")).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder("a").
                longOpt("aux").
                desc(String.format("%s: %s[.aux],%s[.bib]", Localization.lang("Sublibrary from AUX"),
                        Localization.lang("file"),
                        Localization.lang("new"))).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder().
                longOpt("importToOpen").
                desc(Localization.lang("Import to open tab")).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder("f").
                longOpt("fetch").
                desc(Localization.lang("Run fetcher, e.g. \"--fetch=Medline:cancer\"")).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder("m").
                longOpt("exportMatches").
                desc(JabRefCLI.getExportMatchesSyntax()).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder("g").
                longOpt("generateBibtexKeys").
                desc(Localization.lang("Regenerate all keys for the entries in a BibTeX file"))
                .build());

        options.addOption(Option.builder("asfl").
                longOpt("automaticallySetFileLinks").
                desc(Localization.lang("Automatically set file links")).
                build());

        return options;
    }

    public void displayVersion() {
        System.out.println(getVersionInfo());
    }

    public void printUsage() {
        String header = "";

        String importFormats = Globals.IMPORT_FORMAT_READER.getImportFormatList();
        String importFormatsList = String.format("%s:%n%s%n", Localization.lang("Available import formats"), importFormats);

        String outFormats = Globals.exportFactory.getExportersAsString(70, 20, "");
        String outFormatsList = String.format("%s: %s%n", Localization.lang("Available export formats"), outFormats);

        String footer = '\n' + importFormatsList + outFormatsList + "\nPlease report issues at https://github.com/JabRef/jabref/issues.";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("jabref [OPTIONS] [BIBTEX_FILE]\n\nOptions:", header, getOptions(), footer, true);
    }

    private String getVersionInfo() {
        return String.format("JabRef %s", Globals.BUILD_INFO.getVersion());
    }

    public List<String> getLeftOver() {
        return leftOver;
    }
}
