package org.jabref.cli;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "doi-to-bibtex", description = "Converts a DOI to BibTeX")
public class DoiToBibtex implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DoiToBibtex.class);

    @CommandLine.ParentCommand
    private ArgumentProcessor argumentProcessor;

    @CommandLine.Mixin
    private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

    @Parameters(paramLabel = "DOI", description = "one or more DOIs to fetch", arity = "1..*")
    private String[] dois;

    @Override
    public Integer call() {
        CrossRef fetcher = new CrossRef();
        List<BibEntry> entries = new ArrayList<>(dois.length);

        for (String doiString : dois) {
            Optional<DOI> doiParsed = DOI.parse(doiString);
            if (doiParsed.isEmpty()) {
                LOGGER.warn("Skipped DOI {}, because it is not a valid DOI string", doiString);
                System.out.println(Localization.lang("DOI %0 is invalid", doiString));
                System.err.println();
                continue;
            }
            Optional<BibEntry> entry;
            try {
                entry = fetcher.performSearchById(doiParsed.get().asString());
            } catch (FetcherException e) {
                LOGGER.error("Could not fetch BibTeX based on DOI", e);
                System.err.print(Localization.lang("No data was found for the identifier");
                System.err.println(" - " + doiString));
                System.err.println(e.getLocalizedMessage());
                System.err.println();
                continue;
            }

            if (entry.isEmpty()) {
                LOGGER.error("Could not fetch BibTeX based on DOI - entry is empty");
                System.err.print(Localization.lang("No data was found for the identifier");
                System.err.println(" - " + doiString));
                System.err.println();
                continue;
            }

            entries.add(entry.get());
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
            BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(entries));
            BibDatabaseWriter bibWriter = new BibDatabaseWriter(writer, context, argumentProcessor.cliPreferences);
            bibWriter.writeDatabase(context);
        } catch (IOException e) {
            LOGGER.error("Could not write BibTeX", e);
            System.err.println(Localization.lang("Unable to write to %0.", "stdout"));
            return 1;
        }
        return 0;
    }
}
