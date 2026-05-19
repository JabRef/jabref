package org.jabref.toolkit.commands;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.jabref.logic.l10n.Localization;
import org.jabref.toolkit.converter.CygWinPathConverter;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Parameters;
import static picocli.CommandLine.ParentCommand;

@Command(name = "check", description = "Check the integrity and consistency of a library.",
        subcommands = {
                CheckConsistency.class,
                CheckIntegrity.class
        })
class Check implements Callable<Integer> {

    /// Output formats accepted by `--output-format`. Kept lowercase so they can also serve as
    /// `case` labels for a switch over `outputFormat.toLowerCase(...)`.
    static final String FORMAT_ERRORFORMAT = "errorformat";
    static final String FORMAT_TXT = "txt";
    static final String FORMAT_CSV = "csv";

    @ParentCommand
    protected JabKit jabKit;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    /// Optional positional input file. When supplied directly to `check` (without a
    /// `consistency` or `integrity` subcommand), both checks run against it.
    @Parameters(index = "0", arity = "0..1", paramLabel = "FILE", converter = CygWinPathConverter.class,
            description = "Input file. When given without a subcommand, both the consistency and integrity checks run.")
    private Path inputFile;

    @Option(names = {"--output-format"}, description = "Output format: errorformat, txt or csv", defaultValue = FORMAT_ERRORFORMAT)
    private String outputFormat;

    @Override
    public Integer call() {
        if (inputFile == null) {
            System.err.println(Localization.lang("Specify a subcommand (consistency, integrity) or an input file."));
            return 2;
        }

        int consistencyExit = CheckConsistency.execute(inputFile, outputFormat, sharedOptions.porcelain, jabKit);
        int integrityExit = CheckIntegrity.execute(inputFile, outputFormat, true, sharedOptions.porcelain, jabKit);

        // Report the worst exit code: 0 = clean, 1 = findings, 2/3 = error.
        return Math.max(consistencyExit, integrityExit);
    }
}
