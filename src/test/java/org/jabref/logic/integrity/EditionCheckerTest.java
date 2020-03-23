package org.jabref.logic.integrity;

import org.jabref.model.database.BibDatabaseContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditionCheckerTest {
    @Test
    void isFirstCharacterANumber(){
        boolean allowIntegerEdition=false;
        var bibDatabaseContextEdition=new BibDatabaseContext();
        var editionChecker=new EditionChecker(bibDatabaseContextEdition,allowIntegerEdition);
        var stringWithNumber="0HelloWorld";
        assertTrue(editionChecker.isFirstCharDigit(stringWithNumber));
    }
    @Test
    void isFirstCharacterNotANumber(){
        boolean allowIntegerEdition=false;
        var bibDatabaseContextEdition=new BibDatabaseContext();
        var editionChecker=new EditionChecker(bibDatabaseContextEdition,allowIntegerEdition);
        var stringWithLetter="HelloWorld";
        assertFalse(editionChecker.isFirstCharDigit(stringWithLetter));
    }
}
