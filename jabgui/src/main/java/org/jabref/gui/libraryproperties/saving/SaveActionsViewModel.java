package org.jabref.gui.libraryproperties.saving;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.libraryproperties.PropertiesTabViewModel;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

public class SaveActionsViewModel implements PropertiesTabViewModel {
    // FieldFormatterCleanupsPanel
    private final BooleanProperty fieldFormatterCleanupsDisableProperty = new SimpleBooleanProperty();
    private final ListProperty<FieldFormatterCleanup> fieldFormatterCleanupsProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());

    private final SetProperty<CleanupPreferences.CleanupStep> multiFieldCleanupsProperty = new SimpleSetProperty<>(FXCollections.observableSet(new HashSet<>()));

    private final ObjectProperty<Optional<CleanupPreferences.CleanupStep>> journalAbbreviationCleanupProperty = new SimpleObjectProperty<>(Optional.empty());

    private final BibDatabaseContext databaseContext;
    private final MetaData initialMetaData;
    private final CliPreferences preferences;

    public SaveActionsViewModel(BibDatabaseContext databaseContext, CliPreferences preferences) {
        this.databaseContext = databaseContext;
        this.preferences = preferences;
        this.initialMetaData = databaseContext.getMetaData();
    }

    @Override
    public void setValues() {
        Optional<FieldFormatterCleanupActions> fieldFormatterCleanupActions = initialMetaData.getFieldFormatterCleanupActions();
        fieldFormatterCleanupActions.ifPresentOrElse(value -> {
            fieldFormatterCleanupsDisableProperty.setValue(!value.isEnabled());
            fieldFormatterCleanupsProperty.setValue(FXCollections.observableArrayList(value.getConfiguredActions()));
        }, () -> {
            CleanupPreferences defaultPreset = preferences.getDefaultCleanupPreset();
            fieldFormatterCleanupsDisableProperty.setValue(!defaultPreset.getFieldFormatterCleanups().isEnabled());
            fieldFormatterCleanupsProperty.setValue(FXCollections.observableArrayList(defaultPreset.getFieldFormatterCleanups().getConfiguredActions()));
        });

        Optional<Set<CleanupPreferences.CleanupStep>> multiFieldCleanups = initialMetaData.getMultiFieldCleanups();
        multiFieldCleanups.ifPresent(set -> multiFieldCleanupsProperty.set(FXCollections.observableSet(set)));

        journalAbbreviationCleanupProperty.set(initialMetaData.getJournalAbbreviationCleanup());
    }

    @Override
    public void storeSettings() {
        MetaData newMetaData = databaseContext.getMetaData();

        FieldFormatterCleanupActions fieldFormatterCleanupActions = new FieldFormatterCleanupActions(
                !fieldFormatterCleanupsDisableProperty().getValue(),
                fieldFormatterCleanupsProperty());

        if (FieldFormatterCleanupActions.DEFAULT_SAVE_ACTIONS.equals(fieldFormatterCleanupActions.getConfiguredActions())) {
            newMetaData.clearFieldFormatterActions();
        } else {
            if (fieldFormatterCleanupActions.getConfiguredActions().isEmpty()) {
                newMetaData.clearFieldFormatterActions();
            } else {
                newMetaData.setFieldFormatterCleanupActions(fieldFormatterCleanupActions);
            }
        }

        if (multiFieldCleanupsProperty.get().isEmpty()) {
            newMetaData.clearMultiFieldCleanups();
        } else {
            newMetaData.setMultiFieldCleanups(EnumSet.copyOf(multiFieldCleanupsProperty.get()));
        }

        Optional<CleanupPreferences.CleanupStep> journalCleanup = journalAbbreviationCleanupProperty.get();
        if (journalCleanup.isEmpty()) {
            newMetaData.clearJournalAbbreviationCleanup();
        } else {
            newMetaData.setJournalAbbreviationCleanup(journalCleanup.get());
        }

        databaseContext.setMetaData(newMetaData);
    }

    public SetProperty<CleanupPreferences.CleanupStep> multiFieldCleanupsPropertyProperty() {
        return multiFieldCleanupsProperty;
    }

    public ObjectProperty<Optional<CleanupPreferences.CleanupStep>> journalAbbreviationCleanupPropertyProperty() {
        return journalAbbreviationCleanupProperty;
    }

    // FieldFormatterCleanupsPanel

    public BooleanProperty fieldFormatterCleanupsDisableProperty() {
        return fieldFormatterCleanupsDisableProperty;
    }

    public ListProperty<FieldFormatterCleanup> fieldFormatterCleanupsProperty() {
        return fieldFormatterCleanupsProperty;
    }
}
