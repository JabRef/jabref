package org.jabref.logic.importer.fetcher;

import java.net.URISyntaxException;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.identifier.ISBN;

public abstract class AbstractIsbnFetcher implements IdBasedParserFetcher {

    protected final ImportFormatPreferences importFormatPreferences;

    public AbstractIsbnFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_ISBN);
    }

    /**
     * @throws URISyntaxException if the ISBN is invalid
     * @implNote We could have created a new exception (which causes much implementation efforts) or we could have used "FetcherException", which is currently more used for I/O errors than syntax errors (thus also more WTF). Moreover, a ISBN is "kind of" an URI (even if the isbn: prefix is missing)
     */
    protected void ensureThatIsbnIsValid(String identifier) throws URISyntaxException {
        ISBN isbn = new ISBN(identifier);
        if (!isbn.isValid()) {
            throw new URISyntaxException(identifier, "Invalid ISBN");
        }
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences);
    }
}
