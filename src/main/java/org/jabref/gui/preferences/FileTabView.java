package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.NewLineSeperator;

import com.airhacks.afterburner.views.ViewLoader;

public class FileTabView extends VBox implements PrefsTab {

    @FXML private CheckBox openLastStartup;
    @FXML private CheckBox backupOldFile;
    @FXML private TextField noWrapFiles;
    @FXML private RadioButton resolveStringsBibTex;
    @FXML private ToggleGroup stringsResolveToggleGroup;
    @FXML private RadioButton resolveStringsAll;
    @FXML private TextField resolvStringsExcept;
    @FXML private ComboBox<NewLineSeperator> newLineSeparator;
    @FXML private CheckBox alwaysReformatBib;
    @FXML private TextField mainFileDir;
    @FXML private CheckBox useBibLocationAsPrimary;
    @FXML private Button autolinkRegexHelp;
    @FXML private RadioButton autolinkFilesWithBibtex;
    @FXML private ToggleGroup autolinkToggleGroup;
    @FXML private RadioButton autolinkFilesOnlyBibtex;
    @FXML private RadioButton autolinkUseRegex;
    @FXML private TextField autolinkRegexTerm;
    @FXML private CheckBox searchFilesOnOpen;
    @FXML private CheckBox openBrowseOnCreate;
    @FXML private CheckBox autosaveLocalLibraries;
    @FXML private Button autosaveLocalLibrariesHelp;

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    private FileTabViewModel viewModel;

    public FileTabView (DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        viewModel = new FileTabViewModel(dialogService, preferences);

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP_REGEX_SEARCH, new HelpAction(HelpFile.REGEX_SEARCH),autolinkRegexHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AUTOSAVE),autosaveLocalLibrariesHelp);
    }

    @Override
    public Node getBuilder() {
        return this;
    }

    @Override
    public void setValues() {
        // Done by bindings
    }

    @Override
    public void storeSettings() {
        viewModel.storeSettings();
    }

    @Deprecated
    @Override
    public boolean validateSettings() {
        return viewModel.validateSettings();
    }

    @Override
    public String getTabName() {
        return Localization.lang("File");
    }

    public void mainFileDirBrowse() { viewModel.mainFileDirBrowse(); }
}
