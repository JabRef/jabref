package org.jabref;

import org.jabref.cli.JabKit;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.JabRefGuiPreferences;
import org.jabref.logic.preferences.CliPreferences;
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
        JabKit.Result result = JabKit.processArguments(args);
        // The method `processArguments` quites the whole JVM if no GUI is needed.

        // Initialize preferences
        final JabRefGuiPreferences preferences = JabRefGuiPreferences.getInstance();
        Injector.setModelOrService(CliPreferences.class, preferences);
        Injector.setModelOrService(GuiPreferences.class, preferences);

        PreferencesMigrations.runMigrations(preferences);

        JabRefGUI.setup(result.uiCommands(), preferences, result.fileUpdateMonitor());
        JabRefGUI.launch(JabRefGUI.class, args);
    }
}
