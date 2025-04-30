package org.jabref.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.logic.importer.ParserResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Parameters;
import static picocli.CommandLine.ParentCommand;

@Command(name = "convert", description = "Convert between bibliography formats.")
public class Convert implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Convert.class);

    @ParentCommand
    private KitCommandLine kitCommandLine;

    @Parameters(index = "0", description = "Input file", arity = "0..1")
    @Option(names = {"--input"}, description = "Input file")
    private Path inputFile;

    @Option(names = {"--input-format"}, description = "Input format")
    private String inputFormat;

    @Parameters(index = "1", description = "Output file")
    @Option(names = {"--output"}, description = "Output file")
    private Path outputFile;

    @Option(names = {"--output-format"}, description = "Output format")
    private String outputFormat;

    @Override
    public Integer call() throws Exception {
        if (inputFile == null
                || !Files.exists(inputFile)
                || outputFile == null) {
            return 1;
        }

        Optional<ParserResult> pr = kitCommandLine.importFile(inputFile, inputFormat);
        if (pr.isPresent()) {
            kitCommandLine.exportFile(pr.get(), outputFile, outputFormat);
        } else {
            LOGGER.error("Unable to export input file {}", inputFile);
        }
        return 0;
    }
}
