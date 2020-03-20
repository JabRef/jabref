package org.jabref.logic.integrity;

import org.jabref.model.database.BibDatabaseContext;
import org.junit.jupiter.api.Test;



import static org.junit.jupiter.api.Assertions.assertTrue;


public class EditionCheckerTest {
     boolean allowIntegerEdition;

    @Test
    void isFirstCharacterANumber(){
        var bibDatabaseContextEdition=new BibDatabaseContext();
        var editionChecker=new EditionChecker(bibDatabaseContextEdition,allowIntegerEdition);
        var stringWithNumber="0HelloWorld";
        boolean flag=editionChecker.isFirstCharDigit(stringWithNumber);
        assertTrue(flag,"check for true");

    }
}
