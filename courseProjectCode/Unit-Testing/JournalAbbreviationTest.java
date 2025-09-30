import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JournalAbbreviationRepositoryEdgeTest {

    //Swen 777 test
    //Swen 777 test
    @Test
    void noMatch_returnsOriginal() {
        repository.addCustomAbbreviations(List.of(new Abbreviation("Journal of Physics A", "J. Phys. A", "JPA")));

        String input = "Completely Unknown";
        assertTrue(repository.getDefaultAbbreviation(input).isEmpty());
        assertTrue(repository.getDefaultAbbreviation("XYZ").isEmpty());
    }

    //Swen 777 test
    @Test
    void multipleSameScore_tieBreakDeterministic() {

        repository.addCustomAbbreviations(List.of(
                new Abbreviation("Alpha Beta", "A. B.", "AB"),
                new Abbreviation("Alpha Beta Journal", "A. B. J.", "ABJ")
        ));
        String r1 = repository.getDefaultAbbreviation("Alpha Beta").orElse("WRONG");
        String r2 = repository.getDefaultAbbreviation("Alpha Beta").orElse("WRONG");
        assertEquals(r1, r2);
    }

    //Swen 777 test
    @Test
    void trimsWhitespaceAndPunctuation() {
        repository.addCustomAbbreviation(new Abbreviation("Journal of Physics", "J. Phys.", "JP"));
        assertEquals("J. Phys.", repository.getDefaultAbbreviation("  Journal of Physics , ").orElse("WRONG"));
    }
}


