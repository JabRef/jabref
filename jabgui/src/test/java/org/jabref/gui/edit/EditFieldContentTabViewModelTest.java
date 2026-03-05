package org.jabref.gui.edit;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.Notifications;
import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.editfieldcontent.EditFieldContentViewModel;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import com.dlsc.gemsfx.infocenter.Notification;
import com.dlsc.gemsfx.infocenter.NotificationGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class EditFieldContentTabViewModelTest {
    EditFieldContentViewModel editFieldContentViewModel;
    BibEntry entryA;
    BibEntry entryB;

    BibDatabase bibDatabase;

    StateManager stateManager = mock(StateManager.class, Answers.RETURNS_DEEP_STUBS);
    DialogService dialogService = mock(DialogService.class, Answers.RETURNS_DEEP_STUBS);
    NotificationGroup<Object, Notification<Object>> notificationGroup = new NotificationGroup<>("");

    @BeforeEach
    void setup() {
        entryA = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.YEAR, "2015")
                .withField(StandardField.DATE, "2014");

        entryB = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.DATE, "1998")
                .withField(StandardField.YEAR, "");

        bibDatabase = new BibDatabase();
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(entryA, entryB));

        when(dialogService.getNotificationGroups()).thenReturn(List.of(notificationGroup));
        doAnswer(invocation -> {
            notificationGroup.getNotifications().add(invocation.getArgument(0));
            return null;
        }).when(dialogService).notify(any(Notifications.UiNotification.class));

        editFieldContentViewModel = new EditFieldContentViewModel(
                bibDatabase,
                List.of(entryA, entryB),
                mock(NamedCompoundEdit.class),
                dialogService,
                stateManager);
    }

    @Test
    void setFieldValueShouldNotDoAnythingIfOverwriteFieldContentIsNotEnabled() {
        editFieldContentViewModel.overwriteFieldContentProperty().set(false);
        editFieldContentViewModel.selectedFieldProperty().set(StandardField.YEAR);
        editFieldContentViewModel.fieldValueProperty().set("2001");
        editFieldContentViewModel.setFieldValue();

        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR));
    }

    @Test
    void setFieldValueShouldSetFieldValueIfOverwriteFieldContentIsEnabled() {
        editFieldContentViewModel.overwriteFieldContentProperty().set(true);
        editFieldContentViewModel.selectedFieldProperty().set(StandardField.YEAR);
        editFieldContentViewModel.fieldValueProperty().set("2001");
        editFieldContentViewModel.setFieldValue();

        assertEquals(Optional.of("2001"), entryA.getField(StandardField.YEAR));
    }

    @Test
    void setFieldValueShouldSetFieldValueIfFieldContentIsEmpty() {
        editFieldContentViewModel.overwriteFieldContentProperty().set(false);
        editFieldContentViewModel.selectedFieldProperty().set(StandardField.YEAR);
        editFieldContentViewModel.fieldValueProperty().set("2001");
        editFieldContentViewModel.setFieldValue();

        assertEquals(Optional.of("2001"), entryB.getField(StandardField.YEAR));
    }

    @Test
    void appendToFieldValueShouldDoNothingIfOverwriteFieldContentIsNotEnabled() {
        editFieldContentViewModel.overwriteFieldContentProperty().set(false);
        editFieldContentViewModel.selectedFieldProperty().set(StandardField.YEAR);
        editFieldContentViewModel.fieldValueProperty().set("0");
        editFieldContentViewModel.appendToFieldValue();

        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR));
    }

    @Test
    void appendToFieldValueShouldAppendFieldValueIfOverwriteFieldContentIsEnabled() {
        editFieldContentViewModel.overwriteFieldContentProperty().set(true);
        editFieldContentViewModel.selectedFieldProperty().set(StandardField.YEAR);
        editFieldContentViewModel.fieldValueProperty().set("0");
        editFieldContentViewModel.appendToFieldValue();

        assertEquals(Optional.of("20150"), entryA.getField(StandardField.YEAR));
    }

    @Test
    void getAllFieldsShouldNeverBeEmpty() {
        assertNotEquals(0, editFieldContentViewModel.getAllFields().size());
    }

    @Test
    void getSelectedFieldShouldHaveADefaultValue() {
        assertNotEquals(null, editFieldContentViewModel.getSelectedField());
    }
}
