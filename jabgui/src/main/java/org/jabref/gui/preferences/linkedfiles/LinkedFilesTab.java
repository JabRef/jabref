package org.jabref.gui.preferences.linkedfiles;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class LinkedFilesTab extends AbstractPreferenceTabView<LinkedFilesTabViewModel> implements PreferencesTab {

    // Multiplier for row height based on font size
    private static final double FONT_HEIGHT_MULTIPLIER = 2.5;

    // Default row height if font is not available
    private static final double DEFAULT_ROW_HEIGHT = 30.0;

    // Estimate for header height (used in table prefHeight calculation)
    private static final double HEADER_HEIGHT_ESTIMATE = 1.1;

    // Minimum number of (empty) rows to reserve, so an empty table doesn't collapse to just the header
    private static final int MIN_ROW_COUNT = 1;

    @FXML private TextField mainFileDirectory;
    @FXML private RadioButton useMainFileDirectory;
    @FXML private RadioButton useBibLocationAsPrimary;
    @FXML private Button browseDirectory;
    @FXML private Button autolinkRegexHelp;
    @FXML private RadioButton autolinkFileStartsBibtex;
    @FXML private RadioButton autolinkFileExactBibtex;
    @FXML private RadioButton autolinkUseRegex;
    @FXML private RadioButton openFileExplorerInFilesDirectory;
    @FXML private RadioButton openFileExplorerInLastDirectory;
    @FXML private TextField autolinkRegexKey;

    @FXML private CheckBox fulltextIndex;
    @FXML private CheckBox autoRenameFilesOnChange;

    @FXML private ComboBox<String> fileNamePattern;
    @FXML private TextField fileDirectoryPattern;
    @FXML private CheckBox confirmLinkedFileDelete;
    @FXML private CheckBox moveToTrash;
    @FXML private CheckBox adjustLinkedFilesOnTransfer;
    @FXML private CheckBox copyLinkedFilesOnTransfer;
    @FXML private CheckBox moveLinkedFilesOnTransfer;

    @FXML private TableView<DirectoryMappingItem> directoryMappingTable;
    @FXML private TableColumn<DirectoryMappingItem, String> directoryMappingDirectoryColumn;
    @FXML private TableColumn<DirectoryMappingItem, String> directoryMappingMappedDirectoryColumn;
    @FXML private TableColumn<DirectoryMappingItem, Boolean> directoryMappingDeleteColumn;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public LinkedFilesTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Linked files");
    }

    public void initialize() {
        this.viewModel = new LinkedFilesTabViewModel(dialogService, preferences);

        mainFileDirectory.textProperty().bindBidirectional(viewModel.mainFileDirectoryProperty());
        mainFileDirectory.disableProperty().bind(viewModel.useBibLocationAsPrimaryProperty());
        browseDirectory.disableProperty().bind(viewModel.useBibLocationAsPrimaryProperty());
        useBibLocationAsPrimary.selectedProperty().bindBidirectional(viewModel.useBibLocationAsPrimaryProperty());
        useMainFileDirectory.selectedProperty().bindBidirectional(viewModel.useMainFileDirectoryProperty());

        moveToTrash.selectedProperty().bindBidirectional(viewModel.moveToTrashProperty());
        moveToTrash.setDisable(!NativeDesktop.get().moveToTrashSupported());

        autolinkFileStartsBibtex.selectedProperty().bindBidirectional(viewModel.autolinkFileStartsBibtexProperty());
        autolinkFileExactBibtex.selectedProperty().bindBidirectional(viewModel.autolinkFileExactBibtexProperty());
        autolinkUseRegex.selectedProperty().bindBidirectional(viewModel.autolinkUseRegexProperty());
        autolinkRegexKey.textProperty().bindBidirectional(viewModel.autolinkRegexKeyProperty());
        autolinkRegexKey.disableProperty().bind(autolinkUseRegex.selectedProperty().not());
        fulltextIndex.selectedProperty().bindBidirectional(viewModel.fulltextIndexProperty());
        autoRenameFilesOnChange.selectedProperty().bindBidirectional(viewModel.autoRenameFilesOnChangeProperty());
        fileNamePattern.valueProperty().bindBidirectional(viewModel.fileNamePatternProperty());
        fileNamePattern.itemsProperty().bind(viewModel.defaultFileNamePatternsProperty());
        fileDirectoryPattern.textProperty().bindBidirectional(viewModel.fileDirectoryPatternProperty());
        confirmLinkedFileDelete.selectedProperty().bindBidirectional(viewModel.confirmLinkedFileDeleteProperty());
        openFileExplorerInFilesDirectory.selectedProperty().bindBidirectional(viewModel.openFileExplorerInFilesDirectoryProperty());
        openFileExplorerInLastDirectory.selectedProperty().bindBidirectional(viewModel.openFileExplorerInLastDirectoryProperty());
        adjustLinkedFilesOnTransfer.selectedProperty().bindBidirectional(viewModel.adjustLinkedFilesOnTransferProperty());
        copyLinkedFilesOnTransfer.selectedProperty().bindBidirectional(viewModel.copyLinkedFilesOnTransferProperty());
        moveLinkedFilesOnTransfer.selectedProperty().bindBidirectional(viewModel.moveFilesOnTransferProperty());

        EasyBind.listen(adjustLinkedFilesOnTransfer.selectedProperty(), (_, _, selected) -> {
            copyLinkedFilesOnTransfer.setDisable(!selected);
            moveLinkedFilesOnTransfer.setDisable(!selected);
        });

        ActionFactory actionFactory = new ActionFactory();
        actionFactory.configureIconButton(StandardActions.HELP_REGEX_SEARCH, new HelpAction(HelpFile.REGEX_SEARCH, dialogService, preferences.getExternalApplicationsPreferences()), autolinkRegexHelp);

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(viewModel.mainFileDirValidationStatus(), mainFileDirectory));

        directoryMappingTable.setItems(viewModel.getDirectoryMappings());

        directoryMappingDirectoryColumn.setCellValueFactory(data -> data.getValue().directoryProperty());
        directoryMappingDirectoryColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        directoryMappingMappedDirectoryColumn.setCellValueFactory(data -> data.getValue().mappedDirectoryProperty());
        directoryMappingMappedDirectoryColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        directoryMappingDeleteColumn.setCellValueFactory(data -> BindingsHelper.constantOf(true));
        new ValueTableCellFactory<DirectoryMappingItem, Boolean>()
                .withGraphic(none -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withOnMouseClickedEvent((item, none) -> event -> viewModel.removeDirectoryMapping(item))
                .install(directoryMappingDeleteColumn);

        // Dynamic height based on font size and number of items, so the table doesn't reserve empty striped rows
        DoubleBinding rowHeight = Bindings.createDoubleBinding(
                () -> fulltextIndex.getFont() != null ? fulltextIndex.getFont().getSize() * FONT_HEIGHT_MULTIPLIER : DEFAULT_ROW_HEIGHT,
                fulltextIndex.fontProperty());
        directoryMappingTable.fixedCellSizeProperty().bind(rowHeight);
        directoryMappingTable.prefHeightProperty().bind(
                Bindings.max(Bindings.size(directoryMappingTable.getItems()), MIN_ROW_COUNT)
                        .add(HEADER_HEIGHT_ESTIMATE)
                        .multiply(rowHeight));
    }

    public void mainFileDirBrowse() {
        viewModel.mainFileDirBrowse();
    }

    @FXML
    private void addDirectoryMapping() {
        viewModel.addDirectoryMapping();
    }
}
