package org.jabref.gui;

import java.net.Authenticator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
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
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.JavaVersion;
import org.jabref.logic.util.OS;
import org.jabref.migrations.PreferencesMigrations;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.JabRefPreferences;

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
            // Fail on unsupported Java versions
            ensureCorrectJavaVersion();
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

    /**
     * Tests if we are running an acceptable Java and terminates JabRef when we are sure the version is not supported.
     * This test uses the requirements for the Java version as specified in <code>gradle.build</code>. It is possible to
     * define a minimum version including the built number and to indicate whether Java 9 can be used (which it currently
     * can't). It tries to compare this version number to the version of the currently running JVM. The check is
     * optimistic and will rather return true even if we could not exactly determine the version.
     * <p>
     * Note: Users with a very old version like 1.6 will not profit from this since class versions are incompatible and
     * JabRef won't even start. Currently, JabRef won't start with Java 9 either, but the warning that it cannot be used
     * with this version is helpful anyway to prevent users to update from an old 1.8 directly to version 9. Additionally,
     * we soon might have a JabRef that does start with Java 9 but is not perfectly compatible. Therefore, we should leave
     * the Java 9 check alive.
     */
    private static void ensureCorrectJavaVersion() {
        // Check if we are running an acceptable version of Java
        final BuildInfo buildInfo = Globals.BUILD_INFO;
        JavaVersion checker = new JavaVersion();
        final boolean java9Fail = !buildInfo.allowJava9 && checker.isJava9();
        final boolean versionFail = !checker.isAtLeast(buildInfo.minRequiredJavaVersion);

        if (java9Fail || versionFail) {
            StringBuilder versionError = new StringBuilder(
                    Localization.lang("Your current Java version (%0) is not supported. Please install version %1 or higher.",
                            checker.getJavaVersion(),
                            buildInfo.minRequiredJavaVersion));

            versionError.append("\n");
            versionError.append(Localization.lang("Your Java Runtime Environment is located at %0.", checker.getJavaInstallationDirectory()));

            if (!buildInfo.allowJava9) {
                versionError.append("\n");
                versionError.append(Localization.lang("Note that currently, JabRef does not run with Java 9."));
            }

            FXDialog alert = new FXDialog(Alert.AlertType.ERROR, Localization.lang("Error"), true);
            alert.setHeaderText(null);
            alert.setContentText(versionError.toString());

            // We exit on Java 9 error since this will definitely not work
            if (java9Fail) {
                System.exit(0);
            }
        }
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

    private static void applyPreferences(JabRefPreferences preferences) {
        // Read list(s) of journal names and abbreviations
        Globals.journalAbbreviationRepository = JournalAbbreviationLoader.loadRepository(Globals.prefs.getJournalAbbreviationPreferences());

        // Build list of Import and Export formats
        Globals.IMPORT_FORMAT_READER.resetImportFormats(Globals.prefs.getImportFormatPreferences(),
                Globals.prefs.getXmpPreferences(), Globals.getFileUpdateMonitor());
        Globals.entryTypesManager.addCustomOrModifiedTypes(preferences.loadBibEntryTypes(BibDatabaseMode.BIBTEX),
                preferences.loadBibEntryTypes(BibDatabaseMode.BIBLATEX));
        Globals.exportFactory = ExporterFactory.create(
                Globals.prefs.getCustomExportFormats(Globals.journalAbbreviationRepository),
                Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationRepository),
                Globals.prefs.getSavePreferencesForExport(),
                Globals.prefs.getXmpPreferences());

        // Initialize protected terms loader
        Globals.protectedTermsLoader = new ProtectedTermsLoader(Globals.prefs.getProtectedTermsPreferences());

        // Override used newline character with the one stored in the preferences
        // The preferences return the system newline character sequence as default
        OS.NEWLINE = Globals.prefs.get(JabRefPreferences.NEWLINE);
    }

    private static void configureProxy(ProxyPreferences proxyPreferences) {
        ProxyRegisterer.register(proxyPreferences);
        if (proxyPreferences.isUseProxy() && proxyPreferences.isUseAuthentication()) {
            Authenticator.setDefault(new ProxyAuthenticator());
        }
    }
}
