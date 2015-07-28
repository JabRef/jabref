package net.sf.jabref;

import net.sf.jabref.export.ExportFormats;
import org.apache.commons.cli.*;

public class JabRefCLI {

    private String[] leftOver;
    private final CommandLine cl;


    public boolean isHelp() {
        return cl.hasOption("help");
    }

    public boolean isShowVersion() {
        return cl.hasOption("version");
    }

    public boolean isDisableSplash() {
        return cl.hasOption("nosplash");
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

    public JabRefCLI(String[] args) {

        Options options = getOptions();

        try {
            this.cl = new DefaultParser().parse(options, args);
            this.leftOver = cl.getArgs();
        } catch (ParseException e) {
            e.printStackTrace();

            this.printUsage();
            throw new RuntimeException();
        }
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

    private Options getOptions() {
        Options options = new Options();

        // boolean options
        options.addOption("v", "version", false, Globals.lang("Display version"));
        options.addOption("n", "nogui", false, Globals.lang("No GUI. Only process command line options."));
        options.addOption("s", "nosplash", false, Globals.lang("Do not show splash window at startup"));
        options.addOption("h", "help", false, Globals.lang("Display help on command line options"));
        options.addOption("l", "loads", false, Globals.lang("Load session"));
        options.addOption("b", "blank", false, Globals.lang("Do not open any files at startup"));

        options.addOption(Option.builder("i").
                longOpt("import").
                desc(String.format("%s: %s[,import format]", Globals.lang("Import file"), Globals.lang("filename"))).
                hasArg().
                argName("FILE").build());

        options.addOption(Option.builder("o").
                longOpt("output").
                desc(String.format("%s: %s[,export format]", Globals.lang("Output or export file"), Globals.lang("filename"))).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder("x").
                longOpt("prexp").
                desc(Globals.lang("Export preferences to file")).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder("p").
                longOpt("primp").
                desc(Globals.lang("Import preferences from file")).
                hasArg().
                argName("FILE").
                build());
        options.addOption(Option.builder("d").
                longOpt("prdef").
                desc(Globals.lang("Reset preferences (key1,key2,... or 'all')")).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder("a").
                longOpt("aux").
                desc(String.format("%s: %s[.aux],%s[.bib]", Globals.lang("Subdatabase from aux"), Globals.lang("file"), Globals.lang("new"))).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder().
                longOpt("importToOpen").
                desc(Globals.lang("Import to open tab")).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder("f").
                longOpt("fetch").
                desc(Globals.lang("Run Fetcher, e.g. \"--fetch=Medline:cancer\"")).
                hasArg().
                argName("FILE").
                build());

        options.addOption(Option.builder("m").
                longOpt("exportMatches").
                desc(JabRefCLI.getExportMatchesSyntax()).
                hasArg().
                argName("FILE").
                build());

        return options;
    }

    public void displayVersion() {
        System.out.println(getVersionInfo());
    }

    public void printUsage() {
        String header = "";

        String importFormats = Globals.importFormatReader.getImportFormatList();
        String importFormatsList = String.format("%s:%n%s%n", Globals.lang("Available import formats"), importFormats);

        String outFormats = ExportFormats.getConsoleExportList(70, 20, "");
        String outFormatsList = String.format("%s: %s%n", Globals.lang("Available export formats"), outFormats);

        String footer = '\n' + importFormatsList + outFormatsList + "\nPlease report issues at http://sourceforge.net/p/jabref/bugs/";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("jabref [OPTIONS] [BIBTEX_FILE]\n\nOptions:", header, getOptions(), footer, true);
    }

    private String getVersionInfo() {
        return String.format("JabRef %s", Globals.BUILD_INFO.getVersion());
    }

    public String[] getLeftOver() {
        return leftOver;
    }

    public static String getExportMatchesSyntax() {
        return String.format("[%s]searchTerm,outputFile: %s[,%s]", Globals.lang("field"), Globals.lang("file"), Globals.lang("exportFormat"));
    }
}