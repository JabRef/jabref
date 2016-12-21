package net.sf.jabref.logic.importer.fetcher;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedParserFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.Parser;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.ISBN;

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
            throw new FetcherException(Localization.lang("Invalid_ISBN:_'%0'.", identifier));
        }
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences);
    }

}
