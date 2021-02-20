package org.jabref.gui.contentselector;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentSelectorDialogViewModelTest {
    private final LibraryTab libraryTab = mock(LibraryTab.class);
    private final DialogService dialogService = mock(DialogService.class);
    private final List<StandardField> DEFAULT_FIELDS = Arrays.asList(
            StandardField.AUTHOR, StandardField.JOURNAL, StandardField.KEYWORDS, StandardField.PUBLISHER);
    private ContentSelectorDialogViewModel viewModel;
    private BibDatabaseContext bibDatabaseContext;

    @BeforeEach
    void setUp() {
        bibDatabaseContext = new BibDatabaseContext();
        when(libraryTab.getBibDatabaseContext()).thenReturn(bibDatabaseContext);
        viewModel = new ContentSelectorDialogViewModel(libraryTab, dialogService);
    }

    @Test
    void initHasDefaultFieldNames() {
        ListProperty<Field> expected = new SimpleListProperty<>(FXCollections.observableArrayList(DEFAULT_FIELDS));
        ListProperty<Field> result = viewModel.getFieldNamesBackingList();

        assertEquals(expected, result);
    }

    @Test
    void addsNewKeyword() {
        addKeyword(StandardField.KEYWORDS, "test");

        ListProperty<String> expected = new SimpleListProperty<>(
                FXCollections.observableArrayList("test"));
        ListProperty<String> result = viewModel.getKeywordsBackingList();

        assertEquals(expected, result);
    }

    @Test
    void addsKeywordOnlyIfUnique() {
        addKeyword(StandardField.KEYWORDS, "test");
        addKeyword(StandardField.KEYWORDS, "test");

        ListProperty<String> expected = new SimpleListProperty<>(
                FXCollections.observableArrayList("test"));
        ListProperty<String> result = viewModel.getKeywordsBackingList();

        assertEquals(expected, result);
    }

    @Test
    void removesKeyword() {
        addKeyword(StandardField.KEYWORDS, "test");
        removeKeyword(StandardField.KEYWORDS, "test");

        ListProperty<String> expected = new SimpleListProperty<>(FXCollections.observableArrayList());
        ListProperty<String> result = viewModel.getKeywordsBackingList();

        assertEquals(expected, result);
    }

    @Test
    void addsNewField() {
        UnknownField testField = new UnknownField("Test");
        addField(testField);

        ListProperty<Field> fields = viewModel.getFieldNamesBackingList();
        boolean fieldsContainTestValue = fields.stream().anyMatch(field -> field.getDisplayName().equals("Test"));

        assertTrue(fieldsContainTestValue);
    }

    @Test
    void removesField() {
        UnknownField testField = new UnknownField("Test");
        addField(testField);
        removeField(testField);

        ListProperty<Field> expected = new SimpleListProperty<>(FXCollections.observableArrayList(DEFAULT_FIELDS));
        ListProperty<Field> result = viewModel.getFieldNamesBackingList();

        assertEquals(expected, result);
    }

    @Test
    void displaysKeywordsInAlphabeticalOrder() {
        addKeyword(StandardField.KEYWORDS, "test1");
        addKeyword(StandardField.KEYWORDS, "test2");

        ListProperty<String> expected = new SimpleListProperty<>(
                FXCollections.observableArrayList("test1", "test2"));
        ListProperty<String> result = viewModel.getKeywordsBackingList();

        assertEquals(expected, result);
    }

    @Test
    void savingPersistsDataInDatabase() {
        UnknownField testField = new UnknownField("Test");
        addField(testField);
        addKeyword(testField, "test1");
        addKeyword(testField, "test2");
        viewModel.saveChanges();

        List<String> result = bibDatabaseContext.getMetaData()
                                                .getContentSelectorValuesForField(testField);
        List<String> expected = Arrays.asList("test1", "test2");

        assertEquals(expected, result);
    }

    private void addKeyword(Field field, String keyword) {
        when(dialogService.showInputDialogAndWait(
                Localization.lang("Add new keyword"), Localization.lang("Keyword:")))
                .thenReturn(Optional.of(keyword));

        viewModel.showInputKeywordDialog(field);
    }

    private void removeKeyword(Field field, String keyword) {
        when(dialogService.showConfirmationDialogAndWait(Localization.lang("Remove keyword"),
                Localization.lang("Are you sure you want to remove keyword: \"%0\"?", keyword)))
                .thenReturn(true);

        viewModel.showRemoveKeywordConfirmationDialog(field, keyword);
    }

    private void addField(Field field) {
        when(dialogService.showInputDialogAndWait(
                Localization.lang("Add new field name"), Localization.lang("Field name:")))
                .thenReturn(Optional.of(field.getDisplayName()));

        viewModel.showInputFieldNameDialog();
    }

    private void removeField(Field field) {
        when(dialogService.showConfirmationDialogAndWait(
                Localization.lang("Remove field name"),
                Localization.lang("Are you sure you want to remove field name: \"%0\"?", field.getDisplayName())))
                .thenReturn(true);

        viewModel.showRemoveFieldNameConfirmationDialog(field);
    }
}
