package org.jabref;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import org.jabref.cli.ArgumentProcessor;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyAuthenticator;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.net.ssl.TrustStoreManager;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.Directories;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.tinylog.configuration.Configuration;
import picocli.CommandLine;

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

        try {
            final JabRefCliPreferences preferences = JabRefCliPreferences.getInstance();
            Injector.setModelOrService(CliPreferences.class, preferences);

            Injector.setModelOrService(BuildInfo.class, new BuildInfo());

            BibEntryTypesManager entryTypesManager = preferences.getCustomEntryTypesRepository();
            Injector.setModelOrService(BibEntryTypesManager.class, entryTypesManager);

            Injector.setModelOrService(JournalAbbreviationRepository.class, JournalAbbreviationLoader.loadRepository(preferences.getJournalAbbreviationPreferences()));
            Injector.setModelOrService(ProtectedTermsLoader.class, new ProtectedTermsLoader(preferences.getProtectedTermsPreferences()));

            configureProxy(preferences.getProxyPreferences());
            configureSSL(preferences.getSSLPreferences());

            Injector.setModelOrService(FileUpdateMonitor.class, new DummyFileUpdateMonitor());

            // Process arguments
            ArgumentProcessor argumentProcessor = new ArgumentProcessor(preferences, entryTypesManager);
            CommandLine commandLine = new CommandLine(argumentProcessor);
            commandLine.execute(args);

            handleHelp(commandLine, preferences);
        } catch (Exception ex) {
            LOGGER.error("Unexpected exception", ex);
        }
    }

    private static void handleHelp(CommandLine commandLine, CliPreferences cliPreferences) {
        if (commandLine.isUsageHelpRequested()) {
            System.out.printf(ArgumentProcessor.JABREF_BANNER + "%n", new BuildInfo().version);
        }

        if (commandLine.isVersionHelpRequested()) {
            System.out.printf(ArgumentProcessor.JABREF_BANNER + "%n", new BuildInfo().version);
            System.out.println(commandLine.getUsageMessage());

            System.out.println(Localization.lang("Available import formats:"));
            System.out.println(StringUtil.alignStringTable(ArgumentProcessor.getAvailableImportFormats(cliPreferences)));

            System.out.println(Localization.lang("Available export formats:"));
            System.out.println(StringUtil.alignStringTable(ArgumentProcessor.getAvailableExportFormats(cliPreferences)));
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
        boolean isDebugEnabled = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("--debug"));

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
