package org.jabref.gui.preferences;

import java.util.Locale;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesFilter;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class PreferencesFilterDialog extends BaseDialog<Void> {

    private final PreferencesFilter preferencesFilter;
    private final ObservableList<PreferencesFilter.PreferenceOption> preferenceOptions;
    private final FilteredList<PreferencesFilter.PreferenceOption> filteredOptions;

    @FXML private TableView<PreferencesFilter.PreferenceOption> table;
    @FXML private TableColumn<PreferencesFilter.PreferenceOption, PreferencesFilter.PreferenceType> columnType;
    @FXML private TableColumn<PreferencesFilter.PreferenceOption, String> columnKey;
    @FXML private TableColumn<PreferencesFilter.PreferenceOption, Object> columnValue;
    @FXML private TableColumn<PreferencesFilter.PreferenceOption, Object> columnDefaultValue;
    @FXML private CheckBox showOnlyDeviatingPreferenceOptions;
    @FXML private Label count;
    @FXML private TextField searchField;

    public PreferencesFilterDialog(PreferencesFilter preferencesFilter) {
        this.preferencesFilter = Objects.requireNonNull(preferencesFilter);
        this.preferenceOptions = FXCollections.observableArrayList();
        this.filteredOptions = new FilteredList<>(this.preferenceOptions);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.setTitle(Localization.lang("Preferences"));
    }

    @FXML
    private void initialize() {
        showOnlyDeviatingPreferenceOptions.setOnAction(event -> updateModel());
        filteredOptions.predicateProperty().bind(EasyBind.map(searchField.textProperty(), searchText -> {
            if ((searchText == null) || searchText.isEmpty()) {
                return null;
            }
            String lowerCaseSearchText = searchText.toLowerCase(Locale.ROOT);
            return (option) -> option.getKey().toLowerCase(Locale.ROOT).contains(lowerCaseSearchText);
        }));
        columnType.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getType()));
        columnKey.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getKey()));
        columnValue.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getValue()));
        columnDefaultValue.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getDefaultValue().orElse("")));
        table.setItems(filteredOptions);
        count.textProperty().bind(Bindings.size(table.getItems()).asString("(%d)"));
        updateModel();
    }

    private void updateModel() {
        if (showOnlyDeviatingPreferenceOptions.isSelected()) {
            preferenceOptions.setAll(preferencesFilter.getDeviatingPreferences());
        } else {
            preferenceOptions.setAll(preferencesFilter.getPreferenceOptions());
        }
    }
}
