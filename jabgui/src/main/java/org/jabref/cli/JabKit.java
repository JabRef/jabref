package org.jabref.cli;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jabref.logic.UiCommand;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.net.ProxyAuthenticator;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.net.ssl.TrustStoreManager;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.client.RemoteClient;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.Directories;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.tinylog.configuration.Configuration;

/// Entrypoint for a command-line only version of JabRef.
/// It does not open any dialogs, just parses the command line arguments and outputs text and creates/modifies files.
///
/// See [Command Line Interface Guidelines](https://clig.dev/) for general guidelines how to design a good CLI interface.
///
/// It does not open any GUI.
/// For the GUI application see {@link org.jabref.Launcher}.
///
/// Does not do any preference migrations.
public class JabKit {
    private static Logger LOGGER;

    public static void main(String[] args) {
        initLogging(args);

        final JabRefCliPreferences preferences = JabRefCliPreferences.getInstance();
        Injector.setModelOrService(CliPreferences.class, preferences);

        FileUpdateMonitor fileUpdateMonitor = new DummyFileUpdateMonitor();

        List<UiCommand> uiCommands = processArguments(args, preferences, fileUpdateMonitor);
        if (!uiCommands.isEmpty()) {
            LOGGER.error("No GUI needed, but UI commands were returned. Exiting.");
        }
    }

    private static void systemExit() {
        LOGGER.debug("JabRef shut down after processing command line arguments");
        // A clean shutdown takes 60s time
        // We don't need the clean shutdown here
        System.exit(0);
    }

    public static List<UiCommand> processArguments(String[] args, JabRefCliPreferences preferences, FileUpdateMonitor fileUpdateMonitor) {
        try {
            Injector.setModelOrService(BuildInfo.class, new BuildInfo());

            // Early exit in case another instance is already running
            if (!handleMultipleAppInstances(args, preferences.getRemotePreferences())) {
                systemExit();
            }

            BibEntryTypesManager entryTypesManager = preferences.getCustomEntryTypesRepository();
            Injector.setModelOrService(BibEntryTypesManager.class, entryTypesManager);

            Injector.setModelOrService(JournalAbbreviationRepository.class, JournalAbbreviationLoader.loadRepository(preferences.getJournalAbbreviationPreferences()));
            Injector.setModelOrService(ProtectedTermsLoader.class, new ProtectedTermsLoader(preferences.getProtectedTermsPreferences()));

            configureProxy(preferences.getProxyPreferences());
            configureSSL(preferences.getSSLPreferences());

            clearOldSearchIndices();

            try {
                Injector.setModelOrService(FileUpdateMonitor.class, fileUpdateMonitor);

                // Process arguments
                ArgumentProcessor argumentProcessor = new ArgumentProcessor(
                        args,
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
                    return null;
                }

                return new ArrayList<>(argumentProcessor.getUiCommands());
            } catch (ParseException e) {
                LOGGER.error("Problem parsing arguments", e);
                CliOptions.printUsage(preferences);
                systemExit();
                return null;
            }
        } catch (Exception ex) {
            LOGGER.error("Unexpected exception", ex);
            systemExit();
            return null;
        }
    }

    /**
     * This needs to be called as early as possible. After the first log write, it
     * is not possible to alter the log configuration programmatically anymore.
     */
    public static void initLogging(String[] args) {
        // routeLoggingToSlf4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // We must configure logging as soon as possible, which is why we cannot wait for the usual
        // argument parsing workflow to parse logging options .e.g. --debug
        boolean isDebugEnabled;
        try {
            CliOptions cliOptions = new CliOptions(args);
            isDebugEnabled = cliOptions.isDebugLogging();
        } catch (ParseException e) {
            isDebugEnabled = false;
        }

        // addLogToDisk
        // We cannot use `Injector.instantiateModelOrService(BuildInfo.class).version` here, because this initializes logging
        Path directory = Directories.getLogDirectory(new BuildInfo().version);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            LOGGER = LoggerFactory.getLogger(JabKit.class);
            LOGGER.error("Could not create log directory {}", directory, e);
            return;
        }

        // The "Shared File Writer" is explained at
        // https://tinylog.org/v2/configuration/#shared-file-writer
        Map<String, String> configuration = Map.of(
                "level", isDebugEnabled ? "debug" : "info",
                "writerFile", "rolling file",
                "writerFile.level", isDebugEnabled ? "debug" : "info",
                // We need to manually join the path, because ".resolve" does not work on Windows, because ":" is not allowed in file names on Windows
                "writerFile.file", directory + File.separator + "log_{date:yyyy-MM-dd_HH-mm-ss}.txt",
                "writerFile.charset", "UTF-8",
                "writerFile.policies", "startup",
                "writerFile.backups", "30");
        configuration.forEach(Configuration::set);

        LOGGER = LoggerFactory.getLogger(JabKit.class);
    }

    /**
     * @return true if JabRef should continue starting up, false if it should quit.
     */
    private static boolean handleMultipleAppInstances(String[] args, RemotePreferences remotePreferences) throws InterruptedException {
        LOGGER.trace("Checking for remote handling...");
        if (remotePreferences.useRemoteServer()) {
            // Try to contact already running JabRef
            RemoteClient remoteClient = new RemoteClient(remotePreferences.getPort());
            if (remoteClient.ping()) {
                LOGGER.debug("Pinging other instance succeeded.");
                if (args.length == 0) {
                    // There is already a server out there, avoid showing log "Passing arguments" while no arguments are provided.
                    LOGGER.warn("This JabRef instance is already running. Please switch to that instance.");
                } else {
                    // We are not alone, there is already a server out there, send command line arguments to other instance
                    LOGGER.debug("Passing arguments passed on to running JabRef...");
                    if (remoteClient.sendCommandLineArguments(args)) {
                        // So we assume it's all taken care of, and quit.
                        // Output to both to the log and the screen. Therefore, we do not have an additional System.out.println.
                        LOGGER.info("Arguments passed on to running JabRef instance. Shutting down.");
                    } else {
                        LOGGER.warn("Could not communicate with other running JabRef instance.");
                    }
                }
                // We do not launch a new instance in presence if there is another instance running
                return false;
            } else {
                LOGGER.debug("Could not ping JabRef instance.");
            }
        }
        return true;
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
        Path currentIndexPath = Directories.getFulltextIndexBaseDirectory();
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
