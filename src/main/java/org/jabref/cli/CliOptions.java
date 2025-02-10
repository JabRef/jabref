package org.jabref.cli;

import java.util.List;
import java.util.Objects;

import javafx.util.Pair;

import org.jabref.logic.exporter.ExporterFactory;
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

    public static String getExportMatchesSyntax() {
        return "[%s]searchTerm,outputFile:%s[,%s]".formatted(
                Localization.lang("field"),
                Localization.lang("file"),
                Localization.lang("exportFormat"));
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

    public boolean isDisableGui() {
        return commandLine.hasOption("nogui");
    }

    public boolean isCheckConsistency() {
        return commandLine.hasOption("check-consistency");
    }

    public String getCheckConsistency() {
        return commandLine.getOptionValue("check-consistency");
    }

    public String getCheckConsistencyOutputFormat() {
        return commandLine.getOptionValue("output-format");
    }

    public boolean isPorcelainOutputMode() {
        return commandLine.hasOption("porcelain");
    }

    public boolean isPreferencesExport() {
        return commandLine.hasOption("prexp");
    }

    public String getPreferencesExport() {
        return commandLine.getOptionValue("prexp", "jabref_prefs.xml");
    }

    public boolean isPreferencesImport() {
        return commandLine.hasOption("primp");
    }

    public String getPreferencesImport() {
        return commandLine.getOptionValue("primp", "jabref_prefs.xml");
    }

    public boolean isPreferencesReset() {
        return commandLine.hasOption("prdef");
    }

    public String getPreferencesReset() {
        return commandLine.getOptionValue("prdef");
    }

    public boolean isFileExport() {
        return commandLine.hasOption("output");
    }

    public String getFileExport() {
        return commandLine.getOptionValue("output");
    }

    public boolean isBibtexImport() {
        return commandLine.hasOption("importBibtex");
    }

    public String getBibtexImport() {
        return commandLine.getOptionValue("importBibtex");
    }

    public boolean isFileImport() {
        return commandLine.hasOption("import");
    }

    public String getFileImport() {
        return commandLine.getOptionValue("import");
    }

    public boolean isAuxImport() {
        return commandLine.hasOption("aux");
    }

    public String getAuxImport() {
        return commandLine.getOptionValue("aux");
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

    public boolean isFetcherEngine() {
        return commandLine.hasOption("fetch");
    }

    public String getFetcherEngine() {
        return commandLine.getOptionValue("fetch");
    }

    public boolean isExportMatches() {
        return commandLine.hasOption("exportMatches");
    }

    public String getExportMatches() {
        return commandLine.getOptionValue("exportMatches");
    }

    public boolean isGenerateCitationKeys() {
        return commandLine.hasOption("generateCitationKeys");
    }

    public boolean isWriteXmpToPdf() {
        return commandLine.hasOption("writeXmpToPdf");
    }

    public boolean isEmbedBibFileInPdf() {
        return commandLine.hasOption("embedBibFileInPdf");
    }

    public boolean isWriteMetadataToPdf() {
        return commandLine.hasOption("writeMetadataToPdf");
    }

    public String getWriteMetadataToPdf() {
        return commandLine.hasOption("writeMetadatatoPdf") ? commandLine.getOptionValue("writeMetadataToPdf") :
                commandLine.hasOption("writeXMPtoPdf") ? commandLine.getOptionValue("writeXmpToPdf") :
                        commandLine.hasOption("embeddBibfileInPdf") ? commandLine.getOptionValue("embeddBibfileInPdf") : null;
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
        options.addOption("n", "nogui", false, Localization.lang("No GUI. Only process command line options"));
        options.addOption("g", "generateCitationKeys", false, Localization.lang("Regenerate all keys for the entries in a BibTeX file"));
        options.addOption("b", "blank", false, Localization.lang("Do not open any files at startup"));
        options.addOption("v", "version", false, Localization.lang("Display version"));
        options.addOption(null, "debug", false, Localization.lang("Show debug level messages"));

        options.addOption(Option
                .builder("i")
                .longOpt("import")
                .desc("%s: '%s'".formatted(Localization.lang("Import file"), "-i library.bib"))
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
                .desc("%s: '%s'".formatted(Localization.lang("Import BibTeX"), "-ib @article{entry}"))
                .hasArg()
                .argName("BIBTEX_STRING")
                .build());

        options.addOption(Option
                .builder("o")
                .longOpt("output")
                .desc("%s: '%s'".formatted(Localization.lang("Export an input to a file"), "-i db.bib -o db.htm,html"))
                .hasArg()
                .argName("FILE[,FORMAT]")
                .build());

        options.addOption(Option
                .builder("m")
                .longOpt("exportMatches")
                .desc("%s: '%s'".formatted(Localization.lang("Matching"), "-i db.bib -m author=Newton,search.htm,html"))
                .hasArg()
                .argName("QUERY,FILE[,FORMAT]")
                .build());

        options.addOption(Option
                .builder("f")
                .longOpt("fetch")
                .desc("%s: '%s'".formatted(Localization.lang("Run fetcher"), "-f Medline/PubMed:cancer"))
                .hasArg()
                .argName("FETCHER:QUERY")
                .build());

        options.addOption(Option
                .builder("a")
                .longOpt("aux")
                .desc("%s: '%s'".formatted(Localization.lang("Sublibrary from AUX to BibTeX"), "-a thesis.aux,new.bib"))
                .hasArg()
                .argName("FILE[.aux],FILE[.bib] FILE")
                .build());

        options.addOption(Option
                .builder("x")
                .longOpt("prexp")
                .desc("%s: '%s'".formatted(Localization.lang("Export preferences to a file"), "-x prefs.xml"))
                .hasArg()
                .argName("[FILE]")
                .build());

        options.addOption(Option
                .builder("p")
                .longOpt("primp")
                .desc("%s: '%s'".formatted(Localization.lang("Import preferences from a file"), "-p prefs.xml"))
                .hasArg()
                .argName("[FILE]")
                .build());

        options.addOption(Option
                .builder("d")
                .longOpt("prdef")
                .desc("%s: '%s'".formatted(Localization.lang("Reset preferences"), "-d mainFontSize,newline' or '-d all"))
                .hasArg()
                .argName("KEY1[,KEY2][,KEYn] | all")
                .build());

        options.addOption(Option
                .builder()
                .longOpt("writeXmpToPdf")
                .desc("%s: '%s'".formatted(Localization.lang("Write BibTeX as XMP metadata to PDF."), "-w pathToMyOwnPaper.pdf"))
                .hasArg()
                .argName("CITEKEY1[,CITEKEY2][,CITEKEYn] | PDF1[,PDF2][,PDFn] | all")
                .build());

        options.addOption(Option
                .builder()
                .longOpt("embedBibFileInPdf")
                .desc("%s: '%s'".formatted(Localization.lang("Embed BibTeX as attached file in PDF."), "-w pathToMyOwnPaper.pdf"))
                .hasArg()
                .argName("CITEKEY1[,CITEKEY2][,CITEKEYn] | PDF1[,PDF2][,PDFn] | all")
                .build());

        options.addOption(Option
                .builder("w")
                .longOpt("writeMetadataToPdf")
                .desc("%s: '%s'".formatted(Localization.lang("Write BibTeX to PDF (XMP and embedded)"), "-w pathToMyOwnPaper.pdf"))
                .hasArg()
                .argName("CITEKEY1[,CITEKEY2][,CITEKEYn] | PDF1[,PDF2][,PDFn] | all")
                .build());

        options.addOption(Option
                .builder("j")
                .longOpt("jumpToKey")
                .desc("%s: '%s'".formatted(Localization.lang("Jump to the entry of the given citation key."), "-j key"))
                .hasArg()
                .argName("CITATIONKEY")
                .build());

        options.addOption(Option
                .builder("cc")
                .longOpt("check-consistency")
                .desc(Localization.lang("Check consistency of BibTeX file"))
                .hasArg()
                .argName("FILE")
                .build());

        options.addOption(Option
                .builder()
                .longOpt("output-format")
                .desc(Localization.lang("Output format for consistency check (txt/csv)"))
                .hasArg()
                .argName("FORMAT")
                .build());

        options.addOption(Option
                .builder("porcelain")
                .longOpt("porcelain")
                .desc(Localization.lang("Script-friendly output"))
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

        ExporterFactory exporterFactory = ExporterFactory.create(preferences);
        List<Pair<String, String>> exportFormats = exporterFactory
                .getExporters().stream()
                .map(format -> new Pair<>(format.getName(), format.getId()))
                .toList();
        String outFormatsIntro = Localization.lang("Available export formats");
        String outFormatsList = "%s:%n%s%n".formatted(outFormatsIntro, alignStringTable(exportFormats));

        String footer = '\n' + importFormatsList + outFormatsList + "\nPlease report issues at https://github.com/JabRef/jabref/issues.";

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
