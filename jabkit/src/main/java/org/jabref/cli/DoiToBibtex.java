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

    @Parameters(paramLabel = "DOI", description = "one or more DOIs to fetch", arity = "1..*")
    private DOI[] dois;

    @Override
    public Integer call() {
        var fetcher = new CrossRef();
        List<BibEntry> entries = new ArrayList<>(dois.length);

        for (DOI doi : dois) {
            Optional<BibEntry> entry;
            try {
                entry = fetcher.performSearchById(doi.asString());
            } catch (FetcherException e) {
                LOGGER.error("Could not fetch DOI from BibTeX", e);
                continue;
            }

            if (entry.isEmpty()) {
                LOGGER.error("Could not fetch DOI from BibTeX");
                continue;
            }

            entries.add(entry.get());
        }

        try (var writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
            var context = new BibDatabaseContext(new BibDatabase(entries));
            var bibWriter = new BibDatabaseWriter(writer, context, argumentProcessor.cliPreferences);
            bibWriter.writeDatabase(context);
        } catch (IOException e) {
            LOGGER.error("Could not write BibTeX", e);
            return 1;
        }
        return 0;
    }
}
