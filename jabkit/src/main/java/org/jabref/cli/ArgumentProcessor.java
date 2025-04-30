package org.jabref.cli;

import java.util.List;
import java.util.Objects;

import javafx.util.Pair;

import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DummyFileUpdateMonitor;

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

    private final CliPreferences cliPreferences;
    private final BibEntryTypesManager entryTypesManager;

    private final CommandLine cli;

    public ArgumentProcessor(CliPreferences cliPreferences,
                             BibEntryTypesManager entryTypesManager) {
        this.cliPreferences = cliPreferences;
        this.entryTypesManager = entryTypesManager;

        KitCommandLine kitCli = new KitCommandLine(cliPreferences, entryTypesManager);
        cli = new CommandLine(kitCli);
    }

    public void processArguments(String[] args) {
        cli.execute(args);


    }
        /*
        // Check if we should reset all preferences to default values:
        if (cli.isPreferencesReset()) {
            resetPreferences(cli.getPreferencesReset());
        }

        // Check if we should import preferences from a file:
        if (cli.isPreferencesImport()) {
            importPreferences();
        }

        List<ParserResult> loaded = importAndOpenFiles();

        if (cli.isFetcherEngine()) {
            fetch(cli.getFetcherEngine()).ifPresent(loaded::add);
        }

        if (cli.isGenerateCitationKeys()) {
            regenerateCitationKeys(loaded);
        }

        if ((cli.isWriteXmpToPdf() && cli.isEmbedBibFileInPdf()) || (cli.isWriteMetadataToPdf() && (cli.isWriteXmpToPdf() || cli.isEmbedBibFileInPdf()))) {
            System.err.println("Give only one of [writeXmpToPdf, embedBibFileInPdf, writeMetadataToPdf]");
        }

        if (cli.isWriteMetadataToPdf() || cli.isWriteXmpToPdf() || cli.isEmbedBibFileInPdf()) {
            if (!loaded.isEmpty()) {
                writeMetadataToPdf(loaded,
                        cli.getWriteMetadataToPdf(),
                        cliPreferences.getXmpPreferences(),
                        cliPreferences.getFilePreferences(),
                        cliPreferences.getLibraryPreferences().getDefaultBibDatabaseMode(),
                        cliPreferences.getCustomEntryTypesRepository(),
                        cliPreferences.getFieldPreferences(),
                        Injector.instantiateModelOrService(JournalAbbreviationRepository.class),
                        cli.isWriteXmpToPdf() || cli.isWriteMetadataToPdf(),
                        cli.isEmbedBibFileInPdf() || cli.isWriteMetadataToPdf());
            }
        }

        if (cli.isFileExport()) {
            if (!loaded.isEmpty()) {
                exportFile(loaded, cli.getFileExport().split(","));
                LOGGER.debug("Finished export");
            } else {
                System.err.println(Localization.lang("The output option depends on a valid import option."));
            }
        }
    }
*/

    /**
     * @return List of opened files (could be .bib, but also other formats). May also contain error results.
     */
/*    private List<ParserResult> importAndOpenFiles() {
        List<ParserResult> loaded = new ArrayList<>();
        List<String> toImport = new ArrayList<>();
        if (!cli.getLeftOver().isEmpty()) {
            for (String aLeftOver : cli.getLeftOver()) {
                // Leftover arguments that have a "bib" extension are interpreted as
                // BIB files to open. Other files, and files that could not be opened
                // as bib, we try to import instead.
                boolean bibExtension = aLeftOver.toLowerCase(Locale.ENGLISH).endsWith("bib");

                ParserResult pr = new ParserResult();
                if (bibExtension) {
                    try {
                        pr = OpenDatabase.loadDatabase(
                                Path.of(aLeftOver),
                                cliPreferences.getImportFormatPreferences(),
                                fileUpdateMonitor);
                        // In contrast to org.jabref.gui.LibraryTab.onDatabaseLoadingSucceed, we do not execute OpenDatabaseAction.performPostOpenActions(result, dialogService);
                    } catch (IOException ex) {
                        pr = ParserResult.fromError(ex);
                        LOGGER.error("Error opening file '{}'", aLeftOver, ex);
                    }
                }

                if (!bibExtension || (pr.isEmpty())) {
                    // We will try to import this file. Normally we
                    // will import it into a new tab, but if this import has
                    // been initiated by another instance through the remote
                    // listener, we will instead import it into the current library.
                    // This will enable easy integration with web browsers that can
                    // open a reference file in JabRef.
                    if (startupMode == Mode.INITIAL_START) {
                        toImport.add(aLeftOver);
                    } else {
                        loaded.add(importToOpenBase(aLeftOver).orElse(new ParserResult()));
                    }
                } else {
                    loaded.add(pr);
                }
            }
        }

        return loaded;
    }
*/
}
