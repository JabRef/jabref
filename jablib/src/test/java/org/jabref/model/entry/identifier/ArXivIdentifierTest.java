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

    // Tests for findInText method

    @Test
    void findInTextWithPlainIdentifier() {
        Optional<ArXivIdentifier> found = ArXivIdentifier.findInText("Check out this paper 2503.08641 for more details");

        assertEquals(Optional.of(new ArXivIdentifier("2503.08641")), found);
    }

    @Test
    void findInTextWithArxivUrl() {
        Optional<ArXivIdentifier> found = ArXivIdentifier.findInText("See https://arxiv.org/abs/2503.08641v1 for the paper");

        assertEquals(Optional.of(new ArXivIdentifier("2503.08641", "1", "")), found);
    }

    @Test
    void findInTextWithHtmlUrl() {
        Optional<ArXivIdentifier> found = ArXivIdentifier.findInText("Reference: https://arxiv.org/html/2503.08641v1#bib.bib5");

        assertEquals(Optional.of(new ArXivIdentifier("2503.08641", "1", "")), found);
    }

    @Test
    void findInTextWithArxivPrefix() {
        Optional<ArXivIdentifier> found = ArXivIdentifier.findInText("The paper arXiv:2503.08641 discusses this topic");

        assertEquals(Optional.of(new ArXivIdentifier("2503.08641")), found);
    }

    @Test
    void findInTextWithOldStyleIdentifier() {
        Optional<ArXivIdentifier> found = ArXivIdentifier.findInText("See hep-th/9901001 for the original work");

        assertEquals(Optional.of(new ArXivIdentifier("hep-th/9901001", "hep-th")), found);
    }

    @Test
    void findInTextWithOldStyleUrl() {
        Optional<ArXivIdentifier> found = ArXivIdentifier.findInText("Check https://arxiv.org/abs/hep-ex/0307015v1 for details");

        assertEquals(Optional.of(new ArXivIdentifier("hep-ex/0307015", "1", "hep-ex")), found);
    }

    @Test
    void findInTextReturnsEmptyForNoIdentifier() {
        Optional<ArXivIdentifier> found = ArXivIdentifier.findInText("This text contains no arXiv identifier");

        assertEquals(Optional.empty(), found);
    }

    @Test
    void findInTextReturnsEmptyForNull() {
        Optional<ArXivIdentifier> found = ArXivIdentifier.findInText(null);

        assertEquals(Optional.empty(), found);
    }

    @Test
    void findInTextReturnsEmptyForBlank() {
        Optional<ArXivIdentifier> found = ArXivIdentifier.findInText("   ");

        assertEquals(Optional.empty(), found);
    }

    @Test
    void findInTextWithVersionInMiddleOfText() {
        Optional<ArXivIdentifier> found = ArXivIdentifier.findInText("Paper 1502.05795v2 was published last year");

        assertEquals(Optional.of(new ArXivIdentifier("1502.05795", "2", "")), found);
    }

    @Test
    void findInTextWithFiveDigitIdentifier() {
        Optional<ArXivIdentifier> found = ArXivIdentifier.findInText("See arXiv:2301.12345 for more");

        assertEquals(Optional.of(new ArXivIdentifier("2301.12345")), found);
    }
}
