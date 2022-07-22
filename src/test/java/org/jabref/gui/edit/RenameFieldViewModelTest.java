package org.jabref.gui.edit;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.edit.automaticfiededitor.renamefield.RenameFieldViewModel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RenameFieldViewModelTest {
    RenameFieldViewModel renameFieldViewModel;
    BibEntry entryA;
    BibEntry entryB;

    BibDatabase bibDatabase;

    @BeforeEach
    void setup() {
        entryA = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.YEAR, "2015")
                .withField(StandardField.DATE, "2014")
                .withField(StandardField.AUTHOR, "Doe");

        entryB = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.DATE, "1998")
                .withField(StandardField.YEAR, "")
                .withField(StandardField.AUTHOR, "Eddie");

        bibDatabase = new BibDatabase();
        renameFieldViewModel = new RenameFieldViewModel(List.of(entryA, entryB), bibDatabase, new NamedCompound(""));
    }

    @Test
    void renameFieldShouldRenameFieldIfItExist() {
        renameFieldViewModel.selectField(StandardField.DATE);
        renameFieldViewModel.setNewFieldName("ETAD");
        renameFieldViewModel.renameField();

        assertEquals(Optional.of("2014"), entryA.getField(FieldFactory.parseField("ETAD")));
        assertEquals(Optional.empty(), entryA.getField(StandardField.DATE));

        assertEquals(Optional.of("1998"), entryB.getField(FieldFactory.parseField("ETAD")));
        assertEquals(Optional.empty(), entryB.getField(StandardField.DATE));
    }

    @Test
    void renameFieldShouldDoNothingIfFieldDoNotExist() {
        Field toRenameField = new UnknownField("Some_field_that_doesnt_exist");
        renameFieldViewModel.selectField(toRenameField);
        renameFieldViewModel.setNewFieldName("new_field_name");
        renameFieldViewModel.renameField();

        assertEquals(Optional.empty(), entryA.getField(toRenameField));
        assertEquals(Optional.empty(), entryA.getField(new UnknownField("new_field_name")));

        assertEquals(Optional.empty(), entryB.getField(toRenameField));
        assertEquals(Optional.empty(), entryB.getField(new UnknownField("new_field_name")));
    }

    @Test
    void renameFieldShouldNotDoAnythingIfTheNewFieldNameIsEmpty() {
        renameFieldViewModel.selectField(StandardField.AUTHOR);
        renameFieldViewModel.setNewFieldName("");
        renameFieldViewModel.renameField();

        assertEquals(Optional.of("Doe"), entryA.getField(StandardField.AUTHOR));
        assertEquals(Optional.empty(), entryA.getField(FieldFactory.parseField("")));

        assertEquals(Optional.of("Eddie"), entryB.getField(StandardField.AUTHOR));
        assertEquals(Optional.empty(), entryB.getField(FieldFactory.parseField("")));
    }

    @Test
    void renameFieldShouldNotDoAnythingIfTheNewFieldNameHasWhitespaceCharacters() {
        renameFieldViewModel.selectField(StandardField.AUTHOR);
        renameFieldViewModel.setNewFieldName("Hello, World");
        renameFieldViewModel.renameField();

        assertEquals(Optional.of("Doe"), entryA.getField(StandardField.AUTHOR));
        assertEquals(Optional.empty(), entryA.getField(FieldFactory.parseField("Hello, World")));

        assertEquals(Optional.of("Eddie"), entryB.getField(StandardField.AUTHOR));
        assertEquals(Optional.empty(), entryB.getField(FieldFactory.parseField("Hello, World")));
    }

    @Test
    void renameFieldShouldDoNothingWhenThereIsAlreadyAFieldWithTheSameNameAsNewFieldName() {
        renameFieldViewModel.selectField(StandardField.DATE);
        renameFieldViewModel.setNewFieldName(StandardField.YEAR.getName());
        renameFieldViewModel.renameField();

        assertEquals(Optional.of("2014"), entryA.getField(StandardField.DATE));
        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR));

        assertEquals(Optional.empty(), entryB.getField(StandardField.DATE));
        assertEquals(Optional.of("1998"), entryB.getField(StandardField.YEAR));
    }
}
