package org.jabref.gui.newentry;
import java.util.Optional;

import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.entry.identifier.Identifier;
import org.jabref.model.entry.identifier.RFC;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NewEntryViewIdentifierDetectionTest {

    @Test
    void detectsValidDOI() {
        Optional<Identifier> result = CompositeIdFetcher.getIdentifier("10.1109/MCOM.2010.5673082");
        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof DOI);
        assertTrue(DOI.isValid(result.get().asString()));
    }

    @Test
    void rejectsInvalidDOI() {
        Optional<Identifier> result = CompositeIdFetcher.getIdentifier("123456789");
        assertTrue(result.isEmpty() || (result.get() instanceof DOI && !DOI.isValid(result.get().asString())));
    }

    @Test
    void detectsValidISBN() {
        Optional<Identifier> result = CompositeIdFetcher.getIdentifier("9780134685991");
        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof ISBN);
        assertTrue(((ISBN) result.get()).isValid());
    }

    @Test
    void detectsValidArXiv() {
        Optional<Identifier> result = CompositeIdFetcher.getIdentifier("arXiv:1706.03762");
        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof ArXivIdentifier);
    }

    @Test
    void detectsValidRFC() {
        Optional<Identifier> result = CompositeIdFetcher.getIdentifier("rfc2616");
        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof RFC);
    }
}

