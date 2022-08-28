package org.jabref.logic.integrity;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Teste_nomes {

    private PersonNamesChecker checker;
    private PersonNamesChecker checkerb;

    @BeforeEach
    public void setUp() throws Exception {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.setMode(BibDatabaseMode.BIBTEX);
        checker = new PersonNamesChecker(databaseContext);
        BibDatabaseContext database = new BibDatabaseContext();
        database.setMode(BibDatabaseMode.BIBLATEX);
        checkerb = new PersonNamesChecker(database);
    }


    @Test
    public void validNameFirstnameAuthors() throws Exception {
        String nome = "hugo Para{\\~n}os";
        assertEquals(Optional.empty(), checker.checkValue(nome));
    }
}
