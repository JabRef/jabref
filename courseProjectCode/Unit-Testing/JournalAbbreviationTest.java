import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JournalAbbreviationRepositoryEdgeTest {

    @Test
    void noMatch_returnsOriginal() {
        var repo = new JournalAbbreviationRepository(List.of(
                new Abbreviation("Journal of Physics A", "J. Phys. A", "JPA")
        ));
        String input = "Completely Unknown";
        assertEquals(input, repo.abbreviate(input));
        assertEquals(input, repo.unabbreviate("XYZ"));
    }

    @Test
    void multipleSameScore_tieBreakDeterministic() {
        var repo = new JournalAbbreviationRepository(List.of(
                new Abbreviation("Alpha Beta", "A. B.", "AB"),
                new Abbreviation("Alpha Beta Journal", "A. B. J.", "ABJ")
        ));
        String r1 = repo.abbreviate("Alpha Beta");
        String r2 = repo.abbreviate("Alpha Beta");
        assertEquals(r1, r2);
    }


    @Test
    void trimsWhitespaceAndPunctuation() {
        var repo = new JournalAbbreviationRepository(List.of(
                new Abbreviation("Journal of Physics", "J. Phys.", "JP")
        ));
        assertEquals("J. Phys.", repo.abbreviate("  Journal of Physics , "));
    }
}


