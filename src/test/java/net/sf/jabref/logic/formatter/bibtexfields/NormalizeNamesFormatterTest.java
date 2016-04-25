package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class NormalizeNamesFormatterTest {

    private final NormalizeNamesFormatter formatter = new NormalizeNamesFormatter();

    @Test
    public void testNormalizeAuthorList() {
        expectCorrect("Staci D Bilbo", "Bilbo, Staci D.");
        expectCorrect("Staci D. Bilbo", "Bilbo, Staci D.");

        expectCorrect("Staci D Bilbo and Smith SH and Jaclyn M Schwarz", "Bilbo, Staci D. and Smith, S. H. and Schwarz, Jaclyn M.");

        expectCorrect("Ølver MA", "Ølver, M. A.");

        expectCorrect("Ølver MA; GG Øie; Øie GG; Alfredsen JÅÅ; Jo Alfredsen; Olsen Y.Y. and Olsen YY.",
                "Ølver, M. A. and Øie, G. G. and Øie, G. G. and Alfredsen, J. Å. Å. and Alfredsen, Jo and Olsen, Y. Y. and Olsen, Y. Y.");

        expectCorrect("Ølver MA; GG Øie; Øie GG; Alfredsen JÅÅ; Jo Alfredsen; Olsen Y.Y.; Olsen YY.",
                "Ølver, M. A. and Øie, G. G. and Øie, G. G. and Alfredsen, J. Å. Å. and Alfredsen, Jo and Olsen, Y. Y. and Olsen, Y. Y.");

        expectCorrect("Alver, Morten and Alver, Morten O and Alfredsen, JA and Olsen, Y.Y.", "Alver, Morten and Alver, Morten O. and Alfredsen, J. A. and Olsen, Y. Y.");

        expectCorrect("Alver, MA; Alfredsen, JA; Olsen Y.Y.", "Alver, M. A. and Alfredsen, J. A. and Olsen, Y. Y.");

        expectCorrect("Kolb, Stefan and J{\\\"o}rg Lenhard and Wirtz, Guido", "Kolb, Stefan and Lenhard, J{\\\"o}rg and Wirtz, Guido");
    }

    @Test
    public void twoAuthorsSeperatedByColon() {
        expectCorrect("Staci Bilbo; Morten Alver", "Bilbo, Staci and Alver, Morten");
    }

    @Test
    public void threeAuthorsSeperatedByColon() {
        expectCorrect("Staci Bilbo; Morten Alver; Test Name", "Bilbo, Staci and Alver, Morten and Name, Test");
    }

    // Test for https://github.com/JabRef/jabref/issues/318
    @Test
    public void threeAuthorsSeperatedByAnd() {
        expectCorrect("Stefan Kolb and J{\\\"o}rg Lenhard and Guido Wirtz", "Kolb, Stefan and Lenhard, J{\\\"o}rg and Wirtz, Guido");
    }

    // Test for https://github.com/JabRef/jabref/issues/318
    @Test
    public void threeAuthorsSeperatedByAndWithDash() {
        expectCorrect("Heng-Yu Jian and Xu, Z. and Chang, M.-C.F.", "Jian, Heng-Yu and Xu, Z. and Chang, M.-C. F.");
    }

    // Test for https://github.com/JabRef/jabref/issues/318
    @Test
    public void threeAuthorsSeperatedByAndWithLatex() {
        expectCorrect("Oscar Gustafsson and Linda S. DeBrunner and Victor DeBrunner and H{\\aa}kan Johansson", "Gustafsson, Oscar and DeBrunner, Linda S. and DeBrunner, Victor and Johansson, H{\\aa}kan");
    }

    @Test
    public void lastThenInitial() {
        expectCorrect("Smith S", "Smith, S.");
    }

    @Test
    public void lastThenInitials() {
        expectCorrect("Smith SH", "Smith, S. H.");
    }

    @Test
    public void initialThenLast() {
        expectCorrect("S Smith", "Smith, S.");
    }

    @Test
    public void initialDotThenLast() {
        expectCorrect("S. Smith", "Smith, S.");
    }

    @Test
    public void initialsThenLast() {
        expectCorrect("SH Smith", "Smith, S. H.");
    }

    @Test
    public void lastThenJuniorThenFirst() {
        expectCorrect("Name, della, first", "Name, della, first");
    }

    private void expectCorrect(String input, String expected) {
        Assert.assertEquals(expected, formatter.format(input));
    }

    @Test
    public void formatExample() {
        assertEquals("Einstein, Albert and Turing, Alan", formatter.format(formatter.getExampleInput()));
    }

}