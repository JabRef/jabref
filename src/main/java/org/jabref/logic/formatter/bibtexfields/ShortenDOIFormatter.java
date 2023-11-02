package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.importer.util.ShortDOIService;
import org.jabref.logic.importer.util.ShortDOIServiceException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShortenDOIFormatter extends Formatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShortenDOIFormatter.class);
    private static final Predicate<String> SHORT_DOI_FORMAT = Pattern.compile("^10/[a-zA-Z0-9]+$").asPredicate();

    @Override
    public String getName() {
        return Localization.lang("Shorten DOI");
    }

    @Override
    public String getKey() {
        return "short_doi";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);
        return SHORT_DOI_FORMAT.test(value) ? value : DOI.parse(value)
                  .map(doi -> {
                      try {
                          return new ShortDOIService().getShortDOI(doi).getDOI();
                      } catch (ShortDOIServiceException e) {
                          LOGGER.error(e.getMessage(), e);
                          return value;
                      }
                  }).orElse(value);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Shortens DOI to more human readable form.");
    }

    @Override
    public String getExampleInput() {
        return "10.1006/jmbi.1998.2354";
    }
}
