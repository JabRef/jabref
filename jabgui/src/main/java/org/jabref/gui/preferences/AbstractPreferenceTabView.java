package org.jabref.gui.preferences;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.forms.PreferencesFormBuilder;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.Nullable;

import static org.jabref.gui.preferences.forms.FormMetrics.GAP;

/// Base class for preference tabs. A tab describes its UI with a {@link PreferencesFormBuilder}
/// obtained from {@link #form()} and delegates the {@link PreferencesTab} lifecycle to its
/// view model.
public abstract class AbstractPreferenceTabView<T extends PreferenceTabViewModel> extends VBox implements PreferencesTab {

    protected final TaskExecutor taskExecutor;
    protected final DialogService dialogService;
    protected final GuiPreferences preferences;

    protected T viewModel;

    /// The builder created via [#form()], kept to serve its texts to the preferences search.
    private @Nullable PreferencesFormBuilder form;

    protected AbstractPreferenceTabView() {
        this.preferences = Injector.instantiateModelOrService(GuiPreferences.class);
        this.dialogService = Injector.instantiateModelOrService(DialogService.class);
        this.taskExecutor = Injector.instantiateModelOrService(TaskExecutor.class);
        setSpacing(GAP);
    }

    /// Creates the tab's builder, pre-wired with the services needed for section help buttons. A tab
    /// describes itself with one form, so calling this twice would leave the first form's texts
    /// unreachable to the preferences search.
    protected PreferencesFormBuilder form() {
        if (form != null) {
            throw new IllegalStateException("form() was already called; a tab describes itself with one form");
        }
        form = new PreferencesFormBuilder(dialogService, preferences);
        return form;
    }

    @Override
    public List<SearchableElement> getSearchableElements() {
        return form == null ? List.of() : form.getSearchableElements();
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
