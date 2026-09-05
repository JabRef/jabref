package org.jabref.gui.preferences.xmp;

import java.util.Comparator;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.validation.ValidationConstraints;
import org.jabref.gui.validation.ValidationMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldTextMapper;

import org.jfxcore.validation.Constraints;
import org.jfxcore.validation.property.ConstrainedListProperty;
import org.jfxcore.validation.property.SimpleConstrainedListProperty;

public class XmpPrivacyTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty xmpFilterEnabledProperty = new SimpleBooleanProperty();
    private final ConstrainedListProperty<Field, ValidationMessage> xmpFilterListProperty = new SimpleConstrainedListProperty<>(
            FXCollections.observableArrayList(),
            Constraints.forList(ValidationConstraints.<List<Field>>predicate(
                    input -> !input.isEmpty(),
                    ValidationMessage.error("%s > %s %n %n %s".formatted(
                            Localization.lang("XMP metadata"),
                            Localization.lang("Filter List"),
                            Localization.lang("List must not be empty."))))));
    private final ListProperty<Field> availableFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Field> addFieldProperty = new SimpleObjectProperty<>();

    private final DialogService dialogService;
    private final XmpPreferences xmpPreferences;

    XmpPrivacyTabViewModel(DialogService dialogService, XmpPreferences xmpPreferences) {
        this.dialogService = dialogService;
        this.xmpPreferences = xmpPreferences;
    }

    @Override
    public void setValues() {
        xmpFilterEnabledProperty.setValue(xmpPreferences.shouldUseXmpPrivacyFilter());

        xmpFilterListProperty.clear();
        xmpFilterListProperty.addAll(xmpPreferences.getXmpPrivacyFilter());

        availableFieldsProperty.clear();
        availableFieldsProperty.addAll(FieldFactory.getCommonFields());
        availableFieldsProperty.sort(Comparator.comparing(FieldTextMapper::getDisplayName));
    }

    @Override
    public void storeSettings() {
        xmpPreferences.setUseXmpPrivacyFilter(xmpFilterEnabledProperty.getValue());
        xmpPreferences.getXmpPrivacyFilter().clear();
        xmpPreferences.getXmpPrivacyFilter().addAll(xmpFilterListProperty.getValue());
    }

    public void addField() {
        if (addFieldProperty.getValue() == null) {
            return;
        }

        if (xmpFilterListProperty.getValue().stream().filter(item -> item.equals(addFieldProperty.getValue())).findAny().isEmpty()) {
            xmpFilterListProperty.add(addFieldProperty.getValue());
            addFieldProperty.setValue(null);
        }
    }

    public void removeFilter(Field filter) {
        xmpFilterListProperty.remove(filter);
    }

    @Override
    public boolean validateSettings() {
        if (xmpFilterEnabledProperty.getValue() && xmpFilterListProperty.isInvalid()) {
            dialogService.showErrorDialogAndWait(xmpFilterListProperty.getDiagnostics().invalidSubList().getFirst().message());
            return false;
        }
        return true;
    }

    public BooleanProperty xmpFilterEnabledProperty() {
        return xmpFilterEnabledProperty;
    }

    public ConstrainedListProperty<Field, ValidationMessage> filterListProperty() {
        return xmpFilterListProperty;
    }

    public ListProperty<Field> availableFieldsProperty() {
        return availableFieldsProperty;
    }

    public ObjectProperty<Field> addFieldNameProperty() {
        return addFieldProperty;
    }
}
