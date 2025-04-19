package org.jabref.gui.journals;

import java.util.List;

import javax.swing.undo.CompoundEdit;

import org.jabref.architecture.AllowedToUseSwing;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AllowedToUseSwing("UndoableUnabbreviator requires Swing Compound Edit")
class UndoableUnabbreviatorTest {
    
    private static final Abbreviation BUILT_IN_1 = new Abbreviation("Journal of Built-in Testing", "J. Built-in Test.");
    private static final Abbreviation BUILT_IN_2 = new Abbreviation("Archives of Built-in Science", "Arch. Built-in Sci.");
    
    private static final Abbreviation CUSTOM_1 = new Abbreviation("Journal of Custom Testing", "J. Custom Test.");
    private static final Abbreviation CUSTOM_2 = new Abbreviation("Archives of Custom Science", "Arch. Custom Sci.");
    
    private static final String CUSTOM_SOURCE = "custom-source";
    private JournalAbbreviationRepository repository;
    private UndoableUnabbreviator unabbreviator;
    private BibDatabase database;
    private CompoundEdit compoundEdit;
    
    @BeforeEach
    void setUp() {
        repository = new JournalAbbreviationRepository();
        repository.addCustomAbbreviations(List.of(BUILT_IN_1, BUILT_IN_2), 
                                         JournalAbbreviationRepository.BUILTIN_LIST_ID, 
                                         true);
        
        repository.addCustomAbbreviations(List.of(CUSTOM_1, CUSTOM_2), 
                                         CUSTOM_SOURCE, 
                                         true);
        
        unabbreviator = new UndoableUnabbreviator(repository);
        database = new BibDatabase();
        compoundEdit = new CompoundEdit();
    }
    
    private BibEntry createEntryWithAbbreviatedJournal(String abbreviatedJournal) {
        return new BibEntry(StandardEntryType.Article)
                .withField(StandardField.JOURNAL, abbreviatedJournal);
    }
    
    @Test
    void unabbreviateWithBothSourcesEnabled() {
        assertTrue(repository.isSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID));
        assertTrue(repository.isSourceEnabled(CUSTOM_SOURCE));
        
        BibEntry builtInEntry = createEntryWithAbbreviatedJournal(BUILT_IN_1.getAbbreviation());
        boolean builtInResult = unabbreviator.unabbreviate(database, builtInEntry, StandardField.JOURNAL, compoundEdit);
        
        assertEquals(BUILT_IN_1.getName(), builtInEntry.getField(StandardField.JOURNAL).orElse(""),
                   "Journal name should be replaced with full name");
        
        BibEntry customEntry = createEntryWithAbbreviatedJournal(CUSTOM_1.getAbbreviation());
        boolean customResult = unabbreviator.unabbreviate(database, customEntry, StandardField.JOURNAL, compoundEdit);
        
        assertEquals(CUSTOM_1.getName(), customEntry.getField(StandardField.JOURNAL).orElse(""),
                   "Journal name should be replaced with full name");
    }
    
    @Test
    void unabbreviateWithOnlyBuiltInSourceEnabled() {
        repository.setSourceEnabled(CUSTOM_SOURCE, false);
        
        assertTrue(repository.isSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID));
        assertFalse(repository.isSourceEnabled(CUSTOM_SOURCE));
        
        BibEntry builtInEntry = createEntryWithAbbreviatedJournal(BUILT_IN_1.getAbbreviation());
        boolean builtInResult = unabbreviator.unabbreviate(database, builtInEntry, StandardField.JOURNAL, compoundEdit);
        
        assertEquals(BUILT_IN_1.getName(), builtInEntry.getField(StandardField.JOURNAL).orElse(""),
                   "Journal name should be replaced with full name");
        
        BibEntry customEntry = createEntryWithAbbreviatedJournal(CUSTOM_1.getAbbreviation());
        boolean customResult = unabbreviator.unabbreviate(database, customEntry, StandardField.JOURNAL, compoundEdit);
        
        assertEquals(CUSTOM_1.getAbbreviation(), customEntry.getField(StandardField.JOURNAL).orElse(""),
                   "Journal abbreviation should remain unchanged");
    }
    
    @Test
    void unabbreviateWithOnlyCustomSourceEnabled() {
        repository.setSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID, false);
        
        assertFalse(repository.isSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID));
        assertTrue(repository.isSourceEnabled(CUSTOM_SOURCE));
        
        BibEntry builtInEntry = createEntryWithAbbreviatedJournal(BUILT_IN_1.getAbbreviation());
        boolean builtInResult = unabbreviator.unabbreviate(database, builtInEntry, StandardField.JOURNAL, compoundEdit);
        
        assertEquals(BUILT_IN_1.getAbbreviation(), builtInEntry.getField(StandardField.JOURNAL).orElse(""),
                   "Journal abbreviation should remain unchanged");
        
        BibEntry customEntry = createEntryWithAbbreviatedJournal(CUSTOM_1.getAbbreviation());
        boolean customResult = unabbreviator.unabbreviate(database, customEntry, StandardField.JOURNAL, compoundEdit);
        
        assertEquals(CUSTOM_1.getName(), customEntry.getField(StandardField.JOURNAL).orElse(""),
                   "Journal name should be replaced with full name");
    }
    
    @Test
    void unabbreviateWithBothSourcesDisabled() {
        repository.setSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID, false);
        repository.setSourceEnabled(CUSTOM_SOURCE, false);
        
        assertFalse(repository.isSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID));
        assertFalse(repository.isSourceEnabled(CUSTOM_SOURCE));
        
        BibEntry builtInEntry = createEntryWithAbbreviatedJournal(BUILT_IN_1.getAbbreviation());
        boolean builtInResult = unabbreviator.unabbreviate(database, builtInEntry, StandardField.JOURNAL, compoundEdit);
        
        assertEquals(BUILT_IN_1.getAbbreviation(), builtInEntry.getField(StandardField.JOURNAL).orElse(""),
                   "Journal abbreviation should remain unchanged");
        
        BibEntry customEntry = createEntryWithAbbreviatedJournal(CUSTOM_1.getAbbreviation());
        boolean customResult = unabbreviator.unabbreviate(database, customEntry, StandardField.JOURNAL, compoundEdit);
        
        assertEquals(CUSTOM_1.getAbbreviation(), customEntry.getField(StandardField.JOURNAL).orElse(""),
                   "Journal abbreviation should remain unchanged");
    }
}
