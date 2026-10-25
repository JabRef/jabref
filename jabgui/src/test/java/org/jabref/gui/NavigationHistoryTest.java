package org.jabref.gui;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
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

    @Test
    void forwardSkipsDeletedEntry() {
        NavigationHistory history = new NavigationHistory();
        BibEntry first = new BibEntry().withCitationKey("First");
        BibEntry second = new BibEntry().withCitationKey("Second");
        BibEntry third = new BibEntry().withCitationKey("Third");

        history.add(first);
        history.add(second);
        history.add(third);
        history.back();
        history.back();
        history.removeEntries(List.of(second));

        assertEquals(Optional.of(third), history.forward());
    }

    @Test
    void backAndForwardSkipsMultipleDeletedEntries() {
        NavigationHistory history = new NavigationHistory();
        BibEntry first = new BibEntry().withCitationKey("First");
        BibEntry second = new BibEntry().withCitationKey("Second");
        BibEntry third = new BibEntry().withCitationKey("Third");
        BibEntry fourth = new BibEntry().withCitationKey("Fourth");
        BibEntry fifth = new BibEntry().withCitationKey("Fifth");

        history.add(first);
        history.add(second);
        history.add(third);
        history.add(fourth);
        history.add(fifth);
        history.back();
        history.back();
        // current: Third, back: First and Second, forward: Fourth and Fifth
        history.removeEntries(List.of(second, fourth));

        assertEquals(Optional.of(first), history.back());
        assertEquals(Optional.of(third), history.forward());
        assertEquals(Optional.of(fifth), history.forward());
    }
}
