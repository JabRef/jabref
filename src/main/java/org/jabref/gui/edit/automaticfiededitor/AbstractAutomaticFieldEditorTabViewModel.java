package org.jabref.gui.edit.automaticfiededitor;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAutomaticFieldEditorTabViewModel extends AbstractViewModel {
    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractAutomaticFieldEditorTabViewModel.class);

    protected final EventBus eventBus = new EventBus();
    private final ObservableList<Field> allFields = FXCollections.observableArrayList();

    public AbstractAutomaticFieldEditorTabViewModel(BibDatabase bibDatabase) {
        Objects.requireNonNull(bibDatabase);
        addFields(EnumSet.allOf(StandardField.class));
        addFields(bibDatabase.getAllVisibleFields());
        allFields.sort(Comparator.comparing(Field::getName));
    }

    public ObservableList<Field> getAllFields() {
        return allFields;
    }

    private void addFields(Collection<? extends Field> fields) {
        Set<Field> fieldsSet = new HashSet<>(allFields);
        fieldsSet.addAll(fields);
        allFields.setAll(fieldsSet);
    }

    public void registerListener(Object object) {
        eventBus.register(object);
    }

    public void unregisterListener(Object listener) {
        try {
            eventBus.unregister(listener);
        } catch (
                IllegalArgumentException e) {
            // occurs if the event source has not been registered, should not prevent shutdown
            LOGGER.debug("Problem unregistering", e);
        }
    }
}
