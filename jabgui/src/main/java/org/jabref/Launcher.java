package org.jabref;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jabref.cli.ArgumentProcessor;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.JabRefGuiPreferences;
import org.jabref.logic.UiCommand;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.net.ProxyAuthenticator;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.net.ssl.TrustStoreManager;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.client.RemoteClient;
import org.jabref.logic.search.PostgreServer;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.Directories;
import org.jabref.migrations.PreferencesMigrations;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.tinylog.Level;
import org.tinylog.configuration.Configuration;

/// The main entry point for the JabRef application.
///
/// It has two main functions:
///
/// - Handle the command line arguments
/// - Start the JavaFX application
public class Launcher {
    private static Logger LOGGER;

    public enum MultipleInstanceAction {
        CONTINUE,
        SHUTDOWN,
        FOCUS
    }

    public static void main(String[] args) {
        try {
            initLogging(args);

            Injector.setModelOrService(BuildInfo.class, new BuildInfo());

            final JabRefGuiPreferences preferences = JabRefGuiPreferences.getInstance();

            ArgumentProcessor argumentProcessor = new ArgumentProcessor(
                    args,
                    ArgumentProcessor.Mode.INITIAL_START,
                    preferences);

            if (!argumentProcessor.getGuiCli().usageHelpRequested) {
                Injector.setModelOrService(CliPreferences.class, preferences);
                Injector.setModelOrService(GuiPreferences.class, preferences);

                // Early exit in case another instance is already running
                MultipleInstanceAction instanceAction = handleMultipleAppInstances(args, preferences.getRemotePreferences());
                if (instanceAction == MultipleInstanceAction.SHUTDOWN) {
                    systemExit();
                } else if (instanceAction == MultipleInstanceAction.FOCUS) {
                    // Send focus command to running instance
                    RemotePreferences remotePreferences = preferences.getRemotePreferences();
                    RemoteClient remoteClient = new RemoteClient(remotePreferences.getPort());
                    remoteClient.sendFocus();
                    systemExit();
                }

                configureProxy(preferences.getProxyPreferences());
                configureSSL(preferences.getSSLPreferences());
            }

            List<UiCommand> uiCommands = argumentProcessor.processArguments();
            if (argumentProcessor.shouldShutDown()) {
                systemExit();
            }

            PreferencesMigrations.runMigrations(preferences);

            PostgreServer postgreServer = new PostgreServer();
            Injector.setModelOrService(PostgreServer.class, postgreServer);

            CSLStyleLoader.loadInternalStyles();

            JabRefGUI.setup(uiCommands, preferences);
            JabRefGUI.launch(JabRefGUI.class, args);
        } catch (Throwable throwable) {
            LOGGER.error("Could not launch JabRef", throwable);
            throw throwable;
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
        // argument parsing workflow to parse logging options e.g. --debug
        Level logLevel = Arrays.stream(args).anyMatch("--debug"::equalsIgnoreCase)
                         ? Level.DEBUG
                         : Level.INFO;

        // addLogToDisk
        // We cannot use `Injector.instantiateModelOrService(BuildInfo.class).version` here, because this initializes logging
        Path directory = Directories.getLogDirectory(new BuildInfo().version);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            LOGGER = LoggerFactory.getLogger(Launcher.class);
            LOGGER.error("Could not create log directory {}", directory, e);
            return;
        }

        // The "Shared File Writer" is explained at
        // https://tinylog.org/v2/configuration/#shared-file-writer
        Configuration.set("level", logLevel.name().toLowerCase());
        Configuration.set("writerFile", "rolling file");
        Configuration.set("writerFile.level", logLevel.name().toLowerCase());
        // We need to manually join the path, because ".resolve" does not work on Windows,
        // because ":" is not allowed in file names on Windows
        // Idea is to have a clean console, but to have the log file ready to be sent to maintainers for debug
        Configuration.set("writerFile.file", directory + File.separator + "log_{date:yyyy-MM-dd_HH-mm-ss}.txt");
        Configuration.set("writerFile.charset", "UTF-8");
        Configuration.set("writerFile.policies", "startup");
        Configuration.set("writerFile.backups", "30");

        LOGGER = LoggerFactory.getLogger(Launcher.class);
    }

    private static void systemExit() {
        LOGGER.debug("JabRef shut down after processing command line arguments");
        // A clean shutdown takes 60s time
        // We don't need the clean shutdown here
        System.exit(0);
    }

    /**
     * @return MultipleInstanceAction: CONTINUE if JabRef should continue starting up, SHUTDOWN if it should quit, FOCUS if it should focus the existing instance.
     */
    private static MultipleInstanceAction handleMultipleAppInstances(String[] args, RemotePreferences remotePreferences) {
        LOGGER.trace("Checking for remote handling...");

        if (remotePreferences.useRemoteServer()) {
            // Try to contact already running JabRef
            RemoteClient remoteClient = new RemoteClient(remotePreferences.getPort());
            if (remoteClient.ping()) {
                LOGGER.debug("Pinging other instance succeeded.");
                if (args.length == 0) {
                    // There is already a server out there, avoid showing log "Passing arguments" while no arguments are provided.
                    LOGGER.warn("A JabRef instance is already running. Switching to that instance.");
                    return MultipleInstanceAction.FOCUS;
                } else {
                    // We are not alone, there is already a server out there, send command line arguments to other instance
                    LOGGER.debug("Passing arguments passed on to running JabRef...");
                    if (remoteClient.sendCommandLineArguments(args)) {
                        // So we assume it's all taken care of, and quit.
                        // Output to both to the log and the screen. Therefore, we do not have an additional System.out.println.
                        LOGGER.info("Arguments passed on to running JabRef instance. Shutting down.");
                        return MultipleInstanceAction.SHUTDOWN;
                    } else {
                        LOGGER.warn("Could not communicate with other running JabRef instance.");
                    }
                }
                // We do not launch a new instance in presence if there is another instance running
                return MultipleInstanceAction.SHUTDOWN;
            } else {
                LOGGER.debug("Could not ping JabRef instance.");
            }
        }
        return MultipleInstanceAction.CONTINUE;
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
}
