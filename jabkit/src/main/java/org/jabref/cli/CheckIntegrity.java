package org.jabref.cli;

import java.io.File;

import org.jabref.logic.l10n.Localization;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Parameters;

@Command(name = "check-integrity", description = "Check integrity of the database.")
class CheckIntegrity implements Runnable {

    @Mixin
    private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

    @Parameters(index = "0", description = "BibTeX file to check", arity = "0..1")
    private File inputFile;

    @Option(names = {"--input"}, description = "Input BibTeX file")
    private File inputOption;

    @Option(names = {"--output-format"}, description = "Output format: txt or csv")
    private String outputFormat = "txt"; // FixMe: Default value?

    @Override
    public void run() {
        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Checking integrity of '%0'.", inputFile));
            System.out.flush();
        }

        // TODO: Implement integrity checking
    }
}
