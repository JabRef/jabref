package org.jabref.gui;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NavigationHistoryTest {

    @Test
    void backSkipsDeletedEntry() {
        NavigationHistory history = new NavigationHistory();
        BibEntry tokede = new BibEntry().withCitationKey("Tokede_2011");
        BibEntry hooper = new BibEntry().withCitationKey("Hooper_2012");
        BibEntry richard = new BibEntry().withCitationKey("Richard_2017");

        history.add(tokede);
        history.add(hooper);
        history.add(richard);
        history.add(hooper); // right-click on Hooper selects it again
        history.removeEntries(List.of(hooper));

        assertEquals(Optional.of(richard), history.back());
        assertEquals(Optional.of(tokede), history.back());
    }
}
