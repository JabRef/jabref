package org.jabref;

import java.net.Authenticator;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import org.jabref.cli.ArgumentProcessor;
import org.jabref.gui.remote.JabRefMessageHandler;
import org.jabref.logic.exporter.ExportFormat;
import org.jabref.logic.exporter.ExportFormats;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.net.ProxyAuthenticator;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.client.RemoteListenerClient;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.JavaVersionCheck;
import org.jabref.logic.util.OS;
import org.jabref.migrations.PreferencesMigrations;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JabRef MainClass
 */
public class JabRefMain extends Application {

    private static final Log LOGGER = LogFactory.getLog(JabRefMain.class);
    private static String[] arguments;

    public static void main(String[] args) {
        arguments = args;
        launch(arguments);
    }

    @Override
    public void start(Stage mainStage) throws Exception {
        Platform.setImplicitExit(false);
        SwingUtilities.invokeLater(() -> start(arguments));
    }

    private static void start(String[] args) {
        FallbackExceptionHandler.installExceptionHandler();

        JabRefPreferences preferences = JabRefPreferences.getInstance();

        ProxyPreferences proxyPreferences = preferences.getProxyPreferences();
        ProxyRegisterer.register(proxyPreferences);
        if (proxyPreferences.isUseProxy() && proxyPreferences.isUseAuthentication()) {
            Authenticator.setDefault(new ProxyAuthenticator());
        }

        Globals.prefs = preferences;

        // Check if we are running an acceptable version of Java
        final BuildInfo buildInfo = Globals.BUILD_INFO;
        if (!buildInfo.isAcceptableJavaVersion()) {
            StringBuilder versionError = new StringBuilder(
                    Localization.lang("Your current Java version (%0) is not supported. Please install version %1 or higher.",
                    JavaVersionCheck.getJavaVersion(),
                    buildInfo.getMinRequiredJavaVersion()
                    )
            );
            if (!buildInfo.isAllowJava9()) {
                versionError.append("\n");
                versionError.append(Localization.lang("Note that currently, JabRef does not run with Java 9."));
            }
            final JFrame frame = new JFrame();
            JOptionPane.showMessageDialog(frame, versionError, Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
            frame.dispose();
            System.exit(0);
        }
        Globals.startBackgroundTasks();

        // Note that the language was already set during the initialization of the preferences and it is safe to
        // call the next function.
        Globals.prefs.setLanguageDependentDefaultValues();

        // Perform Migrations
        // Perform checks and changes for users with a preference set from an older JabRef version.
        PreferencesMigrations.upgradePrefsToOrgJabRef();
        PreferencesMigrations.upgradeSortOrder();
        PreferencesMigrations.upgradeFaultyEncodingStrings();
        PreferencesMigrations.upgradeLabelPatternToBibtexKeyPattern();
        PreferencesMigrations.upgradeImportFileAndDirePatterns();
        PreferencesMigrations.upgradeStoredCustomEntryTypes();
        PreferencesMigrations.upgradeKeyBindingsToJavaFX();
        PreferencesMigrations.addCrossRefRelatedFieldsForAutoComplete();

        // Update handling of special fields based on preferences
        InternalBibtexFields
                .updateSpecialFields(Globals.prefs.getBoolean(JabRefPreferences.SERIALIZESPECIALFIELDS));
        // Update name of the time stamp field based on preferences
        InternalBibtexFields.updateTimeStampField(Globals.prefs.getTimestampPreferences().getTimestampField());
        // Update which fields should be treated as numeric, based on preferences:
        InternalBibtexFields.setNumericFields(Globals.prefs.getStringList(JabRefPreferences.NUMERIC_FIELDS));

        // Read list(s) of journal names and abbreviations
        Globals.journalAbbreviationLoader = new JournalAbbreviationLoader();

        /* Build list of Import and Export formats */
        Globals.IMPORT_FORMAT_READER.resetImportFormats(Globals.prefs.getImportFormatPreferences(),
                Globals.prefs.getXMPPreferences());
        EntryTypes.loadCustomEntryTypes(preferences.loadCustomEntryTypes(BibDatabaseMode.BIBTEX),
                preferences.loadCustomEntryTypes(BibDatabaseMode.BIBLATEX));
        Map<String, ExportFormat> customFormats = Globals.prefs.customExports.getCustomExportFormats(Globals.prefs,
                Globals.journalAbbreviationLoader);
        LayoutFormatterPreferences layoutPreferences = Globals.prefs
                .getLayoutFormatterPreferences(Globals.journalAbbreviationLoader);
        SavePreferences savePreferences = SavePreferences.loadForExportFromPreferences(Globals.prefs);
        ExportFormats.initAllExports(customFormats, layoutPreferences, savePreferences);

        // Initialize protected terms loader
        Globals.protectedTermsLoader = new ProtectedTermsLoader(Globals.prefs.getProtectedTermsPreferences());
        ProtectTermsFormatter.setProtectedTermsLoader(Globals.protectedTermsLoader);

        // Check for running JabRef
        RemotePreferences remotePreferences = Globals.prefs.getRemotePreferences();
        if (remotePreferences.useRemoteServer()) {
            Globals.REMOTE_LISTENER.open(new JabRefMessageHandler(), remotePreferences.getPort());

            if (!Globals.REMOTE_LISTENER.isOpen()) {
                // we are not alone, there is already a server out there, try to contact already running JabRef:
                if (RemoteListenerClient.sendToActiveJabRefInstance(args, remotePreferences.getPort())) {
                    // We have successfully sent our command line options through the socket to another JabRef instance.
                    // So we assume it's all taken care of, and quit.
                    LOGGER.info(Localization.lang("Arguments passed on to running JabRef instance. Shutting down."));
                    Globals.shutdownThreadPools();
                    // needed to tell JavaFx to stop
                    Platform.exit();
                    return;
                }
            }
            // we are alone, we start the server
            Globals.REMOTE_LISTENER.start();
        }

        // override used newline character with the one stored in the preferences
        // The preferences return the system newline character sequence as default
        OS.NEWLINE = Globals.prefs.get(JabRefPreferences.NEWLINE);

        // Process arguments
        ArgumentProcessor argumentProcessor = new ArgumentProcessor(args, ArgumentProcessor.Mode.INITIAL_START);

        // See if we should shut down now
        if (argumentProcessor.shouldShutDown()) {
            Globals.shutdownThreadPools();
            Platform.exit();
            return;
        }

        // If not, start GUI
        SwingUtilities
                .invokeLater(() -> new JabRefGUI(argumentProcessor.getParserResults(),
                        argumentProcessor.isBlank()));
    }
}
