package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class NormalizeNamesFormatterTest {

    private NormalizeNamesFormatter formatter;

    @Before
    public void setUp() {
        formatter = new NormalizeNamesFormatter();
    }

    @Test
    public void testNormalizeAuthorList() {
        Assert.assertEquals("Bilbo, Staci D.", formatter.format("Staci D Bilbo"));
        Assert.assertEquals("Bilbo, Staci D.", formatter.format("Staci D. Bilbo"));

        Assert.assertEquals("Bilbo, Staci D. and Smith, S. H. and Schwarz, Jaclyn M.", formatter.format("Staci D Bilbo and Smith SH and Jaclyn M Schwarz"));

        Assert.assertEquals("Ølver, M. A.", formatter.format("Ølver MA"));

        Assert.assertEquals("Ølver, M. A. and Øie, G. G. and Øie, G. G. and Alfredsen, J. Å. Å. and Alfredsen, Jo and Olsen, Y. Y. and Olsen, Y. Y.",
                formatter.format("Ølver MA; GG Øie; Øie GG; Alfredsen JÅÅ; Jo Alfredsen; Olsen Y.Y. and Olsen YY."));

        Assert.assertEquals("Ølver, M. A. and Øie, G. G. and Øie, G. G. and Alfredsen, J. Å. Å. and Alfredsen, Jo and Olsen, Y. Y. and Olsen, Y. Y.",
                formatter.format("Ølver MA; GG Øie; Øie GG; Alfredsen JÅÅ; Jo Alfredsen; Olsen Y.Y.; Olsen YY."));

        Assert.assertEquals("Alver, Morten and Alver, Morten O. and Alfredsen, J. A. and Olsen, Y. Y.", formatter.format("Alver, Morten and Alver, Morten O and Alfredsen, JA and Olsen, Y.Y."));

        Assert.assertEquals("Alver, M. A. and Alfredsen, J. A. and Olsen, Y. Y.", formatter.format("Alver, MA; Alfredsen, JA; Olsen Y.Y."));

        Assert.assertEquals("Kolb, Stefan and Lenhard, J{\\\"o}rg and Wirtz, Guido", formatter.format("Kolb, Stefan and J{\\\"o}rg Lenhard and Wirtz, Guido"));
    }

    @Test
    public void twoAuthorsSeperatedByColon() {
        Assert.assertEquals("Bilbo, Staci and Alver, Morten", formatter.format("Staci Bilbo; Morten Alver"));
    }

    @Test
    public void threeAuthorsSeperatedByColon() {
        Assert.assertEquals("Bilbo, Staci and Alver, Morten and Name, Test", formatter.format("Staci Bilbo; Morten Alver; Test Name"));
    }

    // Test for https://github.com/JabRef/jabref/issues/318
    @Test
    public void threeAuthorsSeperatedByAnd() {
        Assert.assertEquals("Kolb, Stefan and Lenhard, J{\\\"o}rg and Wirtz, Guido", formatter.format("Stefan Kolb and J{\\\"o}rg Lenhard and Guido Wirtz"));
    }

    // Test for https://github.com/JabRef/jabref/issues/318
    @Test
    public void threeAuthorsSeperatedByAndWithDash() {
        Assert.assertEquals("Jian, Heng-Yu and Xu, Z. and Chang, M.-C. F.", formatter.format("Heng-Yu Jian and Xu, Z. and Chang, M.-C.F."));
    }

    // Test for https://github.com/JabRef/jabref/issues/318
    @Test
    public void threeAuthorsSeperatedByAndWithLatex() {
        Assert.assertEquals("Gustafsson, Oscar and DeBrunner, Linda S. and DeBrunner, Victor and Johansson, H{\\aa}kan",
                formatter.format("Oscar Gustafsson and Linda S. DeBrunner and Victor DeBrunner and H{\\aa}kan Johansson"));
    }

    @Test
    public void lastThenInitial() {
        Assert.assertEquals("Smith, S.", formatter.format("Smith S"));
    }

    @Test
    public void lastThenInitials() {
        Assert.assertEquals("Smith, S. H.", formatter.format("Smith SH"));
    }

    @Test
    public void initialThenLast() {
        Assert.assertEquals("Smith, S.", formatter.format("S Smith"));
    }

    @Test
    public void initialDotThenLast() {
        Assert.assertEquals("Smith, S.", formatter.format("S. Smith"));
    }

    @Test
    public void initialsThenLast() {
        Assert.assertEquals("Smith, S. H.", formatter.format("SH Smith"));
    }

    @Test
    public void lastThenJuniorThenFirst() {
        Assert.assertEquals("Name, della, first", formatter.format("Name, della, first"));
    }

    @Test
    public void testConcatenationOfAuthorsWithCommas() {
        Assert.assertEquals("Ali Babar, M. and Dingsøyr, T. and Lago, P. and van der Vliet, H.", formatter.format("Ali Babar, M., Dingsøyr, T., Lago, P., van der Vliet, H."));
        Assert.assertEquals("Ali Babar, M.", formatter.format("Ali Babar, M."));
    }

    @Test
    public void testOddCountOfCommas() {
        Assert.assertEquals("Ali Babar, M., Dingsøyr T. Lago P.", formatter.format("Ali Babar, M., Dingsøyr, T., Lago P."));
    }

    @Test
    public void formatExample() {
        assertEquals(formatter.format(formatter.getExampleInput()), "Einstein, Albert and Turing, Alan");
    }

    @Test
    public void testNameAffixe() {
        Assert.assertEquals("Surname, jr, First and Surname2, First2", formatter.format("Surname, jr, First, Surname2, First2"));
    }

    @Test
    public void testAvoidSpecialCharacter() {
        Assert.assertEquals("Surname, {, First; Surname2, First2", formatter.format("Surname, {, First; Surname2, First2"));
    }

    @Test
    public void testAndInName() {
        Assert.assertEquals("Surname and , First, Surname2 First2", formatter.format("Surname, and , First, Surname2, First2"));
    }

    @Test
    public void testMultipleNameAffixes() {
        Assert.assertEquals("Mair, Jr, Daniel and Brühl, Sr, Daniel", formatter.format("Mair, Jr, Daniel, Brühl, Sr, Daniel"));
    }

    @Test
    public void testCommaSeperatedNames() {
        Assert.assertEquals("Bosoi, Cristina and Oliveira, Mariana and Sanchez, Rafael Ochoa and Tremblay, Mélanie and TenHave, Gabrie and Deutz, Nicoolas and Rose, Christopher F. and Bemeur, Chantal",
                formatter.format("Cristina Bosoi, Mariana Oliveira, Rafael Ochoa Sanchez, Mélanie Tremblay, Gabrie TenHave, Nicoolas Deutz, Christopher F. Rose, Chantal Bemeur"));
    }

    @Test
    public void testMultipleSpaces() {
        Assert.assertEquals("Bosoi, Cristina and Oliveira, Mariana and Sanchez, Rafael Ochoa and Tremblay, Mélanie and TenHave, Gabrie and Deutz, Nicoolas and Rose, Christopher F. and Bemeur, Chantal",
                formatter.format("Cristina    Bosoi,    Mariana Oliveira, Rafael Ochoa Sanchez   ,   Mélanie Tremblay  , Gabrie TenHave, Nicoolas Deutz, Christopher F. Rose, Chantal Bemeur"));
    }

    @Test
    public void testAvoidPreposition() {
        Assert.assertEquals("von Zimmer, Hans and van Oberbergern, Michael and zu Berger, Kevin", formatter.format("Hans von Zimmer, Michael van Oberbergern, Kevin zu Berger"));
    }

    @Test
    public void testPreposition() {
        Assert.assertEquals("von Zimmer, Hans and van Oberbergern, Michael and zu Berger, Kevin", formatter.format("Hans von Zimmer, Michael van Oberbergern, Kevin zu Berger"));
    }

    @Test
    public void testAvoidNameAffixes() {
        Assert.assertEquals("der Barbar, Canon and der Große, Alexander", formatter.format("Canon der Barbar, Alexander der Große"));
    }
}