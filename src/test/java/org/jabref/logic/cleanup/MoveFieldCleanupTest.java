package org.jabref.logic.cleanup;

import java.util.List;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MoveFieldCleanupTest {

   private BibEntry temp = new BibEntry();
   private String mockSource = "pretend";
   private String mockDestination = "sent";
   private List<FieldChange> testReturn;
   private MoveFieldCleanup sut;
   @BeforeEach
   void setup(){
       sut = new MoveFieldCleanup(mockSource, mockDestination);
   }
    @Test
    public void givenNoArgsConstructorBibEntryWhenCleanupThenRuns(){
       testReturn = sut.cleanup(temp);
       assertNotNull(testReturn);
    }
}
