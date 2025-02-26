package org.jabref.gui.integrity;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntegrityCheckDialogViewModelTest {

    private BibEntry entry;
    private IntegrityMessage message;
    private IntegrityCheckDialogViewModel viewModel;

    @BeforeEach
    void setUp() {
        entry = Mockito.mock(BibEntry.class);
        message = Mockito.mock(IntegrityMessage.class);
        viewModel = new IntegrityCheckDialogViewModel(Collections.emptyList());

        when(message.entry()).thenReturn(entry);
    }

    @Test
    void maskTitle() {
        when(entry.getTitle()).thenReturn(Optional.of("Proceedings of the 40th International Conference on Machine Learning"));
        viewModel.maskTitle(message);
        verify(entry).setField(Map.of(StandardField.TITLE, "{P}roceedings of the 40th {I}nternational {C}onference on {M}achine {L}earning"));
    }

    @Test
    void removeField() {
        viewModel.removeField(message, StandardField.AUTHOR);
        verify(entry).setField(Map.of(StandardField.AUTHOR, ""));
    }

    @Test
    void correctDateFormat() {
        when(message.entry()).thenReturn(entry);
        when(entry.getField(StandardField.DATE)).thenReturn(Optional.of("25-02-2025"));
        viewModel.correctDateFormat(message);
        verify(entry).setField(Map.of(StandardField.DATE, "2025-02-25"));
    }

    @Test
    void formatPageNumberRange() {
        when(entry.getField(StandardField.PAGES)).thenReturn(Optional.of("abc"));
        viewModel.formatPageNumberRange(message);
        verify(entry).setField(Map.of(StandardField.PAGES, "1-10"));
    }

    @Test
    void ttestReplaceNonASCIICharacters() {
        when(entry.getField(StandardField.ABSTRACT)).thenReturn(Optional.of("Thé abstract"));
        viewModel.replaceNonASCIICharacters(message);
        verify(entry).setField(Map.of(StandardField.ABSTRACT, "Th abstract"));
    }

    @Test
    void replaceNonASCIICharacters() {
        when(message.entry()).thenReturn(entry);
        when(entry.getField(StandardField.ABSTRACT)).thenReturn(Optional.of("Thé abstract"));
        viewModel.replaceNonASCIICharacters(message);
        verify(entry).setField(Map.of(StandardField.ABSTRACT, "Th abstract"));
    }

    @Test
    void handleMissingCitationKey() {
        viewModel.handleMissingCitationKey(message);
        verify(entry).setField(Map.of(StandardField.CROSSREF, "UnknownKey"));
    }

    @Test
    void capitalizeFirstLetter() {
        Field field = StandardField.AUTHOR;
        when(entry.getField(field)).thenReturn(Optional.of("john doe"));
        viewModel.capitalizeFirstLetter(message, field);
        verify(entry).setField(Map.of(field, "John doe"));
    }

    @Test
    void ensureValidEdition() {
        when(entry.getField(StandardField.EDITION)).thenReturn(Optional.of("Fourth"));
        viewModel.ensureValidEdition(message);
        verify(entry).setField(Map.of(StandardField.EDITION, "First"));
    }

    @Test
    void removeNonIntegerEdition() {
        when(entry.getField(StandardField.EDITION)).thenReturn(Optional.of("First"));
        viewModel.removeNonIntegerEdition(message);
        verify(entry).setField(Map.of(StandardField.EDITION, ""));
    }
}
