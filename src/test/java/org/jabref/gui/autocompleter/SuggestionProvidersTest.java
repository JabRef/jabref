package org.jabref.gui.autocompleter;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SuggestionProvidersTest {

  @Test
  void getForFieldTest() {
      BibDatabase database = new BibDatabase();
      JournalAbbreviationRepository abbreviationRepository = mock(JournalAbbreviationRepository.class);
      BibEntry entry = new BibEntry();
      Field personEntryField = StandardField.AUTHOR;
      Field singleEntryField = StandardField.XREF;
      Field multipleEntryField = StandardField.XDATA;
      Field journalEntryField = StandardField.JOURNAL;
      Field publisherEntryField = StandardField.PUBLISHER;
      Field specialEntryField = SpecialField.PRINTED;
      AutoCompletePreferences autoCompletePreferences = new AutoCompletePreferences(true, AutoCompleteFirstNameMode.BOTH, AutoCompletePreferences.NameFormat.BOTH, FieldFactory.parseFieldList(personEntryField.getName() + ";" + singleEntryField.getName() + ";" + multipleEntryField.getName() + ";" + journalEntryField.getName() + ";" + publisherEntryField.getName() + ";" + specialEntryField.getName()), null);
      SuggestionProviders sp = new SuggestionProviders(database, abbreviationRepository, autoCompletePreferences);
      SuggestionProviders empty = new SuggestionProviders();

      entry.setField(personEntryField, "Goethe");
      entry.setField(singleEntryField, "Single");
      entry.setField(multipleEntryField, "Multiple");
      entry.setField(journalEntryField, "Journal");
      entry.setField(publisherEntryField, "Publisher");
      entry.setField(specialEntryField, "2000");

      assertEquals(org.jabref.gui.autocompleter.EmptySuggestionProvider.class, empty.getForField(personEntryField).getClass());
      assertEquals(org.jabref.gui.autocompleter.PersonNameSuggestionProvider.class, sp.getForField(personEntryField).getClass());
      assertEquals(org.jabref.gui.autocompleter.BibEntrySuggestionProvider.class, sp.getForField(singleEntryField).getClass());
      assertEquals(org.jabref.gui.autocompleter.BibEntrySuggestionProvider.class, sp.getForField(multipleEntryField).getClass());
      assertEquals(org.jabref.gui.autocompleter.JournalsSuggestionProvider.class, sp.getForField(journalEntryField).getClass());
      assertEquals(org.jabref.gui.autocompleter.JournalsSuggestionProvider.class, sp.getForField(publisherEntryField).getClass());
      assertEquals(org.jabref.gui.autocompleter.WordSuggestionProvider.class, sp.getForField(specialEntryField).getClass());
  }
}
