package net.sf.jabref;

import gnu.dtools.ritopt.BooleanOption;
import gnu.dtools.ritopt.Options;
import gnu.dtools.ritopt.StringOption;

public class JabRefCLI {

    public final static String exportMatchesSyntax = "[".concat(Globals.lang("field")).concat("]")
            .concat("searchTerm").concat(",").concat("outputFile").concat(": ").
                    concat(Globals.lang("file")).concat("[,").concat(Globals.lang("exportFormat")).concat("]");

    public final StringOption importFile, exportFile, exportPrefs, importPrefs, auxImExport, importToOpenBase,
            fetcherEngine, exportMatches, defPrefs;

    public final BooleanOption helpO, disableGui, blank, loadSess, showVersion, disableSplash;
    public final Options options;
    private String[] leftOver;

    public boolean isHelp() {
        return helpO.isInvoked();
    }

    public boolean isShowVersion() {
        return showVersion.isInvoked();
    }

    public boolean isDisableSplash() {
        return disableSplash.isInvoked();
    }

    public boolean isBlank() {
        return blank.isInvoked();
    }

    public boolean isLoadSession() {
        return loadSess.isInvoked();
    }

    public boolean isDisableGui() {
        return disableGui.isInvoked();
    }

    public String getHelp() {
        return options.getHelp();
    }

    public JabRefCLI(String[] args) {
        importFile = new StringOption("");
        exportFile = new StringOption("");
        helpO = new BooleanOption();
        disableGui = new BooleanOption();
        disableSplash = new BooleanOption();
        blank = new BooleanOption();
        loadSess = new BooleanOption();
        showVersion = new BooleanOption();
        exportPrefs = new StringOption("jabref_prefs.xml");
        importPrefs = new StringOption("jabref_prefs.xml");
        defPrefs = new StringOption("");
        auxImExport = new StringOption("");
        importToOpenBase = new StringOption("");
        fetcherEngine = new StringOption("");
        exportMatches = new StringOption("");

        options = new Options("JabRef ");
        options.setVersion(GUIGlobals.version);

        importFile.setDescription("imopoepuoeu"); //Globals.lang);
        options.register("version", 'v',
                Globals.lang("Display version"), showVersion);
        options.register("nogui", 'n',
                Globals.lang("No GUI. Only process command line options."), disableGui);
        options.register("nosplash", 's',
                Globals.lang("Do not show splash window at startup"), disableSplash);
        options.register("import", 'i',
                Globals.lang("Import file") + ": " + Globals.lang("filename")
                        + "[,import format]", importFile
        );
        options.register("output", 'o',
                Globals.lang("Output or export file") + ": " + Globals.lang("filename")
                        + "[,export format]", exportFile
        );
        options.register("help", 'h',
                Globals.lang("Display help on command line options"), helpO);
        options.register("loads", 'l', Globals.lang("Load session"), loadSess);
        options.register("prexp", 'x', Globals.lang("Export preferences to file"),
                exportPrefs);
        options.register("primp", 'p', Globals.lang("Import preferences from file"),
                importPrefs);
        options.register("prdef", 'd', Globals.lang("Reset preferences (key1,key2,... or 'all')"),
                defPrefs);
        options.register("aux", 'a',
                Globals.lang("Subdatabase from aux") + ": " + Globals.lang("file") + "[.aux]" + "," + Globals.lang("new") + "[.bib]",
                auxImExport);
        options.register("blank", 'b', Globals.lang("Do not open any files at startup"), blank);

        options.register("importToOpen", '\0', Globals.lang("Import to open tab"), importToOpenBase);

        options.register("fetch", 'f', Globals.lang("Run Fetcher, e.g. \"--fetch=Medline:cancer\""), fetcherEngine);

        options.register("exportMatches", 'm', exportMatchesSyntax, exportMatches);

        options.setUseMenu(false);

        parse(args);
    }

    private void parse(String[] args) {
        leftOver = options.process(args);
    }

    public String[] getLeftOver() {
        return leftOver;
    }
}
