package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Transforms a JabRef search query into DNB's SRU/CQL query syntax.
///
/// @see [DNB SRU documentation](https://www.dnb.de/EN/Professionell/Metadatendienste/Datenbezug/SRU/sru_node.html#doc250692bodyText8)
@NullMarked
public class DnbQueryTransformer extends AbstractQueryTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnbQueryTransformer.class);

    @Override
    protected String getLogicalAndOperator() {
        return " and ";
    }

    @Override
    protected String getLogicalOrOperator() {
        return " or ";
    }

    @Override
    protected String getLogicalNotOperator() {
        return " not ";
    }

    @Override
    protected String handleAuthor(String author) {
        return createKeyValuePair("atr", author, "=");
    }

    @Override
    protected String handleTitle(String title) {
        return createKeyValuePair("tit", title, "=");
    }

    @Override
    protected String handleJournal(String journalTitle) {
        LOGGER.debug("DnbQueryTransformer does not support journal-title search in the DNB bibliographic catalogue");
        return "";
    }

    @Override
    protected String handleDoi(String doi) {
        return "num=\"" + doi + "\"";
    }

    @Override
    protected String handleYear(String year) {
        return createKeyValuePair("jhr", year, "=");
    }

    private static String quoteIfNeeded(String value) {
        return value.contains(" ") ? "\"" + value + "\"" : value;
    }

    /// DNB's CQL parser wants =(equal), not :(colon) as separator
    @Override
    protected Optional<String> handleOtherField(String fieldAsString, String term) {
        return Optional.of(createKeyValuePair(fieldAsString, term));
    }

    protected String createKeyValuePair(String fieldAsString, String term) {
        return createKeyValuePair(fieldAsString, term, "=");
    }
}
