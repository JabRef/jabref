package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class NormalizeNamesFormatterTest {

    private NormalizeNamesFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new NormalizeNamesFormatter();
    }

    @Test
    public void testNormalizeAuthorList() {
        assertEquals("Bilbo, Staci D.", formatter.format("Staci D Bilbo"));
        assertEquals("Bilbo, Staci D.", formatter.format("Staci D. Bilbo"));

        assertEquals("Bilbo, Staci D. and Smith, S. H. and Schwarz, Jaclyn M.", formatter.format("Staci D Bilbo and Smith SH and Jaclyn M Schwarz"));

        assertEquals("Ølver, M. A.", formatter.format("Ølver MA"));

        assertEquals("Ølver, M. A. and Øie, G. G. and Øie, G. G. and Alfredsen, J. Å. Å. and Alfredsen, Jo and Olsen, Y. Y. and Olsen, Y. Y.",
                formatter.format("Ølver MA; GG Øie; Øie GG; Alfredsen JÅÅ; Jo Alfredsen; Olsen Y.Y. and Olsen YY."));

        assertEquals("Ølver, M. A. and Øie, G. G. and Øie, G. G. and Alfredsen, J. Å. Å. and Alfredsen, Jo and Olsen, Y. Y. and Olsen, Y. Y.",
                formatter.format("Ølver MA; GG Øie; Øie GG; Alfredsen JÅÅ; Jo Alfredsen; Olsen Y.Y.; Olsen YY."));

        assertEquals("Alver, Morten and Alver, Morten O. and Alfredsen, J. A. and Olsen, Y. Y.", formatter.format("Alver, Morten and Alver, Morten O and Alfredsen, JA and Olsen, Y.Y."));

        assertEquals("Alver, M. A. and Alfredsen, J. A. and Olsen, Y. Y.", formatter.format("Alver, MA; Alfredsen, JA; Olsen Y.Y."));

        assertEquals("Kolb, Stefan and Lenhard, J{\\\"o}rg and Wirtz, Guido", formatter.format("Kolb, Stefan and J{\\\"o}rg Lenhard and Wirtz, Guido"));
    }

    @Test
    public void twoAuthorsSeperatedByColon() {
        assertEquals("Bilbo, Staci and Alver, Morten", formatter.format("Staci Bilbo; Morten Alver"));
    }

    @Test
    public void threeAuthorsSeperatedByColon() {
        assertEquals("Bilbo, Staci and Alver, Morten and Name, Test", formatter.format("Staci Bilbo; Morten Alver; Test Name"));
    }

    // Test for https://github.com/JabRef/jabref/issues/318
    @Test
    public void threeAuthorsSeperatedByAnd() {
        assertEquals("Kolb, Stefan and Lenhard, J{\\\"o}rg and Wirtz, Guido", formatter.format("Stefan Kolb and J{\\\"o}rg Lenhard and Guido Wirtz"));
    }

    // Test for https://github.com/JabRef/jabref/issues/318
    @Test
    public void threeAuthorsSeperatedByAndWithDash() {
        assertEquals("Jian, Heng-Yu and Xu, Z. and Chang, M.-C. F.", formatter.format("Heng-Yu Jian and Xu, Z. and Chang, M.-C.F."));
    }

    // Test for https://github.com/JabRef/jabref/issues/318
    @Test
    public void threeAuthorsSeperatedByAndWithLatex() {
        assertEquals("Gustafsson, Oscar and DeBrunner, Linda S. and DeBrunner, Victor and Johansson, H{\\aa}kan",
                formatter.format("Oscar Gustafsson and Linda S. DeBrunner and Victor DeBrunner and H{\\aa}kan Johansson"));
    }

    @Test
    public void lastThenInitial() {
        assertEquals("Smith, S.", formatter.format("Smith S"));
    }

    @Test
    public void lastThenInitials() {
        assertEquals("Smith, S. H.", formatter.format("Smith SH"));
    }

    @Test
    public void initialThenLast() {
        assertEquals("Smith, S.", formatter.format("S Smith"));
    }

    @Test
    public void initialDotThenLast() {
        assertEquals("Smith, S.", formatter.format("S. Smith"));
    }

    @Test
    public void initialsThenLast() {
        assertEquals("Smith, S. H.", formatter.format("SH Smith"));
    }

    @Test
    public void lastThenJuniorThenFirst() {
        assertEquals("Name, della, first", formatter.format("Name, della, first"));
    }

    @Test
    public void testConcatenationOfAuthorsWithCommas() {
        assertEquals("Ali Babar, M. and Dingsøyr, T. and Lago, P. and van der Vliet, H.", formatter.format("Ali Babar, M., Dingsøyr, T., Lago, P., van der Vliet, H."));
        assertEquals("Ali Babar, M.", formatter.format("Ali Babar, M."));
    }

    @Test
    public void testOddCountOfCommas() {
        assertEquals("Ali Babar, M., Dingsøyr T. Lago P.", formatter.format("Ali Babar, M., Dingsøyr, T., Lago P."));
    }

    @Test
    public void formatExample() {
        assertEquals("Einstein, Albert and Turing, Alan", formatter.format(formatter.getExampleInput()));
    }

    @Test
    public void testNameAffixe() {
        assertEquals("Surname, jr, First and Surname2, First2", formatter.format("Surname, jr, First, Surname2, First2"));
    }

    @Test
    public void testAvoidSpecialCharacter() {
        assertEquals("Surname, {, First; Surname2, First2", formatter.format("Surname, {, First; Surname2, First2"));
    }

    @Test
    public void testAndInName() {
        assertEquals("Surname and , First, Surname2 First2", formatter.format("Surname, and , First, Surname2, First2"));
    }

    @Test
    public void testMultipleNameAffixes() {
        assertEquals("Mair, Jr, Daniel and Brühl, Sr, Daniel", formatter.format("Mair, Jr, Daniel, Brühl, Sr, Daniel"));
    }

    @Test
    public void testCommaSeperatedNames() {
        assertEquals("Bosoi, Cristina and Oliveira, Mariana and Sanchez, Rafael Ochoa and Tremblay, Mélanie and TenHave, Gabrie and Deutz, Nicoolas and Rose, Christopher F. and Bemeur, Chantal",
                formatter.format("Cristina Bosoi, Mariana Oliveira, Rafael Ochoa Sanchez, Mélanie Tremblay, Gabrie TenHave, Nicoolas Deutz, Christopher F. Rose, Chantal Bemeur"));
    }

    @Test
    public void testMultipleSpaces() {
        assertEquals("Bosoi, Cristina and Oliveira, Mariana and Sanchez, Rafael Ochoa and Tremblay, Mélanie and TenHave, Gabrie and Deutz, Nicoolas and Rose, Christopher F. and Bemeur, Chantal",
                formatter.format("Cristina    Bosoi,    Mariana Oliveira, Rafael Ochoa Sanchez   ,   Mélanie Tremblay  , Gabrie TenHave, Nicoolas Deutz, Christopher F. Rose, Chantal Bemeur"));
    }

    @Test
    public void testAvoidPreposition() {
        assertEquals("von Zimmer, Hans and van Oberbergern, Michael and zu Berger, Kevin", formatter.format("Hans von Zimmer, Michael van Oberbergern, Kevin zu Berger"));
    }

    @Test
    public void testPreposition() {
        assertEquals("von Zimmer, Hans and van Oberbergern, Michael and zu Berger, Kevin", formatter.format("Hans von Zimmer, Michael van Oberbergern, Kevin zu Berger"));
    }

    @Test
    public void testOneCommaUntouched() {
        assertEquals("Canon der Barbar, Alexander der Große", formatter.format("Canon der Barbar, Alexander der Große"));
    }

    @Test
    public void testAvoidNameAffixes() {
        assertEquals("der Barbar, Canon and der Große, Alexander and der Alexander, Peter", formatter.format("Canon der Barbar, Alexander der Große, Peter der Alexander"));
    }

    @Test
    public void testUpperCaseSensitiveList() {
        assertEquals("der Barbar, Canon and der Große, Alexander", formatter.format("Canon der Barbar AND Alexander der Große"));
        assertEquals("der Barbar, Canon and der Große, Alexander", formatter.format("Canon der Barbar aNd Alexander der Große"));
        assertEquals("der Barbar, Canon and der Große, Alexander", formatter.format("Canon der Barbar AnD Alexander der Große"));
    }

    @Test
    public void testSemiCorrectNamesWithSemicolon() {
        assertEquals("Last, First and Last2, First2 and Last3, First3", formatter.format("Last, First; Last2, First2; Last3, First3"));
        assertEquals("Last, Jr, First and Last2, First2", formatter.format("Last, Jr, First; Last2, First2"));
        assertEquals("Last, First and Last2, First2 and Last3, First3 and Last4, First4", formatter.format("Last, First; Last2, First2; Last3, First3; First4 Last4"));
        assertEquals("Last and Last2, First2 and Last3, First3 and Last4, First4", formatter.format("Last; Last2, First2; Last3, First3; Last4, First4"));
    }
}
