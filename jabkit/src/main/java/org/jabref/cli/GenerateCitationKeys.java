package org.jabref.cli;

import java.io.File;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Parameters;

@Command(name = "generate-citation-keys", description = "Generate citation keys for entries in a .bib file.")
public class GenerateCitationKeys implements Callable<Integer> {
    @Parameters(index = "0", description = "The input .bib file.")
    File inputFile;

    @Override
    public Integer call() {
        // Logic here
        return 0;
    }
}
