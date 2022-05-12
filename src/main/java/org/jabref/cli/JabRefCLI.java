package org.jabref.cli;

import java.util.List;

import org.jabref.gui.Globals;
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

    private static final int WIDTH = 100; // Number of characters per line
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefCLI.class);
    private final CommandLine cl;
    private final List<String> leftOver;

    public JabRefCLI(String[] args) throws ParseException {
        Options options = getOptions();

        this.cl = new DefaultParser().parse(options, args, true);
        this.leftOver = cl.getArgList();
    }

    public static String getExportMatchesSyntax() {
        return String.format("[%s]searchTerm,outputFile:%s[,%s]",
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

    public boolean isGenerateCitationKeys() {
        return cl.hasOption("generateCitationKeys");
    }

    public boolean isAutomaticallySetFileLinks() {
        return cl.hasOption("automaticallySetFileLinks");
    }

    public boolean isWriteXMPtoPdf() {
        return cl.hasOption("writeXMPtoPdf");
    }

    public boolean isEmbeddBibfileInPdf() {
        return cl.hasOption("embeddBibfileInPdf");
    }

    public boolean isWriteMetadatatoPdf() {
        return cl.hasOption("writeMetadatatoPdf");
    }

    public String getWriteMetadatatoPdf() {
        return cl.hasOption("writeMetadatatoPdf") ? cl.getOptionValue("writeMetadatatoPdf") :
                cl.hasOption("writeXMPtoPdf") ? cl.getOptionValue("writeXMPtoPdf") :
                cl.hasOption("embeddBibfileInPdf") ? cl.getOptionValue("embeddBibfileInPdf") : null;
    }

    private static Options getOptions() {
        Options options = new Options();

        // boolean options
        options.addOption("h", "help", false, Localization.lang("Display help on command line options"));
        options.addOption("n", "nogui", false, Localization.lang("No GUI. Only process command line options"));
        options.addOption("asfl", "automaticallySetFileLinks", false, Localization.lang("Automatically set file links"));
        options.addOption("g", "generateCitationKeys", false, Localization.lang("Regenerate all keys for the entries in a BibTeX file"));
        options.addOption("b", "blank", false, Localization.lang("Do not open any files at startup"));
        options.addOption("v", "version", false, Localization.lang("Display version"));
        options.addOption(null, "debug", false, Localization.lang("Show debug level messages"));

        // The "-console" option is handled by the install4j launcher
        options.addOption(null, "console", false, Localization.lang("Show console output (only when the launcher is used)"));

        options.addOption(Option
                .builder("i")
                .longOpt("import")
                .desc(String.format("%s: '%s'", Localization.lang("Import file"), "-i library.bib"))
                .hasArg()
                .argName("FILE[,FORMAT]")
                .build());

        options.addOption(Option
                .builder()
                .longOpt("importToOpen")
                .desc(Localization.lang("Same as --import, but will be imported to the opened tab"))
                .hasArg()
                .argName("FILE[,FORMAT]")
                .build());

        options.addOption(Option
                .builder("ib")
                .longOpt("importBibtex")
                .desc(String.format("%s: '%s'", Localization.lang("Import BibTeX"), "-ib @article{entry}"))
                .hasArg()
                .argName("BIBTEXT_STRING")
                .build());

        options.addOption(Option
                .builder("o")
                .longOpt("output")
                .desc(String.format("%s: '%s'", Localization.lang("Export an input to a file"), "-i db.bib -o db.htm,html"))
                .hasArg()
                .argName("FILE[,FORMAT]")
                .build());

        options.addOption(Option
                .builder("m")
                .longOpt("exportMatches")
                .desc(String.format("%s: '%s'", Localization.lang("Matching"), "-i db.bib -m author=Newton,search.htm,html"))
                .hasArg()
                .argName("QUERY,FILE[,FORMAT]")
                .build());

        options.addOption(Option
                .builder("f")
                .longOpt("fetch")
                .desc(String.format("%s: '%s'", Localization.lang("Run fetcher"), "-f Medline/PubMed:cancer"))
                .hasArg()
                .argName("FETCHER:QUERY")
                .build());

        options.addOption(Option
                .builder("a")
                .longOpt("aux")
                .desc(String.format("%s: '%s'", Localization.lang("Sublibrary from AUX to BibTeX"), "-a thesis.aux,new.bib"))
                .hasArg()
                .argName("FILE[.aux],FILE[.bib] FILE")
                .build());

        options.addOption(Option
                .builder("x")
                .longOpt("prexp")
                .desc(String.format("%s: '%s'", Localization.lang("Export preferences to a file"), "-x prefs.xml"))
                .hasArg()
                .argName("[FILE]")
                .build());

        options.addOption(Option
                .builder("p")
                .longOpt("primp")
                .desc(String.format("%s: '%s'", Localization.lang("Import preferences from a file"), "-p prefs.xml"))
                .hasArg()
                .argName("[FILE]")
                .build());

        options.addOption(Option
                .builder("d")
                .longOpt("prdef")
                .desc(String.format("%s: '%s'", Localization.lang("Reset preferences"), "-d mainFontSize,newline' or '-d all"))
                .hasArg()
                .argName("KEY1[,KEY2][,KEYn] | all")
                .build());

        options.addOption(Option
                .builder()
                .longOpt("writeXMPtoPdf")
                .desc(String.format("%s: '%s'", Localization.lang("Write BibTeXEntry as XMP metadata to PDF."), "-w pathToMyOwnPaper.pdf"))
                .hasArg()
                .argName("CITEKEY1[,CITEKEY2][,CITEKEYn] | PDF1[,PDF2][,PDFn] | all")
                .build());

        options.addOption(Option
                .builder()
                .longOpt("embeddBibfileInPdf")
                .desc(String.format("%s: '%s'", Localization.lang("Embed BibTeXEntry in PDF."), "-w pathToMyOwnPaper.pdf"))
                .hasArg()
                .argName("CITEKEY1[,CITEKEY2][,CITEKEYn] | PDF1[,PDF2][,PDFn] | all")
                .build());

        options.addOption(Option
                .builder("w")
                .longOpt("writeMetadatatoPdf")
                .desc(String.format("%s: '%s'", Localization.lang("Write BibTeXEntry as metadata to PDF."), "-w pathToMyOwnPaper.pdf"))
                .hasArg()
                .argName("CITEKEY1[,CITEKEY2][,CITEKEYn] | PDF1[,PDF2][,PDFn] | all")
                .build());

        return options;
    }

    public void displayVersion() {
        System.out.println(getVersionInfo());
    }

    public static void printUsage() {
        String header = "";

        String importFormats = Globals.IMPORT_FORMAT_READER.getImportFormatList();
        String importFormatsList = String.format("%s:%n%s%n", Localization.lang("Available import formats"), importFormats);

        String outFormats = Globals.exportFactory.getExportersAsString(70, 20, "");
        String outFormatsList = String.format("%s: %s%n", Localization.lang("Available export formats"), outFormats);

        String footer = '\n' + importFormatsList + outFormatsList + "\nPlease report issues at https://github.com/JabRef/jabref/issues.";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(WIDTH, "jabref [OPTIONS] [BIBTEX_FILE]\n\nOptions:", header, getOptions(), footer, true);
    }

    private String getVersionInfo() {
        return String.format("JabRef %s", Globals.BUILD_INFO.version);
    }

    public List<String> getLeftOver() {
        return leftOver;
    }
}
