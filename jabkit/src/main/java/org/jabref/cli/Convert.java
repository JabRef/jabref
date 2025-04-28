package org.jabref.cli;

import java.io.File;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Parameters;

@Command(name = "convert", description = "Convert between bibliography formats.")
public class Convert implements Callable<Integer> {

    @Parameters(index = "0", description = "Input file", arity = "0..1")
    @Option(names = {"--input"}, description = "Input file")
    private File inputFile;

    @Parameters(index = "1", description = "Output file", arity = "0..1")
    @Option(names = {"--output"}, description = "Output file")
    private File outputFile;

    @Override
    public Integer call() throws Exception {
        // TODO: Implement conversion functionality
        return 0;
    }
}
