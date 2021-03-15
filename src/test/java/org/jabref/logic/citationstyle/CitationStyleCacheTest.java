package org.jabref.logic.citationstyle;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.texparser.Citation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CitationStyleCacheTest {

  private BibEntry bibEntry;
  private List<BibEntry> entries;
  private BibDatabase database;
  private BibDatabaseContext databaseContext;
  private CitationStyleCache csCache;

  @BeforeAll
  static void beforeAll() {
  }

  @Test
  void getCitationForTest() {
    BibEntry bibEntry = new BibEntry();
    bibEntry.setCitationKey("test");
    List<BibEntry> entries = new ArrayList<>();
    entries.add(0,bibEntry);
    BibDatabase database = new BibDatabase(entries);
    BibDatabaseContext databaseContext = new BibDatabaseContext(database);
    CitationStyleCache csCache = new CitationStyleCache(databaseContext);

    assertNotNull(csCache.getCitationFor(bibEntry));
  }
}
