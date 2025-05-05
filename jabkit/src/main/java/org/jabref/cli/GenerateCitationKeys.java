package org.jabref.cli;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "generate-citation-keys", description = "Generate citation keys for entries in a .bib file.")
public class GenerateCitationKeys implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateCitationKeys.class);
    @ParentCommand
    private KitCommandLine kitCommandLine;

    @Option(names = "--input", description = "The input .bib file.", required = true)
    private Path inputFile;

    @Option(names = "--output", description = "The output .bib file.")
    private Path outputFile;

    @Override
    public Integer call() {
        Optional<ParserResult> parserResult = KitCommandLine.importFile(kitCommandLine.cliPreferences, inputFile, "bibtex");
        if (parserResult.isPresent()) {
            BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();

            LOGGER.info(Localization.lang("Regenerating citation keys according to metadata"));

            CitationKeyGenerator keyGenerator = new CitationKeyGenerator(
                    databaseContext,
                    kitCommandLine.cliPreferences.getCitationKeyPatternPreferences());
            for (BibEntry entry : databaseContext.getEntries()) {
                keyGenerator.generateAndSetKey(entry);
            }
        }

        if (outputFile != null) {
            KitCommandLine.saveDatabase(kitCommandLine.cliPreferences, kitCommandLine.entryTypesManager, parserResult.get().getDatabase(), outputFile);
        } else {
            System.out.println(parserResult.get().getDatabase());
        }

        return 0;
    }
}
