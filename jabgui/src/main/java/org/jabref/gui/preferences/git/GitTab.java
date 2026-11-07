package org.jabref.gui.preferences.git;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.forms.PasswordFieldEditor;
import org.jabref.logic.l10n.Localization;

import com.dlsc.gemsfx.EnhancedPasswordField;
import com.tobiasdiez.easybind.EasyBind;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class GitTab extends AbstractPreferenceTabView<GitTabViewModel> {
    private static final String GITHUB_PAT_DOCS_URL =
            "https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens";

    public GitTab() {
        this.viewModel = new GitTabViewModel(preferences.getGitPreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Git");
    }

    private void buildView() {
        EnhancedPasswordField pat = PasswordFieldEditor.create(viewModel.patProperty()).withRevealButton().withClearButton().field();

        setContent(form()
                .section(Localization.lang("Authentication"), authentication -> authentication
                        .stringField(Localization.lang("Username"), viewModel.usernameProperty())
                        .field(Localization.lang("PAT"), pat,
                                field -> field.tooltip(Localization.lang("Personal Access Token"))
                                              .help(GITHUB_PAT_DOCS_URL))
                        .custom(buildPersistPatCheckBox()))

                .section(Localization.lang("Automatic synchronization"), automatic -> automatic
                        .stringField(Localization.lang("Pull interval (minutes)"), viewModel.pullIntervalInMinutesProperty(),
                                field -> field.validate(viewModel.pullIntervalValidationStatus())))

                .build());
    }

    private Node buildPersistPatCheckBox() {
        CheckBox persistPat = new CheckBox(Localization.lang("Persist PAT between sessions"));
        persistPat.selectedProperty().bindBidirectional(viewModel.persistPatProperty());
        persistPat.disableProperty().bind(viewModel.passwordPersistAvailable().not());

        // A disabled node swallows mouse events and so never shows its tooltip; the wrapper carries it instead.
        // It is a SplitPane because setTooltip needs a Control, which Pane and StackPane are not.
        SplitPane wrapper = new SplitPane(persistPat);
        EasyBind.subscribe(viewModel.passwordPersistAvailable(), available ->
                wrapper.setTooltip(available ? null : new Tooltip(Localization.lang("Credential store not available."))));
        return wrapper;
    }
}
