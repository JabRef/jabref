package org.jabref.cli;

import java.io.File;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Parameters;

@Command(name = "search", description = "Search in a library.")
class Search implements Callable<Integer> {
    @Parameters(index = "0", description = "Search query", arity = "0..1")
    @Option(names = {"--query"}, description = "Search query")
    private String query;

    @Parameters(index = "1", description = "Input BibTeX file", arity = "0..1")
    @Option(names = {"--input"}, description = "Input BibTeX file")
    private File inputFile;

    @Option(names = {"--output"}, description = "Output file")
    private File output;

    @Option(names = {"--output-format"}, description = "Output format: bib, txt, etc.")
    private String outputFormat = "bib"; // FixMe: defaultValue ?

    @Override
    public Integer call() throws Exception {
        // TODO: Implement search functionality
        return 0;
    }
}
