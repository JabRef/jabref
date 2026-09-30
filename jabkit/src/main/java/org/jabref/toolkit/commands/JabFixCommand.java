package org.jabref.toolkit.commands;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.lint.rule.Finding;
import org.jabref.logic.lint.rule.RuleSet;
import org.jabref.logic.lint.rule.UnknownRuleException;
import org.jabref.logic.util.ErrorFormat;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.LintSettings;
import org.jabref.model.metadata.RuleMode;
import org.jabref.model.metadata.RuleSelector;
import org.jabref.toolkit.exception.CliException;
import org.jabref.toolkit.exception.ImportServiceException;
import org.jabref.toolkit.service.ImportService;

import org.jspecify.annotations.NullMarked;
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

    @Option(names = {"--enable"}, split = ",", paramLabel = "RULE",
            description = "Rule to apply, which then are the only ones applied. Repeatable, and accepts a comma-separated list. Without it, every rule the library has is applied. The available rules are listed below.")
    private List<String> enabledRules = List.of();

    @Option(names = {"--disable"}, split = ",", paramLabel = "RULE",
            description = "Rule to switch off. Repeatable, and accepts a comma-separated list. The available rules are listed below.")
    private List<String> disabledRules = List.of();

    @Option(names = {"--check-only"}, split = ",", paramLabel = "RULE",
            description = "Rule that only reports what it finds, without repairing it. Repeatable, and accepts a comma-separated list.")
    private List<String> checkOnlyRules = List.of();

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
        // Asking the library to apply the rules is all it takes; the writer builds them. Which
        // rules there are needs the library, since its own Save Actions are rules of this run and
        // the options cover them like any other. These are the rules of this run and not of the
        // library, so they override what it says rather than becoming what it says.
        databaseContext.getMetaData().overrideLintSettings(new LintSettings(selectedModes(databaseContext)));

        try {
            // Only the parsed library in memory is changed here; nothing reaches disk unless
            // --in-place says so, which is what lets --check reuse the very same run.
            SerializedLibrary library = serialize(databaseContext);
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

    /// How far each rule of the run goes.
    ///
    /// A rule runs only where it is named. Naming none, this command names every rule the library
    /// has -- its own Save Actions and the rules JabFix ships with -- and lets each repair, which
    /// is what makes a bare `jabkit fix` format a library. `--enable` names them instead, and then
    /// they are the only ones that run.
    ///
    /// `--check-only` leaves a rule reporting what it finds without repairing it, and names it too:
    /// a rule `--enable` left out still runs if `--check-only` asks for it. `--disable` takes one
    /// out of the run whatever else named it.
    ///
    /// A misspelled id is a usage error, not something to pass over: leaving it unreported would let
    /// the user believe a rule had been switched off while it kept running. A library's own settings
    /// are treated more leniently -- see [RuleSet#asConfiguredBy].
    private SequencedMap<RuleSelector, RuleMode> selectedModes(BibDatabaseContext databaseContext) throws CliException {
        RuleSet rules = RuleSet.forLibrary(databaseContext, jabKit.cliPreferences.getFieldPreferences());
        try {
            rules.rejectUnknown(Stream.of(enabledRules, checkOnlyRules, disabledRules)
                                      .flatMap(List::stream)
                                      .toList());

            // The command line names rules, not fields, so every selector it builds covers every
            // field the rule looks at.
            SequencedMap<RuleSelector, RuleMode> modes = new LinkedHashMap<>();
            (enabledRules.isEmpty() ? rules.ids() : enabledRules)
                    .forEach(ruleId -> modes.put(RuleSelector.of(ruleId), RuleMode.FIX));
            checkOnlyRules.forEach(ruleId -> modes.put(RuleSelector.of(ruleId), RuleMode.CHECK));
            // A rule named by several options is switched off: the strictest of them wins, and
            // saying so in the one place that reads them all keeps it from being a question
            // anywhere else.
            disabledRules.forEach(ruleId -> modes.put(RuleSelector.of(ruleId), RuleMode.OFF));
            return modes;
        } catch (UnknownRuleException e) {
            LOGGER.debug("Rejecting unknown rule id", e);
            throw new CliException(e.getMessage(),
                    Localization.lang("Unknown rule: %0. Available rules: %1",
                            String.join(", ", e.getUnknownIds()),
                            String.join(", ", e.getKnownIds())),
                    CommandLine.ExitCode.USAGE);
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
    private SerializedLibrary serialize(BibDatabaseContext databaseContext) throws IOException {
        StringWriter stringWriter = new StringWriter();
        SelfContainedSaveConfiguration saveConfiguration =
                (SelfContainedSaveConfiguration) new SelfContainedSaveConfiguration().withReformatOnSave(true);

        BibDatabaseWriter databaseWriter = new BibDatabaseWriter(
                new BibWriter(stringWriter, databaseContext.getDatabase().getNewLineSeparator()),
                saveConfiguration,
                jabKit.cliPreferences.getFieldPreferences(),
                jabKit.cliPreferences.getCitationKeyPatternPreferences(),
                jabKit.entryTypesManager);
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
