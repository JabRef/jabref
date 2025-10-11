package org.jabref.gui.libraryproperties.general;

import java.nio.charset.Charset;
import java.nio.file.Path;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.libraryproperties.AbstractPropertiesTabView;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;

import static org.jabref.gui.icon.IconTheme.JabRefIcons.ABSOLUTE_PATH;
import static org.jabref.gui.icon.IconTheme.JabRefIcons.RELATIVE_PATH;

public class GeneralPropertiesView extends AbstractPropertiesTabView<GeneralPropertiesViewModel> {
    @FXML private ComboBox<Charset> encoding;
    @FXML private ComboBox<BibDatabaseMode> databaseMode;
    @FXML private TextField librarySpecificFileDirectory;
    @FXML private TextField userSpecificFileDirectory;
    @FXML private TextField latexFileDirectory;
    @FXML private Button libSpecificFileDirSwitchId;
    @FXML private Button userSpecificFileDirSwitchId;
    @FXML private Button laTexSpecificFileDirSwitchId;
    @FXML private JabRefIconView libSpecificFileDirSwitchIcon;
    @FXML private JabRefIconView userSpecificFileDirSwitchIcon;
    @FXML private JabRefIconView laTexSpecificFileDirSwitchIcon;
    @FXML private Tooltip libSpecificFileDirSwitchTooltip;
    @FXML private Tooltip userSpecificFileDirSwitchTooltip;
    @FXML private Tooltip laTexSpecificFileDirSwitchTooltip;
    @FXML private Tooltip userSpecificFileDirectoryTooltip;
    @FXML private Tooltip latexFileDirectoryTooltip;
    @FXML private Tooltip librarySpecificFileDirectoryTooltip;

    private final ControlsFxVisualizer librarySpecificFileDirectoryValidationVisualizer = new ControlsFxVisualizer();
    private final ControlsFxVisualizer userSpecificFileDirectoryValidationVisualizer = new ControlsFxVisualizer();
    private final ControlsFxVisualizer latexFileDirectoryValidationVisualizer = new ControlsFxVisualizer();
    private final String switchToRelativeText = Localization.lang("Switch to relative path: converts the path to a relative path.");
    private final String switchToAbsoluteText = Localization.lang("Switch to absolute path: converts the path to an absolute path.");

    @Inject private CliPreferences preferences;

    public GeneralPropertiesView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("General");
    }

    public void initialize() {
        this.viewModel = new GeneralPropertiesViewModel(databaseContext, dialogService, preferences);

        new ViewModelListCellFactory<Charset>()
                .withText(Charset::displayName)
                .install(encoding);
        encoding.disableProperty().bind(viewModel.encodingDisableProperty());
        encoding.itemsProperty().bind(viewModel.encodingsProperty());
        encoding.valueProperty().bindBidirectional(viewModel.selectedEncodingProperty());

        new ViewModelListCellFactory<BibDatabaseMode>()
                .withText(BibDatabaseMode::getFormattedName)
                .install(databaseMode);
        databaseMode.itemsProperty().bind(viewModel.databaseModesProperty());
        databaseMode.valueProperty().bindBidirectional(viewModel.selectedDatabaseModeProperty());

        librarySpecificFileDirectoryTooltip.setText(Localization.lang("Library-specific file directory"));
        librarySpecificFileDirectory.textProperty().bindBidirectional(viewModel.librarySpecificDirectoryProperty());

        userSpecificFileDirectory.textProperty().bindBidirectional(viewModel.userSpecificFileDirectoryProperty());
        latexFileDirectory.textProperty().bindBidirectional(viewModel.laTexFileDirectoryProperty());

        userSpecificFileDirectoryTooltip.setText(Localization.lang("User-specific file directory: %0", preferences.getFilePreferences().getUserAndHost()));
        userSpecificFileDirectory.setTooltip(userSpecificFileDirectoryTooltip);

        librarySpecificFileDirectoryValidationVisualizer.setDecoration(new IconValidationDecorator());
        userSpecificFileDirectoryValidationVisualizer.setDecoration(new IconValidationDecorator());
        latexFileDirectoryValidationVisualizer.setDecoration(new IconValidationDecorator());

        libSpecificFileDirSwitchId.setDisable(this.databaseContext.getDatabasePath().isEmpty());
        userSpecificFileDirSwitchId.setDisable(this.databaseContext.getDatabasePath().isEmpty());
        laTexSpecificFileDirSwitchId.setDisable(this.databaseContext.getDatabasePath().isEmpty());

        librarySpecificFileDirectory.textProperty().addListener((_, _, newValue) -> {
            boolean isAbsolute = Path.of(newValue).isAbsolute();
            libSpecificFileDirSwitchIcon.setGlyph(isAbsolute ? RELATIVE_PATH : ABSOLUTE_PATH);
            libSpecificFileDirSwitchTooltip.setText(isAbsolute ? switchToRelativeText : switchToAbsoluteText);
            librarySpecificFileDirectoryTooltip.setText(newValue.trim().isEmpty() ?
                                                        Localization.lang("Library-specific file directory") : Localization.lang("Library-specific file directory: %0", newValue));
        });
        userSpecificFileDirectory.textProperty().addListener((_, _, newValue) -> {
            boolean isAbsolute = Path.of(newValue).isAbsolute();
            userSpecificFileDirSwitchIcon.setGlyph(isAbsolute ? RELATIVE_PATH : ABSOLUTE_PATH);
            userSpecificFileDirSwitchTooltip.setText(isAbsolute ? switchToRelativeText : switchToAbsoluteText);
        });

        latexFileDirectory.textProperty().addListener((_, _, newValue) -> {
            boolean isAbsolute = Path.of(newValue).isAbsolute();
            laTexSpecificFileDirSwitchIcon.setGlyph(isAbsolute ? RELATIVE_PATH : ABSOLUTE_PATH);
            laTexSpecificFileDirSwitchTooltip.setText(isAbsolute ? switchToRelativeText : switchToAbsoluteText);
            latexFileDirectoryTooltip.setText(newValue.trim().isEmpty()
                                              ? Localization.lang("LaTeX file directory") : Localization.lang("LaTeX file directory: %0", newValue));
        });

        Platform.runLater(() -> {
            librarySpecificFileDirectoryValidationVisualizer.initVisualization(viewModel.librarySpecificFileDirectoryStatus(), librarySpecificFileDirectory);
            userSpecificFileDirectoryValidationVisualizer.initVisualization(viewModel.userSpecificFileDirectoryStatus(), userSpecificFileDirectory);
            latexFileDirectoryValidationVisualizer.initVisualization(viewModel.laTexFileDirectoryStatus(), latexFileDirectory);

            librarySpecificFileDirectory.requestFocus();
        });
    }

    @FXML
    public void browseLibrarySpecificFileDirectory() {
        viewModel.browseLibrarySpecificDir();
    }

    @FXML
    public void browseUserSpecificFileDirectory() {
        viewModel.browseUserDir();
    }

    @FXML
    void browseLatexFileDirectory() {
        viewModel.browseLatexDir();
    }

    @FXML
    void libSpecificFileDirPathSwitch() {
        viewModel.togglePath(viewModel.librarySpecificDirectoryProperty());
    }

    @FXML
    void userSpecificFileDirPathSwitch() {
        viewModel.togglePath(viewModel.userSpecificFileDirectoryProperty());
    }

    @FXML
    void laTexSpecificFileDirPathSwitch() {
        viewModel.togglePath(viewModel.laTexFileDirectoryProperty());
    }
}
