package org.jabref.cli;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

@Command(name = "fetch", description = "Fetch entries from a provider.")
class Fetch implements Runnable {
    @Option(names = "--provider", required = true)
    String provider;

    @Option(names = "--query")
    String query;

    @Option(names = "--output", required = true)
    String output;

    @Option(names = "--append", description = "Append to existing file")
    boolean append;

    @Override
    public void run() {
        // TODO: implement
    }
}
