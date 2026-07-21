package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.forms.PreferencesFormBuilder;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.injection.Injector;

/// Base class for preference tabs. A tab describes its UI with a {@link PreferencesFormBuilder}
/// obtained from {@link #form()} and delegates the {@link PreferencesTab} lifecycle to its
/// view model.
public abstract class AbstractPreferenceTabView<T extends PreferenceTabViewModel> extends VBox implements PreferencesTab {

    protected final TaskExecutor taskExecutor;
    protected final DialogService dialogService;
    protected final GuiPreferences preferences;

    protected T viewModel;

    /// The builders created via [#form()], kept to serve their texts to the preferences search.
    private final List<PreferencesFormBuilder> forms = new ArrayList<>();

    protected AbstractPreferenceTabView() {
        this.preferences = Injector.instantiateModelOrService(GuiPreferences.class);
        this.dialogService = Injector.instantiateModelOrService(DialogService.class);
        this.taskExecutor = Injector.instantiateModelOrService(TaskExecutor.class);
        setSpacing(10.0);
    }

    /// Creates a fresh builder pre-wired with the services needed for section help buttons.
    protected PreferencesFormBuilder form() {
        PreferencesFormBuilder form = new PreferencesFormBuilder(dialogService, preferences);
        forms.add(form);
        return form;
    }

    @Override
    public List<SearchableElement> getSearchableElements() {
        return forms.stream()
                    .flatMap(form -> form.getSearchableElements().stream())
                    .toList();
    }

    @Override
    public Node getContent() {
        return this;
    }

    @Override
    public void setValues() {
        viewModel.setValues();
    }

    @Override
    public void storeSettings() {
        viewModel.storeSettings();
    }

    @Override
    public boolean validateSettings() {
        return viewModel.validateSettings();
    }

    @Override
    public List<String> getRestartWarnings() {
        return viewModel.getRestartWarnings();
    }
}
