package org.jabref.gui.walkthrough.declarative.sideeffect;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferencesDialogView;
import org.jabref.gui.walkthrough.Walkthrough;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/// Opens the preferences dialog for a walkthrough and closes it when the walkthrough is reverted.
@NullMarked
// [impl->req~ux.walkthrough.preferences-direct~1]
public class OpenPreferencesSideEffect implements WalkthroughSideEffect {
    private final DialogService dialogService;
    private final PreferencesDialogView preferencesDialog;

    public OpenPreferencesSideEffect() {
        this(Injector.instantiateModelOrService(DialogService.class), new PreferencesDialogView(null));
    }

    OpenPreferencesSideEffect(DialogService dialogService, PreferencesDialogView preferencesDialog) {
        this.dialogService = dialogService;
        this.preferencesDialog = preferencesDialog;
    }

    @Override
    public @NonNull ExpectedCondition expectedCondition() {
        return ExpectedCondition.ALWAYS_TRUE;
    }

    @Override
    public boolean forward(@NonNull Walkthrough walkthrough) {
        dialogService.showCustomDialog(preferencesDialog);
        return true;
    }

    @Override
    public boolean backward(@NonNull Walkthrough walkthrough) {
        preferencesDialog.close();
        return true;
    }

    @Override
    public @NonNull String description() {
        return "Open JabRef preferences.";
    }
}
