package org.jabref.model.entry;

import java.util.Optional;

import org.jabref.model.entry.identifier.ArXivIdentifier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArXivIdentifierTest {

    @Test
    public void parseIgnoresArXivPrefix() throws Exception {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("arXiv:0710.0994");

        assertEquals(Optional.of(new ArXivIdentifier("0710.0994")), parsed);
    }

    @Test
    public void parseIgnoresArxivPrefix() throws Exception {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("arxiv:0710.0994");

        assertEquals(Optional.of(new ArXivIdentifier("0710.0994")), parsed);
    }
}
