package net.sf.jabref;

import java.net.Authenticator;
import java.util.Map;

import javax.swing.SwingUtilities;

import net.sf.jabref.cli.ArgumentProcessor;
import net.sf.jabref.gui.remote.JabRefMessageHandler;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.logic.exporter.ExportFormat;
import net.sf.jabref.logic.exporter.ExportFormats;
import net.sf.jabref.logic.exporter.SavePreferences;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.logic.net.ProxyAuthenticator;
import net.sf.jabref.logic.net.ProxyPreferences;
import net.sf.jabref.logic.net.ProxyRegisterer;
import net.sf.jabref.logic.protectedterms.ProtectedTermsLoader;
import net.sf.jabref.logic.remote.RemotePreferences;
import net.sf.jabref.logic.remote.client.RemoteListenerClient;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.migrations.PreferencesMigrations;
import net.sf.jabref.model.entry.InternalBibtexFields;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JabRef MainClass
 */
public class JabRefMain {
    private static final Log LOGGER = LogFactory.getLog(JabRefMain.class);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> start(args));
    }

    private static void start(String[] args) {
        FallbackExceptionHandler.installExceptionHandler();

        JabRefPreferences preferences = JabRefPreferences.getInstance();

        ProxyPreferences proxyPreferences = preferences.getProxyPreferences();
        ProxyRegisterer.register(proxyPreferences);
        if (proxyPreferences.isUseProxy() && proxyPreferences.isUseAuthentication()) {
            Authenticator.setDefault(new ProxyAuthenticator());
        }

        Globals.startBackgroundTasks();
        Globals.prefs = preferences;
        Localization.setLanguage(preferences.get(JabRefPreferences.LANGUAGE));
        Globals.prefs.setLanguageDependentDefaultValues();

        // Perform Migrations
        // Perform checks and changes for users with a preference set from an older JabRef version.
        PreferencesMigrations.upgradeSortOrder();
        PreferencesMigrations.upgradeFaultyEncodingStrings();
        PreferencesMigrations.upgradeLabelPatternToBibtexKeyPattern();

        // Update handling of special fields based on preferences
        InternalBibtexFields
                .updateSpecialFields(Globals.prefs.getBoolean(JabRefPreferences.SERIALIZESPECIALFIELDS));
        // Update name of the time stamp field based on preferences
        InternalBibtexFields.updateTimeStampField(Globals.prefs.get(JabRefPreferences.TIME_STAMP_FIELD));
        // Update which fields should be treated as numeric, based on preferences:
        InternalBibtexFields.setNumericFields(Globals.prefs.getStringList(JabRefPreferences.NUMERIC_FIELDS));

        // Read list(s) of journal names and abbreviations
        Globals.journalAbbreviationLoader = new JournalAbbreviationLoader();

        /* Build list of Import and Export formats */
        Globals.IMPORT_FORMAT_READER.resetImportFormats(Globals.prefs.getImportFormatPreferences(),
                Globals.prefs.getXMPPreferences());
        CustomEntryTypesManager.loadCustomEntryTypes(preferences);
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
                    JabRefExecutorService.INSTANCE.shutdownEverything();
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
            JabRefExecutorService.INSTANCE.shutdownEverything();
            return;
        }

        // If not, start GUI
        SwingUtilities
                .invokeLater(() -> new JabRefGUI(argumentProcessor.getParserResults(),
                        argumentProcessor.isBlank()));
    }
}
