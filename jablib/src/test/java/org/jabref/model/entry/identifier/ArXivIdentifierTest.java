package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArXivIdentifierTest {

    @Test
    void parse() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("0710.0994");

        assertEquals(Optional.of(new ArXivIdentifier("0710.0994")), parsed);
    }

    @Test
    void parseWithArXivPrefix() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("arXiv:0710.0994");

        assertEquals(Optional.of(new ArXivIdentifier("0710.0994")), parsed);
    }

    @Test
    void parseWithArxivPrefix() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("arxiv:0710.0994");

        assertEquals(Optional.of(new ArXivIdentifier("0710.0994")), parsed);
    }

    @Test
    void parseWithClassification() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("0706.0001v1 [q-bio.CB]");

        assertEquals(Optional.of(new ArXivIdentifier("0706.0001", "1", "q-bio.CB")), parsed);
    }

    @Test
    void parseWithArXivPrefixAndClassification() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("arXiv:0706.0001v1 [q-bio.CB]");

        assertEquals(Optional.of(new ArXivIdentifier("0706.0001", "1", "q-bio.CB")), parsed);
    }

    @Test
    void parseOldIdentifier() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("math.GT/0309136");

        assertEquals(Optional.of(new ArXivIdentifier("math.GT/0309136", "math.GT")), parsed);
    }

    @Test
    void acceptLegacyEprint() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("astro-ph.GT/1234567");
        assertEquals(Optional.of(new ArXivIdentifier("astro-ph.GT/1234567", "astro-ph.GT")), parsed);
    }

    @Test
    void acceptLegacyMathEprint() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("math/1234567");
        assertEquals(Optional.of(new ArXivIdentifier("math/1234567", "math")), parsed);
    }

    @Test
    void parseOldIdentifierWithArXivPrefix() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("arXiv:math.GT/0309136");

        assertEquals(Optional.of(new ArXivIdentifier("math.GT/0309136", "math.GT")), parsed);
    }

    @Test
    void parseUrl() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("http://arxiv.org/abs/1502.05795");

        assertEquals(Optional.of(new ArXivIdentifier("1502.05795", "")), parsed);
    }

    @Test
    void parseHttpsUrl() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("https://arxiv.org/abs/1502.05795");

        assertEquals(Optional.of(new ArXivIdentifier("1502.05795", "")), parsed);
    }

    @Test
    void parsePdfUrl() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("http://arxiv.org/pdf/1502.05795");

        assertEquals(Optional.of(new ArXivIdentifier("1502.05795", "")), parsed);
    }

    @Test
    void parseUrlWithVersion() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("http://arxiv.org/abs/1502.05795v1");

        assertEquals(Optional.of(new ArXivIdentifier("1502.05795", "1", "")), parsed);
    }

    @Test
    void parseOldUrlWithVersion() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("http://arxiv.org/pdf/hep-ex/0307015v1");

        assertEquals(Optional.of(new ArXivIdentifier("hep-ex/0307015", "1", "hep-ex")), parsed);
    }

    @Test
    void fourDigitDateIsInvalidInLegacyFormat() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("2017/1118");
        assertEquals(Optional.empty(), parsed);
    }

    @Test
    void acceptPlainEprint() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("0706.0001");
        assertEquals(Optional.of(new ArXivIdentifier("0706.0001")), parsed);
    }

    @Test
    void acceptPlainEprintWithVersion() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("0706.0001v1");
        assertEquals(Optional.of(new ArXivIdentifier("0706.0001", "v1", "")), parsed);
    }

    @Test
    void acceptArxivPrefix() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("arXiv:0706.0001v1");
        assertEquals(Optional.of(new ArXivIdentifier("0706.0001", "v1", "")), parsed);
    }

    @Test
    void ignoreLeadingAndTrailingWhitespaces() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("  0706.0001v1 ");
        assertEquals(Optional.of(new ArXivIdentifier("0706.0001", "v1", "")), parsed);
    }

    @Test
    void rejectEmbeddedEprint() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("other stuff 0706.0001v1 end");
        assertEquals(Optional.empty(), parsed);
    }

    @Test
    void rejectInvalidEprint() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("https://thisisnouri");
        assertEquals(Optional.empty(), parsed);
    }

    @Test
    void acceptUrlHttpEprint() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("http://arxiv.org/abs/0706.0001v1");
        assertEquals(Optional.of(new ArXivIdentifier("0706.0001", "v1", "")), parsed);
    }

    @Test
    void acceptUrlHttpsEprint() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("https://arxiv.org/abs/0706.0001v1");
        assertEquals(Optional.of(new ArXivIdentifier("0706.0001", "v1", "")), parsed);
    }

    @Test
    void rejectUrlOtherDomainEprint() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("https://asdf.org/abs/0706.0001v1");
        assertEquals(Optional.empty(), parsed);
    }

    @Test
    void constructCorrectURLForEprint() throws URISyntaxException {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("0706.0001v1");
        assertEquals(Optional.of(new URI("https://arxiv.org/abs/0706.0001v1")), parsed.get().getExternalURI());
    }

    @Test
    void parseHtmlUrl() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("https://arxiv.org/html/2511.01348v2");

        assertEquals(Optional.of(new ArXivIdentifier("2511.01348", "2", "")), parsed);
    }

    @Test
    void parseHtmlUrlWithoutVersion() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("https://arxiv.org/html/2511.01348");

        assertEquals(Optional.of(new ArXivIdentifier("2511.01348", "", "")), parsed);
    }

    @Test
    void parseHttpHtmlUrl() {
        Optional<ArXivIdentifier> parsed = ArXivIdentifier.parse("http://arxiv.org/html/1502.05795v1");

        assertEquals(Optional.of(new ArXivIdentifier("1502.05795", "1", "")), parsed);
    }

    @Test
    void findsArXivIdentifierInUrl() {
        String text = "https://arxiv.org/html/2503.08641v1#bib.bib5";

        Optional<ArXivIdentifier> identifier = ArXivIdentifier.findInText(text);

        assertTrue(identifier.isPresent());
        assertEquals("2503.08641", identifier.get().getIdentifier());
        assertEquals("1", identifier.get().getVersion());
    }

}
