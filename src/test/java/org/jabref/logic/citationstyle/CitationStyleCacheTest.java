package org.jabref.logic.citationstyle;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CitationStyleCacheTest {

  private BibEntry bibEntry;
  private List<BibEntry> entries;
  private BibDatabase database;
  private BibDatabaseContext databaseContext;
  private CitationStyleCache csCache;

  @Test
  void getCitationForTest() {
    BibEntry bibEntry = new BibEntry().withCitationKey("test");
    List<BibEntry> entries = List.of(bibEntry);
    BibDatabase database = new BibDatabase(entries);
    BibDatabaseContext databaseContext = new BibDatabaseContext(database);
    CitationStyleCache csCache = new CitationStyleCache(databaseContext);

    assertNotNull(csCache.getCitationFor(bibEntry));
  }
}
