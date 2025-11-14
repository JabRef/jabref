package org.jabref.jablib.examples;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;

import org.jabref.model.entry.BibEntry;

import org.tinylog.Logger;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Converts a DOI to BibTeX
 */
public class App {
    public static void main(String[] args) {
        String doi;
        if (args.length == 1) {
            doi = args[0];
        } else {
            doi = "10.47397/tb/44-3/tb138kopp-jabref";
        }
        JabRefCliPreferences preferences = JabRefCliPreferences.getInstance();

        // All `IdParserFetcher<DOI>` can do. In JabRef, there is currently only one implemented

        CrossRef fetcher = new CrossRef();
        BibEntry entry = null; // will throw an exception if not found
        try {
            entry = fetcher.performSearchById(doi).get();
        } catch (FetcherException e) {
            Logger.error("Could not fetch entry", e);
            return;
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
            BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(entry)));
            BibDatabaseWriter bibWriter = new BibDatabaseWriter(writer, context, preferences);
            bibWriter.writeDatabase(context);
        } catch (IOException e) {
            Logger.error("Could not write library", e);
        }
    }
}
