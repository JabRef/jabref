package org.jabref.gui.preferences.forms;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.injection.Injector;

/// Base class for preference tabs that describe their UI with a {@link PreferencesFormBuilder}
/// instead of an FXML file.
///
/// It extends {@link AbstractPreferenceTabView} (rather than replacing it) so the preferences
/// dialog still recognises these tabs — {@code PreferencesDialogView} renders only nodes that are
/// an {@code AbstractPreferenceTabView}. The inherited `@Inject` fields are normally populated by
/// afterburner during FXML loading; since these tabs skip FXML, we resolve the same services
/// through the {@link Injector} (the same registry that backs `@Inject`).
public abstract class AbstractFormTabView<T extends PreferenceTabViewModel> extends AbstractPreferenceTabView<T> {

    protected AbstractFormTabView() {
        this.preferences = Injector.instantiateModelOrService(GuiPreferences.class);
        this.dialogService = Injector.instantiateModelOrService(DialogService.class);
        this.taskExecutor = Injector.instantiateModelOrService(TaskExecutor.class);
        setSpacing(10.0);
    }

    /// Creates a fresh builder pre-wired with the services needed for section help buttons.
    protected PreferencesFormBuilder form() {
        return new PreferencesFormBuilder(dialogService, preferences);
    }
}
