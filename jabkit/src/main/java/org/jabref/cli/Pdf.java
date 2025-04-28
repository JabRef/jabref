package org.jabref.cli;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

@Command(name = "pdf", description = "Manage PDF metadata.",
        subcommands = {
                Pdf.PdfUpdate.class
                // RemoveComments.class
                // RemoveEmbedded.class
                // RemovePrivateFields.class
        })
class Pdf implements Runnable {
        @Override
        public void run() {
            System.out.println("Specify a subcommand (write-xmp, update).");
        }

    @Command(name = "update", description = "Update linked PDFs with XMP and/or embedded BibTeX.")
    class PdfUpdate implements Callable<Integer> {

        @Option(names = "--format", description = "Format to update (xmp, bibtex-attachment)", split = ",")
        List<String> formats = List.of("xmp", "bibtex-attachment");

        @Option(names = {"-k", "--citation-key"}, description = "Citation keys", required = true)
        List<String> citationKeys;

        @Option(names = "--input", description = "Input .bib file", required = true)
        File inputBib;

        @Option(names = "--update-linked-files", description = "Update linked files automatically.")
        boolean updateLinkedFiles;

        @Override
        public Integer call() {
            // Logic
            return 0;
        }
    }
}
