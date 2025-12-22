package org.jabref.toolkit;

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
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.toolkit.commands.JabKit;

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
public class JabKitLauncher {
    // J.U.L. bridge to SLF4J must be initialized before any logger is created, see initLogging()
    private static Logger LOGGER;

    private static final String JABKIT_BRAND = "JabKit - command line toolkit for JabRef";

    /// Note: To test with gradle, use jabkit -> Tasks -> application -> run
    ///       Use `--args="..."` as parameters to "Run"
    public static void main(String[] args) {
        initLogging(args);

        try {
            final JabRefCliPreferences preferences = JabRefCliPreferences.getInstance();
            Injector.setModelOrService(CliPreferences.class, preferences);

            BuildInfo buildInfo = new BuildInfo();
            Injector.setModelOrService(BuildInfo.class, buildInfo);

            BibEntryTypesManager entryTypesManager = preferences.getCustomEntryTypesRepository();
            Injector.setModelOrService(BibEntryTypesManager.class, entryTypesManager);

            JabKit jabKit = new JabKit(preferences, entryTypesManager);
            CommandLine commandLine = new CommandLine(jabKit);
            String usageHeader = BuildInfo.JABREF_BANNER.formatted(buildInfo.version) + "\n" + JABKIT_BRAND;
            commandLine.getCommandSpec().usageMessage().header(usageHeader);
            applyUsageFooters(commandLine,
                    JabKit.getAvailableImportFormats(preferences),
                    JabKit.getAvailableExportFormats(preferences),
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

    /**
     * Applies appropriate usage footers to each subcommand based on their supported options.
     * Distinguishes between input formats, output formats, and export formats.
     */
    private static void applyUsageFooters(CommandLine commandLine,
                                          List<Pair<String, String>> inputFormats,
                                          List<Pair<String, String>> outputFormats,
                                          Set<SearchBasedFetcher> fetchers) {

        final String INPUT_FOOTER_LABEL = Localization.lang("Available import formats:");
        final String OUTPUT_FOOTER_LABEL = Localization.lang("Available output formats:");
        final String EXPORT_FOOTER_LABEL = Localization.lang("Available export formats:");

        String inputFooter = "\n"
                + INPUT_FOOTER_LABEL + "\n"
                + StringUtil.alignStringTable(inputFormats);
        String outputFooter = "\n"
                + OUTPUT_FOOTER_LABEL + "\n"
                + StringUtil.alignStringTable(outputFormats);
        String exportFooter = "\n"
                + EXPORT_FOOTER_LABEL + "\n"
                + StringUtil.alignStringTable(outputFormats);

        commandLine.getSubcommands().values().forEach(subCommand -> {
            Map<String, Boolean> hasOptions = Map.of(
                    "input", hasCommandOption(subCommand.getCommandSpec(), "--input-format"),
                    "output", hasCommandOption(subCommand.getCommandSpec(), "--output-format"),
                    "export", hasCommandOption(subCommand.getCommandSpec(), "--export-format")
            );

            String footerText = "";
            // Skip format footers for check-consistency since formats are already documented in option description
            if (!"check-consistency".equals(subCommand.getCommandSpec().name())) {
                footerText += hasOptions.get("input") ? inputFooter : "";
                footerText += hasOptions.get("output") ? outputFooter : "";
                footerText += hasOptions.get("export") ? exportFooter : "";
            }
            subCommand.getCommandSpec().usageMessage().footer(footerText);
        });

        commandLine.getSubcommands().get("fetch")
                   .getCommandSpec().usageMessage().footer(Localization.lang("The following providers are available:") + "\n"
                           + fetchers.stream()
                                     .map(WebFetcher::getName)
                                     .filter(name -> !"Search pre-configured".equals(name))
                                     .collect(Collectors.joining(", ")));
    }

    private static boolean hasCommandOption(CommandLine.Model.CommandSpec commandSpec, String optionName) {
        return commandSpec.options().stream()
                          .anyMatch(opt -> Arrays.asList(opt.names()).contains(optionName));
    }

    /// This needs to be called as early as possible. After the first log writing, it
    /// is not possible to alter the log configuration programmatically anymore.
    public static void initLogging(String[] args) {
        // routeLoggingToSlf4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // We must configure logging as soon as possible, which is why we cannot wait for the usual
        // argument parsing workflow to parse logging options e.g. --debug or --porcelain
        boolean isPorcelain = Arrays.stream(args).anyMatch("--porcelain"::equalsIgnoreCase);
        Level logLevel;
        if (Arrays.stream(args).anyMatch("--debug"::equalsIgnoreCase)) {
            logLevel = Level.DEBUG;
        } else {
            logLevel = Level.INFO;
        }

        // addLogToDisk
        // We cannot use `Injector.instantiateModelOrService(BuildInfo.class).version` here, because this initializes logging
        Path directory = Directories.getLogDirectory(new BuildInfo().version);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            LOGGER = LoggerFactory.getLogger(JabKitLauncher.class);
            LOGGER.error("Could not create log directory {}", directory, e);
            return;
        }

        String fileWriterName;
        if (isPorcelain) {
            fileWriterName = "writer";
        } else {
            fileWriterName = "writerFile";
        }

        // The "Shared File Writer" is explained at
        // https://tinylog.org/v2/configuration/#shared-file-writer
        Configuration.set("level", logLevel.name().toLowerCase());
        Configuration.set(fileWriterName, "rolling file");
        Configuration.set("%s.logLevel".formatted(fileWriterName), logLevel == Level.DEBUG ? "debug" : "info");
        // We need to manually join the path, because ".resolve" does not work on Windows, because ":" is not allowed in file names on Windows
        Configuration.set("%s.file".formatted(fileWriterName), directory + File.separator + "log_{date:yyyy-MM-dd_HH-mm-ss}.txt");
        Configuration.set("%s.charset".formatted(fileWriterName), "UTF-8");
        Configuration.set("%s.policies".formatted(fileWriterName), "startup");
        Configuration.set("%s.backups".formatted(fileWriterName), "30");

        LOGGER = LoggerFactory.getLogger(JabKitLauncher.class);
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
