package org.jabref.gui;

import java.net.Authenticator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import org.jabref.cli.ArgumentProcessor;
import org.jabref.cli.JabRefCLI;
import org.jabref.gui.remote.JabRefMessageHandler;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyAuthenticator;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.client.RemoteClient;
import org.jabref.logic.util.OS;
import org.jabref.migrations.PreferencesMigrations;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JabRef's main class to process command line options and to start the UI
 */
public class JabRefMain extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefMain.class);

    private static String[] arguments;

    public static void main(String[] args) {
        arguments = args;
        launch(arguments);
    }

    @Override
    public void start(Stage mainStage) {
        try {
            FallbackExceptionHandler.installExceptionHandler();

            // Init preferences
            final JabRefPreferences preferences = JabRefPreferences.getInstance();
            Globals.prefs = preferences;
            // Perform migrations
            PreferencesMigrations.runMigrations();

            configureProxy(preferences.getProxyPreferences());

            Globals.startBackgroundTasks();

            applyPreferences(preferences);

            try {
                // Process arguments
                ArgumentProcessor argumentProcessor = new ArgumentProcessor(arguments, ArgumentProcessor.Mode.INITIAL_START);
                // Check for running JabRef
                if (!handleMultipleAppInstances(arguments) || argumentProcessor.shouldShutDown()) {
                    Platform.exit();
                    return;
                }

                // If not, start GUI
                new JabRefGUI(mainStage, argumentProcessor.getParserResults(), argumentProcessor.isBlank());
            } catch (ParseException e) {
                LOGGER.error("Problem parsing arguments", e);

                JabRefCLI.printUsage();
                Platform.exit();
            }
        } catch (Exception ex) {
            LOGGER.error("Unexpected exception", ex);
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        Globals.stopBackgroundTasks();
        Globals.shutdownThreadPools();
    }

    private static boolean handleMultipleAppInstances(String[] args) {
        RemotePreferences remotePreferences = Globals.prefs.getRemotePreferences();
        if (remotePreferences.useRemoteServer()) {
            // Try to contact already running JabRef
            RemoteClient remoteClient = new RemoteClient(remotePreferences.getPort());
            if (remoteClient.ping()) {
                // We are not alone, there is already a server out there, send command line arguments to other instance
                if (remoteClient.sendCommandLineArguments(args)) {
                    // So we assume it's all taken care of, and quit.
                    LOGGER.info(Localization.lang("Arguments passed on to running JabRef instance. Shutting down."));
                    return false;
                } else {
                    LOGGER.warn("Could not communicate with other running JabRef instance.");
                }
            } else {
                // We are alone, so we start the server
                Globals.REMOTE_LISTENER.openAndStart(new JabRefMessageHandler(), remotePreferences.getPort());
            }
        }
        return true;
    }

    private static void applyPreferences(PreferencesService preferences) {
        // Read list(s) of journal names and abbreviations
        Globals.journalAbbreviationRepository = JournalAbbreviationLoader.loadRepository(preferences.getJournalAbbreviationPreferences());

        // Build list of Import and Export formats
        Globals.IMPORT_FORMAT_READER.resetImportFormats(preferences.getImportFormatPreferences(),
                preferences.getXmpPreferences(), Globals.getFileUpdateMonitor());
        Globals.entryTypesManager.addCustomOrModifiedTypes(preferences.getBibEntryTypes(BibDatabaseMode.BIBTEX),
                preferences.getBibEntryTypes(BibDatabaseMode.BIBLATEX));
        Globals.exportFactory = ExporterFactory.create(
                preferences.getCustomExportFormats(Globals.journalAbbreviationRepository),
                preferences.getLayoutFormatterPreferences(Globals.journalAbbreviationRepository),
                preferences.getSavePreferencesForExport(),
                preferences.getXmpPreferences());

        // Initialize protected terms loader
        Globals.protectedTermsLoader = new ProtectedTermsLoader(preferences.getProtectedTermsPreferences());

        // Override used newline character with the one stored in the preferences
        // The preferences return the system newline character sequence as default
        OS.NEWLINE = preferences.getNewLineSeparator().toString();
    }

    private static void configureProxy(ProxyPreferences proxyPreferences) {
        ProxyRegisterer.register(proxyPreferences);
        if (proxyPreferences.isUseProxy() && proxyPreferences.isUseAuthentication()) {
            Authenticator.setDefault(new ProxyAuthenticator());
        }
    }
}
