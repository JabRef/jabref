package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.toolkit.exception.CliExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class JabFixCommandTest extends AbstractJabKitTest {

    private static final String NOTHING_ENABLED = Localization.lang("No mode or rule is enabled, so nothing is checked or fixed. Choose a mode with --mode (%0), and add --save-config to keep it.",
            "whitespace, report, default-save-actions");

    private String inputFile;

    @BeforeEach
    void setUpCommand() {
        // The launcher installs this; without it a usage error surfaces as a stack trace and exit code 70.
        commandLine.setExecutionExceptionHandler(new CliExceptionHandler(commandLine.getExecutionExceptionHandler()));
        when(preferences.getFieldPreferences()).thenReturn(new FieldPreferences(true, List.of(), List.of()));
        inputFile = getClassResourceAsFullyQualifiedString("jabfix-sloppy.bib");
    }

    /// Off by default: a library that asks for nothing has nothing done to it, and the run says so,
    /// since the library is still written and the missing repairs are easy to overlook.
    @Test
    void nothingRunsUnlessAModeOrARuleIsEnabled() {
        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", inputFile));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = { Knuth, Donald E. },"), formatted);
        String reported = commandLine.getErrorOutput();
        assertTrue(reported.contains(NOTHING_ENABLED), reported);
    }

    @Test
    void aModeEnablesItsRules() {
        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", "--mode", "whitespace", inputFile));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = {Knuth, Donald E.},"), formatted);
        String reported = commandLine.getErrorOutput();
        assertFalse(reported.contains(NOTHING_ENABLED), reported);
    }

    @Test
    void anUnknownModeIsAUsageError() {
        assertEquals(CommandLine.ExitCode.USAGE, commandLine.executeToLog("fix", "--mode", "whitespce", inputFile));

        String errors = commandLine.getErrorOutput();
        assertTrue(errors.contains("Unknown mode: whitespce."), errors);
    }

    /// The save action goes on being applied, and the library now states it in the one place that
    /// says what is done to it.
    @Test
    void saveConfigMovesTheSaveActionsIntoTheConfiguration() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", "--save-config",
                        getClassResourceAsFullyQualifiedString("jabfix-save-actions.bib")));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("title  = {some title},"), formatted);
        assertTrue(formatted.contains("\"title:lower-case\" : \"fix\""), formatted);
        assertFalse(formatted.contains("saveActions"), formatted);
    }

    /// Nothing to store and nothing to move, so the library is not given a configuration.
    @Test
    void saveConfigLeavesALibraryWithNothingToStoreAsItWas() {
        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", "--save-config", inputFile));

        String formatted = commandLine.getStandardOutput();
        assertFalse(formatted.contains("jabref-meta-0.1.0"), formatted);
    }

    /// Nobody is made to move away from Save Actions: the item stays, and goes on being applied.
    @Test
    void aLibraryKeepsItsSaveActionsUntilItIsConfigured() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix",
                        getClassResourceAsFullyQualifiedString("jabfix-save-actions.bib")));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("title  = {some title},"), formatted);
        assertTrue(formatted.contains("saveActions"), formatted);
        assertFalse(formatted.contains("jabref-meta-0.1.0"), formatted);
        String reported = commandLine.getErrorOutput();
        assertFalse(reported.contains(NOTHING_ENABLED), reported);
    }

    /// The library says `surrounding-whitespace` only reports, and names no other rule, so the
    /// whitespace around the author survives and is said out loud instead.
    @Test
    void theLibrarysFormattingBlockConfiguresTheRun() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", getClassResourceAsFullyQualifiedString("jabfix-configured.bib")));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = { Knuth, Donald E. },"), formatted);
        String reported = commandLine.getErrorOutput();
        assertTrue(reported.contains("[surrounding-whitespace]"), reported);
    }

    /// An option typed for one run outranks what the library says.
    @Test
    void aRuleSwitchedOffBeatsTheLibrarysFormattingBlock() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", "--rule", "surrounding-whitespace=off",
                        getClassResourceAsFullyQualifiedString("jabfix-configured.bib")));

        String reported = commandLine.getErrorOutput();
        assertFalse(reported.contains("[surrounding-whitespace]"), reported);
    }

    @Test
    void aRuleSetToFixBeatsTheLibrarysFormattingBlock() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", "--rule", "surrounding-whitespace=fix",
                        getClassResourceAsFullyQualifiedString("jabfix-configured.bib")));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = {Knuth, Donald E.},"), formatted);
    }

    /// `--rule` names rules on top of the library's, so a rule only the library names still runs.
    @Test
    void aRuleOptionLeavesTheOtherRulesOfTheLibraryInTheRun() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", "--rule", "repeated-whitespace=fix",
                        getClassResourceAsFullyQualifiedString("jabfix-configured.bib")));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = { Knuth, Donald E. },"), formatted);
        String reported = commandLine.getErrorOutput();
        assertTrue(reported.contains("[surrounding-whitespace]"), reported);
    }

    /// A mode is broader than a rule the library names, so the rule goes on doing what the library says.
    @Test
    void aRuleTheLibraryNamesOutranksTheModeOfTheRun() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", "--mode", "whitespace",
                        getClassResourceAsFullyQualifiedString("jabfix-configured.bib")));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = { Knuth, Donald E. },"), formatted);
    }

    @Test
    void theModeOfTheRunTakesThePlaceOfTheLibrarys(@TempDir Path tempDir) throws IOException {
        Path library = Files.writeString(tempDir.resolve("reported.bib"), """
                @Comment{jabref-meta-0.1.0
                {
                  "formatting" : {
                    "mode" : "report"
                  }
                }
                }

                @Article{key,
                  author = { Doe, Jane },
                }
                """);

        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", "--mode", "whitespace", library.toString()));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = {Doe, Jane},"), formatted);
        assertTrue(formatted.contains("\"mode\" : \"report\""), formatted);
    }

    /// The rules of a run are not what the library says about itself, so formatting one must not
    /// leave a configuration behind in a library that never had one.
    @Test
    void formattingALibraryDoesNotGiveItAFormattingBlock() {
        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", "--mode", "whitespace", inputFile));

        String formatted = commandLine.getStandardOutput();
        assertFalse(formatted.contains("jabref-meta-0.1.0"), formatted);
    }

    /// Every formatter a Save Action can use is a rule, so it can be named without the library
    /// having such a Save Action -- and narrowed to one field.
    @Test
    void aRuleOptionTakesAFormatterOnOneField(@TempDir Path tempDir) throws IOException {
        Path library = Files.writeString(tempDir.resolve("shouting.bib"), """
                @Article{key,
                  author = {DOE, JANE},
                  title  = {SOME TITLE},
                }
                """);

        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", "--rule", "title:lower-case=fix", library.toString()));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("title  = {some title},"), formatted);
        assertTrue(formatted.contains("author = {DOE, JANE},"), formatted);
    }

    /// One option gives every rule its mode, spelled the way the stored configuration spells it.
    @Test
    void aRuleOptionGivesEachRuleItsMode() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", "--rule", "repeated-whitespace=fix,surrounding-whitespace=check", inputFile));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = { Knuth, Donald E. },"), formatted);
        String reported = commandLine.getErrorOutput();
        assertTrue(reported.contains("[surrounding-whitespace]"), reported);
    }

    /// A rule outranks a mode, the run's included.
    @Test
    void aRuleSwitchedOffOutranksTheModeOfTheRun() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", "--mode", "whitespace", "--rule", "surrounding-whitespace=off", inputFile));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = { Knuth, Donald E. },"), formatted);
    }

    /// Split into two rules, of which only the misspelled one is unknown.
    @Test
    void aRuleOptionTakesACommaSeparatedList() {
        assertEquals(CommandLine.ExitCode.USAGE,
                commandLine.executeToLog("fix", "--rule", "surrounding-whitespace=off,surounding-whitespace=off", inputFile));

        String errors = commandLine.getErrorOutput();
        assertTrue(errors.contains("Unknown rule: surounding-whitespace."), errors);
    }

    /// A typo has to be reported, not passed over: otherwise the user believes a rule is off while
    /// it goes on running.
    @Test
    void anIdThatNamesNoRuleIsAUsageError() {
        assertEquals(CommandLine.ExitCode.USAGE,
                commandLine.executeToLog("fix", "--rule", "surounding-whitespace=off", inputFile));

        String errors = commandLine.getErrorOutput();
        assertTrue(errors.contains("surounding-whitespace"), errors);
        assertTrue(errors.contains("surrounding-whitespace"), errors);
    }

    @Test
    void anUnknownRuleModeIsAUsageError() {
        assertEquals(CommandLine.ExitCode.USAGE,
                commandLine.executeToLog("fix", "--rule", "surrounding-whitespace=repair", inputFile));

        String errors = commandLine.getErrorOutput();
        assertTrue(errors.contains("Unknown rule mode: repair. Available rule modes: off, check, fix"), errors);
    }

    /// Set once, the mode is what the library says, so later runs and JabRef's saves need no options.
    @Test
    void saveConfigStoresTheModeOfTheRun() {
        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", "--mode", "whitespace", "--save-config", inputFile));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = {Knuth, Donald E.},"), formatted);
        assertTrue(formatted.contains("\"mode\" : \"whitespace\""), formatted);
    }

    /// What the library said and the run did not touch stays what the library says.
    @Test
    void saveConfigKeepsWhatTheLibrarySaidBeside() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", "--mode", "whitespace", "--save-config",
                        getClassResourceAsFullyQualifiedString("jabfix-configured.bib")));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("\"mode\" : \"whitespace\""), formatted);
        assertTrue(formatted.contains("\"surrounding-whitespace\" : \"check\""), formatted);
    }

    /// A finding says where it is, in the `file:line:column:citationKey:field: message` format the
    /// `check` commands use, so an editor or a CI log scraper can jump to it.
    @Test
    void checkReportsWhereEveryFindingIs(@TempDir Path tempDir) throws IOException {
        Path library = Files.writeString(tempDir.resolve("sloppy.bib"), """
                @Article{knuth1984,
                  author = { Knuth, Donald E. },
                }
                """);

        assertEquals(1, commandLine.executeToLog("fix", "--check", "-p", "--mode", "whitespace", library.toString()));

        assertEquals(List.of(library + ":2:3:knuth1984:author: value has leading or trailing whitespace [surrounding-whitespace]"),
                commandLine.getStandardOutput().replace("\r\n", "\n").lines()
                           .filter(line -> line.contains("[")).toList());
    }

    /// Serialization is the writer's half of the work: entry type capitalization, value delimiters,
    /// field order and indentation are normalized whatever the input looked like.
    @Test
    void theFormattedLibraryIsNormalized(@TempDir Path tempDir) throws IOException {
        Path library = Files.writeString(tempDir.resolve("sloppy.bib"), """
                @ARTICLE{key,
                author = " Doe, Jane ",
                    YEAR="2024"
                }
                """);

        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", "--mode", "whitespace", library.toString()));

        // The database type is written although the input never named it: the importer infers it for
        // a file that declares none, so even a clean library grows this line.
        assertEquals("""
                @Article{key,
                  author = {Doe, Jane},
                  year   = {2024},
                }

                @Comment{jabref-meta: databaseType:bibtex;}
                """, commandLine.getStandardOutput().replace("\r\n", "\n"));
    }

    @Test
    void formattingAnAlreadyFormattedLibraryChangesNothing(@TempDir Path tempDir) throws IOException {
        Path library = Files.writeString(tempDir.resolve("sloppy.bib"), """
                @ARTICLE{key,
                author = " Doe, Jane ",
                    YEAR="2024"
                }
                """);

        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", "--in-place", "--mode", "whitespace", library.toString()));
        String once = Files.readString(library);
        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", "--in-place", "--mode", "whitespace", library.toString()));

        assertEquals(once, Files.readString(library));
    }

    /// A magic comment naming no rule switches nothing off, so the check says so instead of letting
    /// the entry be repaired as if the comment were not there.
    @Test
    void checkReportsAMagicCommentThatNamesNoRule(@TempDir Path tempDir) throws IOException {
        Path library = Files.writeString(tempDir.resolve("typo.bib"), """
                % jabref-format-ignore surounding-whitespace
                @Article{key,
                  author = {Doe, Jane},
                }
                """);

        assertEquals(1, commandLine.executeToLog("fix", "--check", "-p", "--mode", "whitespace", library.toString()));

        String findings = commandLine.getStandardOutput();
        assertTrue(findings.contains("[magic-comment]"), findings);
    }

    /// Switching a rule off for one run is no reason to report the comments that switch it off.
    @Test
    void checkDoesNotReportAMagicCommentNamingARuleTheRunSwitchedOff(@TempDir Path tempDir) throws IOException {
        Path library = Files.writeString(tempDir.resolve("clean.bib"), """
                % jabref-format-ignore repeated-whitespace
                @Article{key,
                  author = {Doe, Jane},
                }
                """);

        commandLine.executeToLog("fix", "--check", "-p", "--mode", "whitespace", "--rule", "repeated-whitespace=off", library.toString());

        String findings = commandLine.getStandardOutput();
        assertFalse(findings.contains("[magic-comment]"), findings);
    }

    /// Trimming is a rule like any other now, so a magic comment keeps that one field as written
    /// while the rest of the entry is still repaired.
    @Test
    void aSuppressedFieldKeepsItsWhitespace(@TempDir Path tempDir) throws IOException {
        Path library = Files.writeString(tempDir.resolve("suppressed.bib"), """
                % jabref-format-ignore author:surrounding-whitespace
                @ARTICLE{key,
                author = " Doe, Jane ",
                title = " A Title "
                }
                """);

        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", "--mode", "whitespace", library.toString()));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = { Doe, Jane },"), formatted);
        assertTrue(formatted.contains("title  = {A Title},"), formatted);
    }

    @Test
    void inPlaceAndCheckCannotBeCombined() {
        assertEquals(CommandLine.ExitCode.USAGE,
                commandLine.executeToLog("fix", "--in-place", "--check", inputFile));
    }
}
