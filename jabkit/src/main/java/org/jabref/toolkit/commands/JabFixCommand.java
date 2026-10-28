package org.jabref.toolkit.commands;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.lint.JabFixConfiguration;
import org.jabref.logic.lint.JabFixMode;
import org.jabref.logic.lint.rule.Finding;
import org.jabref.logic.lint.rule.RuleSet;
import org.jabref.logic.lint.rule.UnknownRuleException;
import org.jabref.logic.util.ErrorFormat;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;
import org.jabref.toolkit.converter.JabFixModeConverter;
import org.jabref.toolkit.exception.CliException;
import org.jabref.toolkit.exception.ImportServiceException;
import org.jabref.toolkit.service.ImportService;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

/// Thin CLI wrapper around [JabFix]; the rules and the formatting live in the `org.jabref.logic.lint` package.
///
/// Exit codes follow the other checking commands: 0 = nothing to do, 1 = the library is not clean
/// (`--check` only), 2/3 = error.
@NullMarked
@Command(name = "fix", description = "Lint and format a BibTeX library.")
class JabFixCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JabFixCommand.class);

    @ParentCommand
    private JabKit jabKit;

    @Mixin
    private JabKit.SharedOptions sharedOptions;

    @Mixin
    private InputOption inputOption = new InputOption();

    @Option(names = {"--in-place"}, description = "Write the formatted library back to the input file instead of to standard output.")
    private boolean inPlace;

    @Option(names = {"--check"}, description = "Report what is wrong without writing anything.")
    private boolean checkOnly;

    @Option(names = {"--mode"}, paramLabel = "MODE", converter = JabFixModeConverter.class,
            description = "Mode to apply instead of the library's: a set of rules JabFix ships. Rules the library or --rule name still outrank it. The available modes are listed below.")
    private @Nullable JabFixMode mode;

    @Option(names = {"--rule"}, split = ",", paramLabel = "RULE=MODE",
            description = "How far a rule goes, outranking what the library says about it: off, check (report only) or fix (report and repair). "
                    + "'field:rule' narrows a rule to one field. Repeatable, and accepts a comma-separated list. The available rules are listed below.")
    private Map<String, String> ruleModes = new LinkedHashMap<>();

    @Option(names = {"--save-config"},
            description = "Store --mode and --rule in the library, moving its save actions there as well, so that later runs and JabRef's saves apply them without options. "
                    + "Together with --in-place this rewrites the library; without it, the result only goes to standard output.")
    private boolean saveConfig;

    // [impl->req~jabkit.cli.jabfix~1]
    @Override
    public Integer call() throws ImportServiceException, CliException {
        if (inPlace && checkOnly) {
            throw new CliException("--in-place and --check are mutually exclusive",
                    Localization.lang("Only one of --in-place and --check can be given."),
                    CommandLine.ExitCode.USAGE);
        }

        Path inputFile = inputOption.getInputFile(jabKit.cliPreferences);

        // Without --in-place or --check the formatted library itself goes to stdout, so the
        // importer's progress chatter has to be suppressed there to keep the output pipeable.
        boolean quiet = sharedOptions.porcelain || !(inPlace || checkOnly);
        ParserResult parserResult = ImportService.importBibTexFile(inputFile, jabKit.cliPreferences, quiet);

        BibDatabaseContext databaseContext = parserResult.getDatabaseContext();
        MetaData metaData = databaseContext.getMetaData();
        // These are the rules of this run and not of the library, so they are laid over what it
        // says rather than becoming what it says -- unless --save-config makes them what it says.
        LintSettings runSettings = runSettings();
        if (saveConfig) {
            // Storing them leaves what the run applies unchanged, since the run lays them over the
            // library either way, so it does not matter that this happens before the run.
            JabFixConfiguration.configure(metaData, metaData.getFormatting().orElse(LintSettings.NONE).overriddenBy(runSettings));
        }
        warnIfNothingIsEnabled(JabFixConfiguration.applied(metaData, runSettings), metaData);

        try {
            // Only the parsed library in memory is changed here; nothing reaches disk unless
            // --in-place says so, which is what lets --check reuse the very same run.
            SerializedLibrary library = serialize(databaseContext, runSettings);
            List<Finding> findings = library.findings();

            if (checkOnly) {
                return check(inputFile, findings, library.formatted(), parserResult);
            }

            // A rule that found something it cannot repair has to be said out loud, since it will
            // not show up in the output the way an applied fix does.
            report(inputFile, findings.stream().filter(finding -> !finding.isFixable()).toList(), parserResult, System.err);

            if (inPlace) {
                return write(inputFile, library.formatted());
            }

            System.out.print(library.formatted());
            System.out.flush();
            return CommandLine.ExitCode.OK;
        } catch (IOException e) {
            System.err.println(Localization.lang("Unable to write to %0.", inPlace ? inputFile : "stdout"));
            return CommandLine.ExitCode.SOFTWARE;
        }
    }

    /// What this run asks for, which the writer lays over what the library asks for -- see
    /// [JabFixConfiguration#applied]: `--mode` takes the place of the library's mode, and each rule
    /// `--rule` names outranks what the library says about that rule. A rule `--rule` does not name
    /// goes on doing what the library says. `--rule` is spelled the way the stored configuration is,
    /// so a rule named twice takes the later mode, like a repeated key there.
    ///
    /// A misspelled id is a usage error, not something to pass over: leaving it unreported would let
    /// the user believe a rule had been switched off while it kept running. A library's own settings
    /// are treated more leniently -- see [RuleSet#asConfiguredBy].
    private LintSettings runSettings() throws CliException {
        LintSettings run = Optional.ofNullable(mode)
                                   .map(chosen -> LintSettings.NONE.withMode(chosen.getKey()))
                                   .orElse(LintSettings.NONE);
        rejectUnknownRules(ruleModes.keySet());
        for (Map.Entry<String, String> rule : ruleModes.entrySet()) {
            RuleMode ruleMode = knownRuleMode(rule.getValue());
            // Readable: a key that is not names no rule, and was rejected above.
            run = run.and(RuleSelector.parse(rule.getKey()).orElseThrow(), ruleMode);
        }
        return run;
    }

    /// Holds the keys the user typed against the rules JabFix knows. One that cannot be read as a
    /// selector names no rule either, and is reported the same way.
    private void rejectUnknownRules(Collection<String> keys) throws CliException {
        try {
            RuleSet.known(jabKit.cliPreferences.getFieldPreferences())
                   .rejectUnknown(keys.stream()
                                      .map(key -> RuleSelector.parse(key).map(RuleSelector::ruleId).orElse(key))
                                      .toList());
        } catch (UnknownRuleException e) {
            LOGGER.debug("Rejecting unknown rule id", e);
            throw new CliException(e.getMessage(),
                    Localization.lang("Unknown rule: %0. Available rules: %1",
                            String.join(", ", e.getUnknownIds()),
                            String.join(", ", e.getKnownIds())),
                    CommandLine.ExitCode.USAGE);
        }
    }

    private static RuleMode knownRuleMode(String key) throws CliException {
        return RuleMode.fromKey(key).orElseThrow(() -> new CliException("Unknown rule mode: " + key,
                Localization.lang("Unknown rule mode: %0. Available rule modes: %1", key,
                        Arrays.stream(RuleMode.values()).map(RuleMode::asKey).collect(Collectors.joining(", "))),
                CommandLine.ExitCode.USAGE));
    }

    /// Nothing runs unless a mode or a rule is enabled, which is easy to miss: the library is still
    /// written, and only its layout changes. The way out is one option, so the warning names it.
    private void warnIfNothingIsEnabled(LintSettings applied, MetaData metaData) {
        if (RuleSet.configuredBy(applied, metaData, jabKit.cliPreferences.getFieldPreferences()).rules().isEmpty()) {
            LOGGER.warn("No JabFix mode or rule is enabled, so no rule runs");
            System.err.println(Localization.lang("No mode or rule is enabled, so nothing is checked or fixed. Choose a mode with --mode (%0), and add --save-config to keep it.",
                    JabFixModeConverter.availableModes()));
        }
    }

    /// What a save of the library produces: the text, and what the rules reported while producing it.
    private record SerializedLibrary(String formatted, List<Finding> findings) {
    }

    /// Writes the library, which applies the rules it asks for on the way, since that is what a save
    /// of it does.
    ///
    /// Reformatting on save rewrites every entry; without it the writer would keep the serialization
    /// each entry had in the input file, which is exactly what is to be replaced.
    private SerializedLibrary serialize(BibDatabaseContext databaseContext, LintSettings runSettings) throws IOException {
        StringWriter stringWriter = new StringWriter();
        SelfContainedSaveConfiguration saveConfiguration =
                (SelfContainedSaveConfiguration) new SelfContainedSaveConfiguration().withReformatOnSave(true);

        BibDatabaseWriter databaseWriter = new BibDatabaseWriter(
                new BibWriter(stringWriter, databaseContext.getDatabase().getNewLineSeparator()),
                saveConfiguration,
                jabKit.cliPreferences.getFieldPreferences(),
                jabKit.cliPreferences.getCitationKeyPatternPreferences(),
                jabKit.entryTypesManager)
                .asFormatterRun(runSettings);
        databaseWriter.writeDatabase(databaseContext);

        return new SerializedLibrary(stringWriter.toString(), databaseWriter.getFindings());
    }

    private int check(Path inputFile, List<Finding> findings, String formatted, ParserResult parserResult) throws IOException {
        report(inputFile, findings, parserResult, System.out);

        // Findings alone are not the whole story: reformatting alters things no rule reports on,
        // such as entry type capitalization, so the serialized result has to be compared as well.
        boolean formattingDiffers = !formatted.equals(Files.readString(inputFile, StandardCharsets.UTF_8));

        if (findings.isEmpty() && !formattingDiffers) {
            if (!sharedOptions.porcelain) {
                System.out.println(Localization.lang("'%0' is already formatted.", inputFile));
            }
            return CommandLine.ExitCode.OK;
        }
        System.out.println(Localization.lang("'%0' would be reformatted.", inputFile));
        return 1;
    }

    private int write(Path inputFile, String formatted) throws IOException {
        Files.writeString(inputFile, formatted, StandardCharsets.UTF_8);
        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Saved %0.", inputFile));
        }
        return CommandLine.ExitCode.OK;
    }

    /// Writes one line per finding, in the same `file:line:column:citationKey[:field]: message`
    /// format the `check` commands use, with the rule id appended so that a reader knows what to
    /// switch off.
    // [impl->req~jabkit.cli.check-errorformat-output~1]
    private void report(Path inputFile, List<Finding> findings, ParserResult parserResult, PrintStream target) {
        for (Finding finding : findings) {
            target.println(ErrorFormat.line(
                    inputFile,
                    rangeOf(finding, parserResult),
                    finding.citationKey(),
                    finding.field(),
                    "%s [%s]".formatted(finding.message(), finding.rule().id())));
        }
    }

    /// Where the finding is in the file the library was read from: the field it is about, or the
    /// entry as a whole for a finding that names no field.
    private static ParserResult.Range rangeOf(Finding finding, ParserResult parserResult) {
        return finding.field()
                      .map(field -> parserResult.getFieldRange(finding.entry(), field))
                      .orElseGet(() -> parserResult.getCompleteEntryIndicator(finding.entry()));
    }
}
