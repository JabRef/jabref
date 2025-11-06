package org.jabref;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.util.Pair;

import org.jabref.cli.ArgumentProcessor;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetcher;
import org.jabref.logic.importer.WebFetchers;
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
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.tinylog.Level;
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
    // J.U.L. bridge to SLF4J must be initialized before any logger is created, see initLogging()
    private static Logger LOGGER;

    private static final String JABKIT_BRAND = "JabKit - command line toolkit for JabRef";

    public static void main(String[] args) {
        initLogging(args);

        try {
            final JabRefCliPreferences preferences = JabRefCliPreferences.getInstance();
            Injector.setModelOrService(CliPreferences.class, preferences);

            BuildInfo buildInfo = new BuildInfo();
            Injector.setModelOrService(BuildInfo.class, buildInfo);

            BibEntryTypesManager entryTypesManager = preferences.getCustomEntryTypesRepository();
            Injector.setModelOrService(BibEntryTypesManager.class, entryTypesManager);

            ArgumentProcessor argumentProcessor = new ArgumentProcessor(preferences, entryTypesManager);
            CommandLine commandLine = new CommandLine(argumentProcessor)
                    .registerConverter(DOI.class, DOI::new);
            String usageHeader = BuildInfo.JABREF_BANNER.formatted(buildInfo.version) + "\n" + JABKIT_BRAND;
            commandLine.getCommandSpec().usageMessage().header(usageHeader);
            applyUsageFooters(commandLine,
                    ArgumentProcessor.getAvailableImportFormats(preferences),
                    ArgumentProcessor.getAvailableExportFormats(preferences),
                    WebFetchers.getSearchBasedFetchers(preferences.getImportFormatPreferences(), preferences.getImporterPreferences()));

            // Show help when no arguments are given. Placed after header and footer setup
            // to ensure output matches --help command
            if (args.length == 0) {
                commandLine.usage(System.out);
                System.exit(0);
            }

            // Heavy initialization only needed when actually executing a command
            Injector.setModelOrService(JournalAbbreviationRepository.class, JournalAbbreviationLoader.loadRepository(preferences.getJournalAbbreviationPreferences()));
            Injector.setModelOrService(ProtectedTermsLoader.class, new ProtectedTermsLoader(preferences.getProtectedTermsPreferences()));

            configureProxy(preferences.getProxyPreferences());
            configureSSL(preferences.getSSLPreferences());

            Injector.setModelOrService(FileUpdateMonitor.class, new DummyFileUpdateMonitor());

            int result = commandLine.execute(args);
            System.exit(result);
        } catch (Exception ex) {
            LOGGER.error("Unexpected exception", ex);
        }
    }

    private static void applyUsageFooters(CommandLine commandLine,
                                          List<Pair<String, String>> inputFormats,
                                          List<Pair<String, String>> outputFormats,
                                          Set<SearchBasedFetcher> fetchers) {
        String inputFooter = "\n"
                + Localization.lang("Available import formats:") + "\n"
                + StringUtil.alignStringTable(inputFormats);
        String outputFooter = "\n"
                + Localization.lang("Available export formats:") + "\n"
                + StringUtil.alignStringTable(outputFormats);

        commandLine.getSubcommands().values().forEach(subCommand -> {
            boolean hasInputOption = subCommand.getCommandSpec().options().stream()
                                               .anyMatch(opt -> Arrays.asList(opt.names()).contains("--input-format"));
            boolean hasOutputOption = subCommand.getCommandSpec().options().stream()
                                                .anyMatch(opt -> Arrays.asList(opt.names()).contains("--output-format"));

            String footerText = "";
            footerText += hasInputOption ? inputFooter : "";
            footerText += hasOutputOption ? outputFooter : "";
            subCommand.getCommandSpec().usageMessage().footer(footerText);
        });

        commandLine.getSubcommands().get("fetch")
                   .getCommandSpec().usageMessage().footer(Localization.lang("The following providers are available:") + "\n"
                           + fetchers.stream()
                                     .map(WebFetcher::getName)
                                     .filter(name -> !"Search pre-configured".equals(name))
                                     .collect(Collectors.joining(", ")));
    }

    /// This needs to be called as early as possible. After the first log writing, it
    /// is not possible to alter the log configuration programmatically anymore.
    public static void initLogging(String[] args) {
        // routeLoggingToSlf4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // We must configure logging as soon as possible, which is why we cannot wait for the usual
        // argument parsing workflow to parse logging options e.g. --debug or --porcelain
        Level logLevel;
        if (Arrays.stream(args).anyMatch("--debug"::equalsIgnoreCase)) {
            logLevel = Level.DEBUG;
        } else if (Arrays.stream(args).anyMatch("--porcelain"::equalsIgnoreCase)) {
            logLevel = Level.ERROR;
        } else {
            logLevel = Level.INFO;
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
                "level", logLevel.name().toLowerCase(),
                "writerFile", "rolling file",
                "writerFile.logLevel", logLevel == Level.DEBUG ? "debug" : "info",
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
