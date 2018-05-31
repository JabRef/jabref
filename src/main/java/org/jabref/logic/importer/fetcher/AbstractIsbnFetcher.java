package org.jabref.logic.importer.fetcher;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.util.DummyFileUpdateMonitor;

public abstract class AbstractIsbnFetcher implements IdBasedParserFetcher {

    protected final ImportFormatPreferences importFormatPreferences;

    public AbstractIsbnFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_ISBN;
    }

    protected void ensureThatIsbnIsValid(String identifier) throws FetcherException {
        ISBN isbn = new ISBN(identifier);
        if (!isbn.isValid()) {
            throw new FetcherException(Localization.lang("Invalid ISBN: '%0'.", identifier));
        }
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
    }

}
