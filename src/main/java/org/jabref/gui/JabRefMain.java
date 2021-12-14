package org.jabref.gui;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

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
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.client.RemoteClient;
import org.jabref.migrations.PreferencesMigrations;
import org.jabref.model.database.BibDatabaseContext;
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
        URLDownload.bypassSSLVerification();
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

            clearOldSearchIndices();

            try {
                // Process arguments
                ArgumentProcessor argumentProcessor = new ArgumentProcessor(arguments, ArgumentProcessor.Mode.INITIAL_START, preferences);
                // Check for running JabRef
                if (!handleMultipleAppInstances(arguments, preferences) || argumentProcessor.shouldShutDown()) {
                    Platform.exit();
                    return;
                }

                // If not, start GUI
                new JabRefGUI(mainStage, argumentProcessor.getParserResults(), argumentProcessor.isBlank(), preferences);
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

    private static boolean handleMultipleAppInstances(String[] args, PreferencesService preferences) {
        RemotePreferences remotePreferences = preferences.getRemotePreferences();
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
                Globals.REMOTE_LISTENER.openAndStart(new JabRefMessageHandler(), remotePreferences.getPort(), preferences);
            }
        }
        return true;
    }

    private static void applyPreferences(PreferencesService preferences) {
        // Read list(s) of journal names and abbreviations
        Globals.journalAbbreviationRepository = JournalAbbreviationLoader.loadRepository(preferences.getJournalAbbreviationPreferences());

        // Build list of Import and Export formats
        Globals.IMPORT_FORMAT_READER.resetImportFormats(preferences.getImporterPreferences(),
                preferences.getGeneralPreferences(), preferences.getImportFormatPreferences(),
                preferences.getXmpPreferences(), Globals.getFileUpdateMonitor());
        Globals.entryTypesManager.addCustomOrModifiedTypes(preferences.getBibEntryTypes(BibDatabaseMode.BIBTEX),
                preferences.getBibEntryTypes(BibDatabaseMode.BIBLATEX));
        Globals.exportFactory = ExporterFactory.create(
                preferences.getCustomExportFormats(Globals.journalAbbreviationRepository),
                preferences.getLayoutFormatterPreferences(Globals.journalAbbreviationRepository),
                preferences.getSavePreferencesForExport(),
                preferences.getXmpPreferences(),
                preferences.getGeneralPreferences().getDefaultBibDatabaseMode(),
                Globals.entryTypesManager);

        // Initialize protected terms loader
        Globals.protectedTermsLoader = new ProtectedTermsLoader(preferences.getProtectedTermsPreferences());
    }

    private static void configureProxy(ProxyPreferences proxyPreferences) {
        ProxyRegisterer.register(proxyPreferences);
        if (proxyPreferences.shouldUseProxy() && proxyPreferences.shouldUseAuthentication()) {
            Authenticator.setDefault(new ProxyAuthenticator());
        }
    }

    private static void clearOldSearchIndices() {
        Path currentIndexPath = BibDatabaseContext.getFulltextIndexBasePath();
        Path appData = currentIndexPath.getParent();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(appData)) {
            for (Path path : stream) {
                if (Files.isDirectory(path) && !path.equals(currentIndexPath)) {
                    LOGGER.info("Deleting out-of-date fulltext search index at {}.", path);
                    Files.walk(path)
                         .sorted(Comparator.reverseOrder())
                         .map(Path::toFile)
                         .forEach(File::delete);

                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not access app-directory at {}", appData, e);
        }
    }
}
