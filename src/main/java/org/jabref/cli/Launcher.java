package org.jabref.cli;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import org.jabref.gui.Globals;
import org.jabref.gui.MainApplication;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.predatory.PredatoryJournalListLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyAuthenticator;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.net.ssl.TrustStoreManager;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.client.RemoteClient;
import org.jabref.logic.util.OS;
import org.jabref.migrations.PreferencesMigrations;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.tinylog.configuration.Configuration;

/**
 * The main entry point for the JabRef application.
 * <p>
 * It has two main functions:
 * - Handle the command line arguments
 * - Start the JavaFX application (if not in cli mode)
 */
public class Launcher {
    private static Logger LOGGER;
    private static String[] ARGUMENTS;
    private static boolean isDebugEnabled;

    public static void main(String[] args) {
        routeLoggingToSlf4J();
        ARGUMENTS = args;

        // We must configure logging as soon as possible, which is why we cannot wait for the usual
        // argument parsing workflow to parse logging options .e.g. --debug
        JabRefCLI jabRefCLI;
        try {
            jabRefCLI = new JabRefCLI(ARGUMENTS);
            isDebugEnabled = jabRefCLI.isDebugLogging();
        } catch (ParseException e) {
            isDebugEnabled = false;
        }

        addLogToDisk();
        try {
            BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();
            Globals.entryTypesManager = entryTypesManager;

            // Initialize preferences
            final JabRefPreferences preferences = JabRefPreferences.getInstance();

            // Early exit in case another instance is already running
            if (!handleMultipleAppInstances(ARGUMENTS, preferences.getRemotePreferences())) {
                return;
            }

            Globals.prefs = preferences;
            PreferencesMigrations.runMigrations(preferences, entryTypesManager);

            // Initialize rest of preferences
            configureProxy(preferences.getProxyPreferences());
            configureSSL(preferences.getSSLPreferences());
            initGlobals(preferences);
            clearOldSearchIndices();

            try {
                FileUpdateMonitor fileUpdateMonitor = Globals.getFileUpdateMonitor();

                // Process arguments
                ArgumentProcessor argumentProcessor = new ArgumentProcessor(
                        ARGUMENTS,
                        ArgumentProcessor.Mode.INITIAL_START,
                        preferences,
                        fileUpdateMonitor,
                        entryTypesManager);
                argumentProcessor.processArguments();
                if (argumentProcessor.shouldShutDown()) {
                    LOGGER.debug("JabRef shut down after processing command line arguments");
                    // A clean shutdown takes 60s time
                    // We don't need the clean shutdown here
                    System.exit(0);
                }

                MainApplication.main(argumentProcessor.getParserResults(), argumentProcessor.isBlank(), preferences, fileUpdateMonitor, ARGUMENTS);
            } catch (ParseException e) {
                LOGGER.error("Problem parsing arguments", e);
                JabRefCLI.printUsage(preferences);
            }
        } catch (Exception ex) {
            LOGGER.error("Unexpected exception", ex);
        }
    }

    private static void routeLoggingToSlf4J() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    /**
     * This needs to be called as early as possible. After the first log write, it
     * is not possible to alter
     * the log configuration programmatically anymore.
     */
    private static void addLogToDisk() {
        Path directory = OS.getNativeDesktop().getLogDirectory();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            initializeLogger();
            LOGGER.error("Could not create log directory {}", directory, e);
            return;
        }
        // The "Shared File Writer" is explained at
        // https://tinylog.org/v2/configuration/#shared-file-writer
        Map<String, String> configuration = Map.of(
                "writerFile", "shared file",
                "writerFile.level", isDebugEnabled ? "debug" : "info",
                "level", isDebugEnabled ? "debug" : "info",
                "writerFile.file", directory.resolve("log.txt").toString(),
                "writerFile.charset", "UTF-8");

        configuration.forEach(Configuration::set);
        initializeLogger();
    }

    private static void initializeLogger() {
        LOGGER = LoggerFactory.getLogger(MainApplication.class);
    }

    private static boolean handleMultipleAppInstances(String[] args, RemotePreferences remotePreferences) {
        if (remotePreferences.useRemoteServer()) {
            // Try to contact already running JabRef
            RemoteClient remoteClient = new RemoteClient(remotePreferences.getPort());
            if (remoteClient.ping()) {
                // We are not alone, there is already a server out there, send command line
                // arguments to other instance
                if (remoteClient.sendCommandLineArguments(args)) {
                    // So we assume it's all taken care of, and quit.
                    LOGGER.info(Localization.lang("Arguments passed on to running JabRef instance. Shutting down."));
                    return false;
                } else {
                    LOGGER.warn("Could not communicate with other running JabRef instance.");
                }
            }
        }
        return true;
    }

    private static void initGlobals(PreferencesService preferences) {
        // Read list(s) of journal names and abbreviations
        Globals.journalAbbreviationRepository = JournalAbbreviationLoader
                .loadRepository(preferences.getJournalAbbreviationPreferences());
        Globals.predatoryJournalRepository = PredatoryJournalListLoader
                .loadRepository();

        Globals.entryTypesManager = preferences.getCustomEntryTypesRepository();
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
    }

    private static void clearOldSearchIndices() {
        Path currentIndexPath = OS.getNativeDesktop().getFulltextIndexBaseDirectory();
        Path appData = currentIndexPath.getParent();

        try {
            Files.createDirectories(currentIndexPath);
        } catch (IOException e) {
            LOGGER.error("Could not create index directory {}", appData, e);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(appData)) {
            for (Path path : stream) {
                if (Files.isDirectory(path) && !path.toString().endsWith("ssl") && path.toString().contains("lucene")
                        && !path.equals(currentIndexPath)) {
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
