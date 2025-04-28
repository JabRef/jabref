package org.jabref.cli;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.prefs.BackingStoreException;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.UiCommand;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.os.OS;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArgumentProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentProcessor.class);

    public enum Mode { INITIAL_START, REMOTE_START }

    private final CliOptions cli;

    private final Mode startupMode;

    private final GuiPreferences preferences;
    private final FileUpdateMonitor fileUpdateMonitor;

    private boolean guiNeeded;
    private final List<UiCommand> uiCommands = new ArrayList<>();

    /**
     * First call the constructor, then call {@link #processArguments()}.
     * Afterward, you can access the {@link #getUiCommands()}.
     */
    public ArgumentProcessor(String[] args,
                             Mode startupMode,
                             GuiPreferences preferences,
                             FileUpdateMonitor fileUpdateMonitor)
            throws ParseException {
        this.cli = new CliOptions(args);
        this.startupMode = startupMode;
        this.preferences = preferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
    }

    /**
     * Will open a file (like {@link #importFile(String)}, but will also request JabRef to focus on this library.
     *
     * @return ParserResult with setToOpenTab(true)
     */
    private Optional<ParserResult> importToOpenBase(String importArguments) {
        Optional<ParserResult> result = importFile(importArguments);
        result.ifPresent(ParserResult::setToOpenTab);
        return result;
    }

    /**
     *
     * @param importArguments Format: <code>fileName[,format]</code>
     */
    private Optional<ParserResult> importFile(String importArguments) {
        LOGGER.debug("Importing file {}", importArguments);
        String[] data = importArguments.split(",");

        String address = data[0];
        Path file;
        if (address.startsWith("http://") || address.startsWith("https://") || address.startsWith("ftp://")) {
            // Download web resource to temporary file
            try {
                file = new URLDownload(address).toTemporaryFile();
            } catch (FetcherException | MalformedURLException e) {
                System.err.println(Localization.lang("Problem downloading from %1", address) + e.getLocalizedMessage());
                return Optional.empty();
            }
        } else {
            if (OS.WINDOWS) {
                file = Path.of(address);
            } else {
                file = Path.of(address.replace("~", System.getProperty("user.home")));
            }
        }

        String importFormat;
        if (data.length > 1) {
            importFormat = data[1];
        } else {
            importFormat = "*";
        }

        Optional<ParserResult> importResult = importFile(file, importFormat);
        importResult.ifPresent(result -> {
            if (result.hasWarnings()) {
                System.out.println(result.getErrorMessage());
            }
        });
        return importResult;
    }

    private Optional<ParserResult> importFile(Path file, String importFormat) {
        try {
            ImportFormatReader importFormatReader = new ImportFormatReader(
                    preferences.getImporterPreferences(),
                    preferences.getImportFormatPreferences(),
                    preferences.getCitationKeyPatternPreferences(),
                    fileUpdateMonitor
            );

            if (!"*".equals(importFormat)) {
                System.out.println(Localization.lang("Importing %0", file));
                ParserResult result = importFormatReader.importFromFile(importFormat, file);
                return Optional.of(result);
            } else {
                // * means "guess the format":
                System.out.println(Localization.lang("Importing file %0 as unknown format", file));

                ImportFormatReader.UnknownFormatImport importResult =
                        importFormatReader.importUnknownFormat(file, new DummyFileUpdateMonitor());

                System.out.println(Localization.lang("Format used: %0", importResult.format()));
                return Optional.of(importResult.parserResult());
            }
        } catch (ImportException ex) {
            System.err.println(Localization.lang("Error opening file '%0'", file) + "\n" + ex.getLocalizedMessage());
            return Optional.empty();
        }
    }

    public void processArguments() {
        uiCommands.clear();
        guiNeeded = true;

        if ((startupMode == Mode.INITIAL_START) && cli.isShowVersion()) {
            cli.displayVersion();
            guiNeeded = false;
            return;
        }

        if ((startupMode == Mode.INITIAL_START) && cli.isHelp()) {
            CliOptions.printUsage(preferences);
            guiNeeded = false;
            return;
        }

        // Check if we should reset all preferences to default values:
        if (cli.isPreferencesReset()) {
            resetPreferences();
        }

        List<ParserResult> loaded = importAndOpenFiles();

        if (cli.isBlank()) {
            uiCommands.add(new UiCommand.BlankWorkspace());
        }

        if (!cli.isBlank() && cli.isJumpToKey()) {
            uiCommands.add(new UiCommand.JumpToEntryKey(cli.getJumpToKey()));
        }

        if (!cli.isBlank() && !loaded.isEmpty()) {
            uiCommands.add(new UiCommand.OpenDatabases(loaded));
        }

        if (cli.isBlank() && loaded.isEmpty()) {
            uiCommands.add(new UiCommand.BlankWorkspace());
        }
    }

    /**
     * @return List of opened files (could be .bib, but also other formats). May also contain error results.
     */
    private List<ParserResult> importAndOpenFiles() {
        List<ParserResult> loaded = new ArrayList<>();
        List<String> toImport = new ArrayList<>();
        if (!cli.isBlank() && (!cli.getLeftOver().isEmpty())) {
            for (String aLeftOver : cli.getLeftOver()) {
                // Leftover arguments that have a "bib" extension are interpreted as
                // BIB files to open. Other files, and files that could not be opened
                // as bib, we try to import instead.
                boolean bibExtension = aLeftOver.toLowerCase(Locale.ENGLISH).endsWith("bib");

                ParserResult pr = new ParserResult();
                if (bibExtension) {
                    try {
                        pr = OpenDatabase.loadDatabase(
                                Path.of(aLeftOver),
                                preferences.getImportFormatPreferences(),
                                fileUpdateMonitor);
                        // In contrast to org.jabref.gui.LibraryTab.onDatabaseLoadingSucceed, we do not execute OpenDatabaseAction.performPostOpenActions(result, dialogService);
                    } catch (IOException ex) {
                        pr = ParserResult.fromError(ex);
                        LOGGER.error("Error opening file '{}'", aLeftOver, ex);
                    }
                }

                if (!bibExtension || (pr.isEmpty())) {
                    // We will try to import this file. Normally we
                    // will import it into a new tab, but if this import has
                    // been initiated by another instance through the remote
                    // listener, we will instead import it into the current library.
                    // This will enable easy integration with web browsers that can
                    // open a reference file in JabRef.
                    if (startupMode == Mode.INITIAL_START) {
                        toImport.add(aLeftOver);
                    } else {
                        loaded.add(importToOpenBase(aLeftOver).orElse(new ParserResult()));
                    }
                } else {
                    loaded.add(pr);
                }
            }
        }

        if (!cli.isBlank() && cli.isFileImport()) {
            toImport.add(cli.getFileImport());
        }

        for (String filenameString : toImport) {
            importFile(filenameString).ifPresent(loaded::add);
        }

        if (!cli.isBlank() && cli.isImportToOpenBase()) {
            importToOpenBase(cli.getImportToOpenBase()).ifPresent(loaded::add);
        }

        return loaded;
    }

    private void resetPreferences() {
        try {
            System.out.println(Localization.lang("Setting all preferences to default values."));
            preferences.clear();
            new SharedDatabasePreferences().clear();
        } catch (BackingStoreException e) {
            System.err.println(Localization.lang("Unable to clear preferences."));
            LOGGER.error("Unable to clear preferences", e);
        }
    }

    public boolean shouldShutDown() {
        return cli.isShowVersion() || !guiNeeded;
    }

    public List<UiCommand> getUiCommands() {
        return uiCommands;
    }
}
