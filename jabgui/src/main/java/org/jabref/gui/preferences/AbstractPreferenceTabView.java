package org.jabref.gui.preferences;

import java.util.List;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.forms.PreferencesFormBuilder;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Base class for preference tabs. A tab describes its UI with a [PreferencesFormBuilder]
/// obtained from [#form()] and delegates the [PreferencesTab] lifecycle to its
/// view model.
///
/// A tab is not itself a node: the form the builder assembles is the tab's content, handed over with
/// [#setContent]. Were the tab a container of its own, every tab would nest that form in a
/// second pane with the same spacing, and it would be ambiguous which of the two owns the layout.
public abstract class AbstractPreferenceTabView<T extends PreferenceTabViewModel> implements PreferencesTab {

    protected final TaskExecutor taskExecutor;
    protected final DialogService dialogService;
    protected final GuiPreferences preferences;

    protected T viewModel;

    /// The builder created via [#form()], kept to serve its texts to the preferences search.
    private @Nullable PreferencesFormBuilder form;

    private @Nullable Node content;

    protected AbstractPreferenceTabView() {
        this.preferences = Injector.instantiateModelOrService(GuiPreferences.class);
        this.dialogService = Injector.instantiateModelOrService(DialogService.class);
        this.taskExecutor = Injector.instantiateModelOrService(TaskExecutor.class);
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

    /// Hands the assembled form over as the tab's content; called once, from the tab's constructor.
    protected void setContent(@NonNull Node content) {
        this.content = content;
    }

    @Override
    public Node getContent() {
        if (content == null) {
            throw new IllegalStateException("the tab built no content; call setContent() from its constructor");
        }
        return content;
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
