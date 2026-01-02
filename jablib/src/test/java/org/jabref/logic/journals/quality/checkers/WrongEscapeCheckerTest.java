package org.jabref.logic.journals.quality.checkers;

import java.util.List;

import org.jabref.logic.journals.quality.AbbreviationEntry;
import org.jabref.logic.journals.quality.Finding;
import org.jabref.logic.journals.quality.Severity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WrongEscapeCheckerTest {

    @Test
    void detectsEscapeInFullName() {
        AbbreviationEntry entry = new AbbreviationEntry("Zeszyty Naukowe W\\y", "Problemy Mat.");
                   WrongEscapeChecker checker = new WrongEscapeChecker();
        List<Finding> findings = checker.check(List.of(entry));

        assertEquals(1, findings.size());
        assertEquals(Severity.ERROR, findings.get(0).severity());
        assertEquals("ERR_WRONG_ESCAPE", findings.get(0).code());
    }
}


