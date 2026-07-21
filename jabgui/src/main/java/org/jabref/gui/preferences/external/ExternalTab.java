package org.jabref.gui.preferences.external;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.forms.AbstractFormTabView;
import org.jabref.gui.push.GuiPushToApplication;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.SearchableComboBox;

public class ExternalTab extends AbstractFormTabView<ExternalTabViewModel> {

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    // Kept as fields so validation can be attached after the grids are assembled.
    private TextField citeCommand;
    private TextField customTerminalCommand;
    private TextField customFileBrowserCommand;

    public ExternalTab() {
        this.viewModel = new ExternalTabViewModel(dialogService, preferences);
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("External programs");
    }

    private void buildView() {
        getChildren().add(form()
                .title(Localization.lang("External programs"))

                .section(Localization.lang("Sending of emails"))
                .stringField(Localization.lang("Subject for sending an email with references"), viewModel.eMailReferenceSubjectProperty())
                .stringField(Localization.lang("Email address for sending to Kindle"), viewModel.kindleEmailProperty())
                .checkbox(Localization.lang("Automatically open folders of attached files"), viewModel.autoOpenAttachedFoldersProperty())

                .section(Localization.lang("Push applications"))
                .custom(buildPushGrid())

                .section(Localization.lang("Custom applications"))
                .custom(buildCustomApplicationsGrid())

                .build());

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> {
            validationVisualizer.initVisualization(viewModel.terminalCommandValidationStatus(), customTerminalCommand);
            validationVisualizer.initVisualization(viewModel.fileBrowserCommandValidationStatus(), customFileBrowserCommand);
            validationVisualizer.initVisualization(viewModel.citeCommandValidationStatus(), citeCommand);
        });
    }

    private Node buildPushGrid() {
        SearchableComboBox<GuiPushToApplication> pushToApplicationCombo = new SearchableComboBox<>();
        new ViewModelListCellFactory<GuiPushToApplication>()
                .withText(GuiPushToApplication::getDisplayName)
                .withIcon(GuiPushToApplication::getApplicationIcon)
                .install(pushToApplicationCombo);
        pushToApplicationCombo.setPrefWidth(200.0);
        pushToApplicationCombo.itemsProperty().bind(viewModel.pushToApplicationsListProperty());
        pushToApplicationCombo.valueProperty().bindBidirectional(viewModel.selectedPushToApplication());

        Button applicationSettings = ControlHelper.narrowIconButton(IconTheme.JabRefIcons.PREFERENCES, Localization.lang("Application settings"), viewModel::pushToApplicationSettings);

        citeCommand = new TextField();
        citeCommand.setPrefWidth(300.0);
        citeCommand.textProperty().bindBidirectional(viewModel.citeCommandProperty());

        Button citeHelp = new Button();
        citeHelp.getStyleClass().addAll("icon-button", "narrow");
        citeHelp.setPrefSize(20.0, 20.0);
        new ActionFactory().configureIconButton(
                StandardActions.HELP_PUSH_TO_APPLICATION,
                new HelpAction(HelpFile.PUSH_TO_APPLICATION, dialogService, preferences.getExternalApplicationsPreferences()),
                citeHelp);
        Button resetCite = ControlHelper.narrowIconButton(IconTheme.JabRefIcons.REFRESH, Localization.lang("Reset to default"), viewModel::resetCiteCommandToDefault);

        GridPane grid = new GridPane();
        grid.setHgap(4.0);
        grid.setVgap(4.0);
        grid.setAlignment(Pos.CENTER_LEFT);
        grid.add(new Label(Localization.lang("Application to push entries to")), 0, 0);
        grid.add(pushToApplicationCombo, 1, 0);
        grid.add(applicationSettings, 2, 0);
        grid.add(new Label(Localization.lang("Cite command")), 0, 1);
        grid.add(citeCommand, 1, 1);
        grid.add(citeHelp, 2, 1);
        grid.add(resetCite, 3, 1);
        return grid;
    }

    private Node buildCustomApplicationsGrid() {
        String dirPlaceholderHint = Localization.lang("Note: Use the placeholder %DIR% for the location of the opened library file.");

        CheckBox useCustomTerminal = new CheckBox(Localization.lang("Use custom terminal emulator"));
        useCustomTerminal.selectedProperty().bindBidirectional(viewModel.useCustomTerminalProperty());
        customTerminalCommand = commandField(dirPlaceholderHint, viewModel.customTerminalCommandProperty(), useCustomTerminal);
        Button terminalBrowse = browseButton(useCustomTerminal, viewModel::customTerminalBrowse);

        CheckBox useCustomFileBrowser = new CheckBox(Localization.lang("Use custom file browser"));
        useCustomFileBrowser.selectedProperty().bindBidirectional(viewModel.useCustomFileBrowserProperty());
        customFileBrowserCommand = commandField(dirPlaceholderHint, viewModel.customFileBrowserCommandProperty(), useCustomFileBrowser);
        Button fileBrowserBrowse = browseButton(useCustomFileBrowser, viewModel::customFileBrowserBrowse);

        GridPane grid = new GridPane();
        grid.setHgap(4.0);
        grid.setVgap(4.0);
        grid.setAlignment(Pos.CENTER_LEFT);
        ColumnConstraints checkColumn = new ColumnConstraints();
        checkColumn.setMinWidth(50.0);
        checkColumn.setPrefWidth(200.0);
        ColumnConstraints commandColumn = new ColumnConstraints();
        commandColumn.setHgrow(Priority.ALWAYS);
        commandColumn.setMinWidth(100.0);
        grid.getColumnConstraints().addAll(checkColumn, commandColumn, new ColumnConstraints());
        grid.add(useCustomTerminal, 0, 0);
        grid.add(customTerminalCommand, 1, 0);
        grid.add(terminalBrowse, 2, 0);
        grid.add(useCustomFileBrowser, 0, 1);
        grid.add(customFileBrowserCommand, 1, 1);
        grid.add(fileBrowserBrowse, 2, 1);
        return grid;
    }

    private TextField commandField(String tooltip, javafx.beans.property.StringProperty value, CheckBox enabler) {
        TextField field = new TextField();
        field.textProperty().bindBidirectional(value);
        field.setTooltip(new Tooltip(tooltip));
        field.disableProperty().bind(enabler.selectedProperty().not());
        return field;
    }

    private Button browseButton(CheckBox enabler, Runnable action) {
        Button button = ControlHelper.narrowIconButton(IconTheme.JabRefIcons.OPEN, Localization.lang("Browse"), action);
        button.disableProperty().bind(enabler.selectedProperty().not());
        return button;
    }
}
