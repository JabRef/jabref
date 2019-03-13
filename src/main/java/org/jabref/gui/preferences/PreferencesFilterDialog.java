package org.jabref.gui.preferences;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferencesFilter;

import com.airhacks.afterburner.views.ViewLoader;

public class PreferencesFilterDialog extends BaseDialog<Void> {

    private final JabRefPreferencesFilter preferencesFilter;
    private final ObservableList<JabRefPreferencesFilter.PreferenceOption> preferenceOptions;
    private final List<JabRefPreferencesFilter.PreferenceOption> auxillaryPreferenceOptions;

    @FXML private TableView<JabRefPreferencesFilter.PreferenceOption> table;
    @FXML private TableColumn<JabRefPreferencesFilter.PreferenceOption, JabRefPreferencesFilter.PreferenceType> columnType;
    @FXML private TableColumn<JabRefPreferencesFilter.PreferenceOption, String> columnKey;
    @FXML private TableColumn<JabRefPreferencesFilter.PreferenceOption, Object> columnValue;
    @FXML private TableColumn<JabRefPreferencesFilter.PreferenceOption, Object> columnDefaultValue;
    @FXML private CheckBox showOnlyDeviatingPreferenceOptions;
    @FXML private Label count;
    @FXML private TextField searchField;

    public PreferencesFilterDialog(JabRefPreferencesFilter preferencesFilter) {
        this.preferencesFilter = Objects.requireNonNull(preferencesFilter);
        this.preferenceOptions = FXCollections.observableArrayList();
        this.auxillaryPreferenceOptions = preferencesFilter.getPreferenceOptions();

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.setTitle(Localization.lang("Preferences"));
    }

    @FXML
    private void initialize() {
        showOnlyDeviatingPreferenceOptions.setOnAction(event -> updateModel());
        searchField.textProperty().addListener((observable, previousText, newText) -> filterPreferences(newText.toLowerCase(Locale.ROOT)));
        columnType.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getType()));
        columnKey.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getKey()));
        columnValue.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getValue()));
        columnDefaultValue.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getDefaultValue().orElse("")));
        table.setItems(preferenceOptions);
        updateModel();
    }

    private void updateModel() {
        auxillaryPreferenceOptions.clear();
        if (showOnlyDeviatingPreferenceOptions.isSelected()) {
            auxillaryPreferenceOptions.addAll(preferencesFilter.getDeviatingPreferences());
        } else {
            auxillaryPreferenceOptions.addAll(preferencesFilter.getPreferenceOptions());
        }
        preferenceOptions.setAll(auxillaryPreferenceOptions);
        count.setText(String.format("(%d)", preferenceOptions.size()));
        String searchText = searchField.getText();
        if (!searchText.isEmpty()) {
            filterPreferences(searchText);
        }
    }

    private void filterPreferences(String searchText) {
        if (searchText.isEmpty()) {
            updateModel();
            return;
        }

        List<JabRefPreferencesFilter.PreferenceOption> filteredOptions = auxillaryPreferenceOptions.stream()
                                                                                                   .filter(p -> p.getKey().toLowerCase(Locale.ROOT).contains(searchText))
                                                                                                   .collect(Collectors.toList());
        preferenceOptions.setAll(filteredOptions);
    }

}
