package org.jabref.cli;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

@Command(name = "generate-bib-from-aux", description = "Generate small bib from aux file.")
class GenerateBibFromAux implements Runnable {
    @Option(names = "--aux", required = true)
    String aux;

    @Option(names = "--input", required = true)
    String input;

    @Option(names = "--output", required = true)
    String output;

    @Override
    public void run() {
        // TODO: implement
    }
}
