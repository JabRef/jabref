package org.jabref.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Parameters;

@Command(name = "import", aliases = {"--import"}, description = "Import bibliography files.")
public class ImportSubCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportSubCommand.class);

    @Option(names = {"--format"}, description = "Import format")
    public String importFormat;

    @Option(names = {"--add"}, description = "Add to currently opened library")
    public boolean append;

    @Parameters(paramLabel = "<FILE>", description = "File to be imported.", arity = "1")
    public String inputFile;

}
