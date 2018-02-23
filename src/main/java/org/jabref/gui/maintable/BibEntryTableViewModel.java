package org.jabref.gui.maintable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;

import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FileFieldParser;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.KeywordGroup;

import org.fxmisc.easybind.EasyBind;

public class BibEntryTableViewModel {
    private final BibEntry entry;

    public BibEntryTableViewModel(BibEntry entry) {
        this.entry = entry;
    }

    public BibEntry getEntry() {
        return entry;
    }

    public Optional<String> getResolvedFieldOrAlias(String field, BibDatabase database) {
        return entry.getResolvedFieldOrAlias(field, database);
    }

    public ObjectBinding<String> getField(String fieldName) {
        return entry.getFieldBinding(fieldName);
    }

    public ObservableValue<Optional<SpecialFieldValueViewModel>> getSpecialField(SpecialField field) {
        return EasyBind.map(getField(field.getFieldName()),
                value -> field.parse(value).map(SpecialFieldValueViewModel::new));
    }

    public ObservableValue<List<LinkedFile>> getLinkedFiles() {
        return EasyBind.map(getField(FieldName.FILE), FileFieldParser::parse);
    }

    public ObservableValue<List<AbstractGroup>> getMatchedGroups() {
        SimpleObjectProperty<List<AbstractGroup>> matchedGroups = new SimpleObjectProperty<>(Collections.emptyList());

        //if (entry.isGroupHit()) {
        KeywordGroup testGroup = new ExplicitGroup("Test", GroupHierarchyType.INCLUDING, ',');
        testGroup.setColor(Color.RED);
        matchedGroups.setValue(Collections.singletonList(testGroup));
        //}

        return matchedGroups;
    }
}
