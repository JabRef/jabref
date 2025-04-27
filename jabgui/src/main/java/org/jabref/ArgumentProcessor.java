package org.jabref;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.prefs.BackingStoreException;

import javafx.util.Pair;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.UiCommand;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class ArgumentProcessor {
    private static final String JABREF_BANNER = """

       &&&    &&&&&    &&&&&&&&   &&&&&&&&   &&&&&&&&& &&&&&&&&&
       &&&    &&&&&    &&&   &&&  &&&   &&&  &&&       &&&
       &&&   &&& &&&   &&&   &&&  &&&   &&&  &&&       &&&
       &&&   &&   &&   &&&&&&&    &&&&&&&&   &&&&&&&&  &&& %s
       &&&  &&&&&&&&&  &&&   &&&  &&&   &&&  &&&       &&&
       &&&  &&&   &&&  &&&   &&&  &&&   &&&  &&&       &&&
    &&&&&   &&&   &&&  &&&&&&&&   &&&   &&&  &&&&&&&&& &&&

    Staying on top of your literature since 2003 - https://www.jabref.org/
    Please report issues at https://github.com/JabRef/jabref/issues
    """;

    private static final String WRAPPED_LINE_PREFIX = ""; // If a line break is added, this prefix will be inserted at the beginning of the next line
    private static final String STRING_TABLE_DELIMITER = " : ";

    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentProcessor.class);

    public enum Mode { INITIAL_START, REMOTE_START; }

    private final Mode startupMode;
    private final GuiPreferences preferences;
    private final GuiCommandLine guiCli;
    private final CommandLine cli;

    private final List<UiCommand> uiCommands = new ArrayList<>();
    private boolean guiNeeded = true;

    public ArgumentProcessor(String[] args,
                             Mode startupMode,
                             GuiPreferences preferences) {
        this.startupMode = startupMode;
        this.preferences = preferences;
        this.guiCli = new GuiCommandLine();

        cli = new CommandLine(this.guiCli);
        cli.parseArgs(args);
    }

    public List<UiCommand> processArguments() {
        uiCommands.clear();
        guiNeeded = true;

        if ((startupMode == Mode.INITIAL_START) && cli.isVersionHelpRequested()) {
            System.out.printf(JABREF_BANNER + "%n", new BuildInfo().version);
            guiNeeded = false;
            return List.of();
        }

        if ((startupMode == Mode.INITIAL_START) && cli.isUsageHelpRequested()) {
            System.out.printf(JABREF_BANNER + "%n", new BuildInfo().version);
            System.out.println(cli.getUsageMessage());

            System.out.println(Localization.lang("Available import formats"));
            System.out.println(alignStringTable(getAvailableImportFormats(preferences)));

            guiNeeded = false;
            return List.of();
        }

        // Check if we should reset all preferences to default values:
        if (guiCli.resetPreferences) {
            resetPreferences();
        }

        if (guiCli.blank) {
            uiCommands.add(new UiCommand.BlankWorkspace());
            return uiCommands;
        }

        if (StringUtil.isBlank(guiCli.jumpToKey)) {
            uiCommands.add(new UiCommand.JumpToEntryKey(guiCli.jumpToKey));
        }

        if (guiCli.libraries != null && !guiCli.libraries.isEmpty()) {
            if (guiCli.append) {
                uiCommands.add(new UiCommand.AppendToCurrentLibrary(guiCli.libraries));
            } else {
                uiCommands.add(new UiCommand.OpenLibraries(guiCli.libraries));
            }
        }

        return uiCommands;
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
        return !guiNeeded;
    }

    public static List<Pair<String, String>> getAvailableImportFormats(CliPreferences preferences) {
        ImportFormatReader importFormatReader = new ImportFormatReader(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getCitationKeyPatternPreferences(),
                new DummyFileUpdateMonitor()
        );
        return importFormatReader
                .getImportFormats().stream()
                .map(format -> new Pair<>(format.getName(), format.getId()))
                .toList();
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
