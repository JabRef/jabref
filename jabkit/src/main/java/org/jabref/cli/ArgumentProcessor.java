package org.jabref.cli;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 jabkit generate-citation-keys Chocolate.bib
 jabkit --help
 jabkit --version
 jabkit --debug

 jabkit check-consistency Chocolate.bib
 jabkit check-consistency --input Chocolate.bib --output-format csv
 jabkit check-consistency --input Chocolate.bib --output-format txt

 jabkit check-integrity Chocolate.bib

 # Overwrite
 jabkit fetch --provider Medline/PubMed --query cancer --output Chocolate.bib
 # Append - similar to "--import to upen"
 jabkit fetch --provider Medline/PubMed --query cancer --output Chocolate.bib --append
 # query is read from stdin if not provided

 // Search within the library
 // OLD: jabkit export-matches --from db.bib -m author=Newton,search.htm,html
 // Place target at the end
 // search.g4 / https://docs.jabref.org/finding-sorting-and-cleaning-entries/search
 jabkit search <searchstring> Chocolate.bib
 jabkit search --query <searchstring> --input Chocolate.bib --output newfile.bib
 // no --input: stdin
 // no --output: stdout
 // --output-format medline # otherwise: auto detected
 // --input-format ris      # otherwise: auto detected

 // standard format: Output matched citation keys a list
 // sno
 jabkit search --query <searchstring> --input Chocolate.bib
 jabkit search <searchstring> Chocolate.bib --output-format <format>
 jabkit search --query <searchstring> --input Chocolate.bib --output-format <format> --output newfile.txt

 // similarily: convert
 jabkit convert a.ris b.bib
 jabkit convert --input a.ris --output b.bib

 // Localization.lang("Sublibrary from AUX to BibTeX")
 jabkit generate-bib-from-aux --aux thesis.aux --input thesis.bib --output small.bib

 # Reset preferences
 jabkit preferences reset
 # Import preferences from a file
 jabkit preferences import <filename>
 # Export preferences to a file
 jabkit preferences export <filename>


 # Write BibTeX data into PDF file
 jabkit pdf write-xmp --citation-key key1 --citation-key key2 --input Chocolate.bib --output paper.pdf
 also: jabkit pdf write-xmp -k key1 -k key2

 # takes Chocolate.bib, searches the citatoin-keys, looks up linked files and writes xmp
 # Description?
 jabkit pdf update --format=xmp --citation-key key1 --citation-key key2 --update-linked-files --input Chocolate.bib
 jabkit pdf update --format=xmp --format=bibtex-attachment --citation-key key1 --citation-key key2 --update-linked-files --input Chocolate.bib
 # default: all formats: xmp and bibtex-attachment
 # implementation: open Chocolate.bib, search for key1 and key2 (List<BibEntry>), search for linked files - map linked files to BibEntry, for each map entry: execute update action (xmp and/or embed-bibtex)

 # NOT jabkit pdf update-embedded-bibtex (reminder: as above)

 # jabkit pdf embed-metadata
 # NOT CHOSEN:jabkit pdf embed --format=xmp --format=bibtex --citation-key key1 --citation-key key2 --update-linked-files --input Chocolate.bib

 # updates all linked files (only ommitting -k leads to error)
 # NOT jabkit pdf write-xmp --all --update-linked-files --input Chocolate.bib

 // .desc(Localization.lang("Script-friendly output"))
 jabkit <whateveraction> --porcelain
 */
public class ArgumentProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentProcessor.class);

    private final CommandLine cli;

    public ArgumentProcessor(CliPreferences cliPreferences,
                             BibEntryTypesManager entryTypesManager) {

        KitCommandLine kitCli = new KitCommandLine(cliPreferences, entryTypesManager);
        cli = new CommandLine(kitCli);
    }

    public void processArguments(String[] args) {
        cli.execute(args);
    }
}
