package org.jabref.toolkit.commands;

import java.nio.file.Path;

import org.jabref.toolkit.converter.CygWinPathConverter;

import io.github.adr.linked.ADR;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/// Reusable input-file argument shared by all `jabkit` subcommands that read a single file.
///
/// The file can be supplied either as a positional argument
/// (`jabkit check integrity references.bib`) or via the `--input` option
/// (`jabkit check integrity --input references.bib`). The positional form is the preferred,
/// idiomatic style; `--input` is retained as a backward-compatible alias.
///
/// Exactly one of the two forms must be given; picocli enforces this at parse time via the
/// required, mutually exclusive [ArgGroup].
///
/// See ADR-0057 (which supersedes ADR-0045) for the rationale.
class InputOption {

    @ArgGroup(exclusive = true, multiplicity = "1")
    private InputSource inputSource;

    /// @return the resolved input file
    Path getInputFile() {
        return inputSource.positionalInput != null
               ? inputSource.positionalInput
               : inputSource.optionInput;
    }

    private static class InputSource {
        // [impl->req~jabkit.cli.input-flag~2]
        @Parameters(index = "0", paramLabel = "FILE", converter = CygWinPathConverter.class,
                description = "Input file. Alternatively, pass it via --input.")
        private Path positionalInput;

        @ADR(45)
        @Option(names = {"--input"}, converter = CygWinPathConverter.class,
                description = "Input file (alias for the positional FILE argument).")
        private Path optionInput;
    }
}
