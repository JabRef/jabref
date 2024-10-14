package org.jabref;

import java.util.List;

import org.jabref.cli.JabRefCli;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.JabRefGuiPreferences;
import org.jabref.gui.util.DefaultFileUpdateMonitor;
import org.jabref.logic.UiCommand;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.migrations.PreferencesMigrations;

import com.airhacks.afterburner.injection.Injector;

/// The main entry point for the JabRef application.
///
/// It has two main functions:
///
/// - Handle the command line arguments
/// - Start the JavaFX application (if not in CLI mode)
public class Launcher {

    public static void main(String[] args) {
        JabRefCli.initLogging(args);

        // Initialize preferences
        final JabRefGuiPreferences preferences = JabRefGuiPreferences.getInstance();
        Injector.setModelOrService(CliPreferences.class, preferences);
        Injector.setModelOrService(GuiPreferences.class, preferences);

        DefaultFileUpdateMonitor fileUpdateMonitor = new DefaultFileUpdateMonitor();
        HeadlessExecutorService.INSTANCE.executeInterruptableTask(fileUpdateMonitor, "FileUpdateMonitor");

        List<UiCommand> uiCommands = JabRefCli.processArguments(args, preferences, fileUpdateMonitor);
        // The method `processArguments` quites the whole JVM if no GUI is needed.

        PreferencesMigrations.runMigrations(preferences);

        JabRefGUI.setup(uiCommands, preferences, fileUpdateMonitor);
        JabRefGUI.launch(JabRefGUI.class, args);
    }
}
