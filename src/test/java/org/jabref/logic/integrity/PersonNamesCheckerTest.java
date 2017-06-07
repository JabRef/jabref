package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PersonNamesCheckerTest {

    PersonNamesChecker checker;

    @Before
    public void setUp() throws Exception {
        checker = new PersonNamesChecker();
    }

    @Test
    public void complainAboutPersonStringWithTwoManyCommas() throws Exception {
        assertEquals(Optional.of("Names are not in the standard BibTeX format."),
                checker.checkValue("Test1, Test2, Test3, Test4, Test5, Test6"));
    }

    @Test
    public void doNotComplainAboutSecondNameInFront() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("M. J. Gotay"));
    }
}
