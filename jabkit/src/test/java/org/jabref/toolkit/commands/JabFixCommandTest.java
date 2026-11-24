package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.toolkit.exception.CliExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class JabFixCommandTest extends AbstractJabKitTest {

    private String inputFile;

    @BeforeEach
    void setUpCommand() {
        // The launcher installs this; without it a usage error surfaces as a stack trace and exit code 70.
        commandLine.setExecutionExceptionHandler(new CliExceptionHandler(commandLine.getExecutionExceptionHandler()));
        when(preferences.getFieldPreferences()).thenReturn(new FieldPreferences(true, List.of(), List.of()));
        inputFile = getClassResourceAsFullyQualifiedString("jabfix-sloppy.bib");
    }

    @Test
    void everyRuleRunsByDefault() {
        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", inputFile));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = {Knuth, Donald E.},"), formatted);
    }

    /// Naming one rule leaves the rest of them out, so the whitespace around the author survives.
    @Test
    void enableNamesTheOnlyRulesThatRun() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", "--enable", "repeated-whitespace", inputFile));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = { Knuth, Donald E. },"), formatted);
    }

    /// `--check-only` names a rule as well, so one left out of `--enable` still reports.
    @Test
    void checkOnlyRunsARuleEnableLeftOut() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", "--enable", "repeated-whitespace",
                        "--check-only", "surrounding-whitespace", inputFile));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = { Knuth, Donald E. },"), formatted);
        String reported = commandLine.getErrorOutput();
        assertTrue(reported.contains("[surrounding-whitespace]"), reported);
    }

    @Test
    void anIdThatNamesNoRuleIsAUsageErrorForEnableToo() {
        assertEquals(CommandLine.ExitCode.USAGE,
                commandLine.executeToLog("fix", "--enable", "surounding-whitespace", inputFile));

        String errors = commandLine.getErrorOutput();
        assertTrue(errors.contains("surounding-whitespace"), errors);
    }

    @Test
    void disableSwitchesARuleOff() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", "--disable", "surrounding-whitespace", inputFile));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = { Knuth, Donald E. },"), formatted);
    }

    /// The value stays as it was, and the finding is said out loud instead of passing unseen.
    @Test
    void checkOnlyReportsARuleWithoutApplyingIt() {
        assertEquals(CommandLine.ExitCode.OK,
                commandLine.executeToLog("fix", "--check-only", "surrounding-whitespace", inputFile));

        String formatted = commandLine.getStandardOutput();
        assertTrue(formatted.contains("author = { Knuth, Donald E. },"), formatted);
        String reported = commandLine.getErrorOutput();
        assertTrue(reported.contains("[surrounding-whitespace]"), reported);
    }

    @Test
    void anIdThatNamesNoRuleIsAUsageErrorForCheckOnlyToo() {
        assertEquals(CommandLine.ExitCode.USAGE,
                commandLine.executeToLog("fix", "--check-only", "surounding-whitespace", inputFile));

        String errors = commandLine.getErrorOutput();
        assertTrue(errors.contains("surounding-whitespace"), errors);
    }

    /// Split into two ids, of which only the misspelled one is unknown.
    @Test
    void disableTakesACommaSeparatedList() {
        assertEquals(CommandLine.ExitCode.USAGE,
                commandLine.executeToLog("fix", "--disable", "surrounding-whitespace,surounding-whitespace", inputFile));

        String errors = commandLine.getErrorOutput();
        assertTrue(errors.contains("Unknown rule: surounding-whitespace."), errors);
    }

    /// A typo has to be reported, not passed over: otherwise the user believes a rule is off while
    /// it goes on running.
    @Test
    void anIdThatNamesNoRuleIsAUsageError() {
        assertEquals(CommandLine.ExitCode.USAGE,
                commandLine.executeToLog("fix", "--disable", "surounding-whitespace", inputFile));

        String errors = commandLine.getErrorOutput();
        assertTrue(errors.contains("surounding-whitespace"), errors);
        assertTrue(errors.contains("surrounding-whitespace"), errors);
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

        assertEquals(1, commandLine.executeToLog("fix", "--check", "-p", library.toString()));

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

        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", library.toString()));

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

        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", "--in-place", library.toString()));
        String once = Files.readString(library);
        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", "--in-place", library.toString()));

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

        assertEquals(1, commandLine.executeToLog("fix", "--check", "-p", library.toString()));

        String findings = commandLine.getStandardOutput();
        assertTrue(findings.contains("[magic-comment]"), findings);
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

        assertEquals(CommandLine.ExitCode.OK, commandLine.executeToLog("fix", library.toString()));

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
