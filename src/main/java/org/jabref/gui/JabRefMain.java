package org.jabref.gui;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

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
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.net.ssl.TrustStoreManager;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.client.RemoteClient;
import org.jabref.logic.util.BuildInfo;
import org.jabref.migrations.PreferencesMigrations;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;

import net.harawata.appdirs.AppDirsFactory;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinylog.configuration.Configuration;

/**
 * JabRef's main class to process command line options and to start the UI
 */
public class JabRefMain extends Application {
    private static Logger LOGGER;

    private static String[] arguments;

    public static void main(String[] args) {
        addLogToDisk();
        arguments = args;
        launch(arguments);
    }

    private static void initializeLogger() {
         LOGGER = LoggerFactory.getLogger(JabRefMain.class);
    }

    /**
     * This needs to be called as early as possible. After the first log write, it is not possible to alter
     * the log configuration programmatically anymore.
     */
    private static void addLogToDisk() {
        Path directory = Path.of(AppDirsFactory.getInstance().getUserLogDir(
                                     "jabref",
                                     new BuildInfo().version.toString(),
                                     "org.jabref"));
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            initializeLogger();
            LOGGER.error("Could not create log directory {}", directory, e);
            return;
        }
        // The "Shared File Writer" is explained at https://tinylog.org/v2/configuration/#shared-file-writer
        Map<String, String> configuration = Map.of(
                "writerFile", "shared file",
                "writerFile.level", "info",
                "writerFile.file", directory.resolve("log.txt").toString(),
                "writerFile.charset", "UTF-8");
        Configuration.replace(configuration);
        initializeLogger();
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

            configureSSL(preferences.getSSLPreferences());

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

    private static void configureSSL(SSLPreferences sslPreferences) {
        TrustStoreManager.createTruststoreFileIfNotExist(Path.of(sslPreferences.getTruststorePath()));
        System.setProperty("javax.net.ssl.trustStore", sslPreferences.getTruststorePath());
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    private static void clearOldSearchIndices() {
        Path currentIndexPath = BibDatabaseContext.getFulltextIndexBasePath();
        Path appData = currentIndexPath.getParent();

        try {
            Files.createDirectories(currentIndexPath);
        } catch (IOException e) {
            LOGGER.error("Could not create index directory {}", appData, e);
        }

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
