package org.jabref.gui.externalfiles;

import java.io.BufferedWriter;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.importer.UnlinkedFilesCrawler;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.ViewModelTreeCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GUI Dialog for the feature "Find unlinked files".
 */
public class FindUnlinkedFilesDialog extends BaseDialog<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FindUnlinkedFilesDialog.class);
    private final JabRefFrame frame;
    private final BibDatabaseContext databaseContext;
    private final ImportHandler importHandler;
    private final JabRefPreferences preferences = Globals.prefs;
    private final DialogService dialogService;
    private Button buttonScan;
    private Button buttonExport;
    private Button buttonApply;
    private TextField textfieldDirectoryPath;
    private TreeView<FileNodeWrapper> tree;
    private ComboBox<FileChooser.ExtensionFilter> comboBoxFileTypeSelection;
    private VBox panelSearchProgress;
    private BackgroundTask findUnlinkedFilesTask;

    public FindUnlinkedFilesDialog(JabRefFrame frame) {
        super();
        this.setTitle(Localization.lang("Find unlinked files"));
        this.frame = frame;
        dialogService = frame.getDialogService();

        databaseContext = frame.getCurrentBasePanel().getBibDatabaseContext();
        importHandler = new ImportHandler(
                dialogService,
                databaseContext,
                ExternalFileTypes.getInstance(),
                Globals.prefs.getFilePreferences(),
                Globals.prefs.getImportFormatPreferences(),
                Globals.prefs.getUpdateFieldPreferences(),
                Globals.getFileUpdateMonitor(),
                frame.getUndoManager()
        );

        initialize();
    }

    /**
     * Initializes the components, the layout, the data structure and the actions in this dialog.
     */
    private void initialize() {
        Button buttonBrowse = new Button(Localization.lang("Browse"));
        buttonBrowse.setTooltip(new Tooltip(Localization.lang("Opens the file browser.")));
        buttonBrowse.getStyleClass().add("text-button");
        buttonBrowse.setOnAction(e -> {
            DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                    .withInitialDirectory(preferences.get(JabRefPreferences.WORKING_DIRECTORY)).build();
            dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                         .ifPresent(selectedDirectory -> {
                             textfieldDirectoryPath.setText(selectedDirectory.toAbsolutePath().toString());
                             preferences.put(JabRefPreferences.WORKING_DIRECTORY, selectedDirectory.toAbsolutePath().toString());
                         });
        });

        buttonScan = new Button(Localization.lang("Scan directory"));
        buttonScan.setTooltip(new Tooltip((Localization.lang("Searches the selected directory for unlinked files."))));
        buttonScan.setOnAction(e -> startSearch());
        buttonScan.setDefaultButton(true);
        buttonScan.setPadding(new Insets(5, 0, 0, 0));

        buttonExport = new Button(Localization.lang("Export selected entries"));
        buttonExport.setTooltip(new Tooltip(Localization.lang("Export to text file.")));
        buttonExport.getStyleClass().add("text-button");
        buttonExport.setDisable(true);
        buttonExport.setOnAction(e -> startExport());

        ButtonType buttonTypeImport = new ButtonType(Localization.lang("Import"), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().setAll(
                buttonTypeImport,
                ButtonType.CANCEL
        );
        buttonApply = (Button) getDialogPane().lookupButton(buttonTypeImport);
        buttonApply.setTooltip(new Tooltip((Localization.lang("Starts the import of BibTeX entries."))));
        buttonApply.setDisable(true);

        /* Actions for the TreeView */
        Button buttonOptionSelectAll = new Button();
        buttonOptionSelectAll.setText(Localization.lang("Select all"));
        buttonOptionSelectAll.getStyleClass().add("text-button");
        buttonOptionSelectAll.setOnAction(event -> {
            CheckBoxTreeItem<FileNodeWrapper> root = (CheckBoxTreeItem<FileNodeWrapper>) tree.getRoot();
            // Need to toggle a twice to make sure everything is selected
            root.setSelected(true);
            root.setSelected(false);
            root.setSelected(true);
        });
        Button buttonOptionDeselectAll = new Button();
        buttonOptionDeselectAll.setText(Localization.lang("Unselect all"));
        buttonOptionDeselectAll.getStyleClass().add("text-button");
        buttonOptionDeselectAll.setOnAction(event -> {
            CheckBoxTreeItem<FileNodeWrapper> root = (CheckBoxTreeItem<FileNodeWrapper>) tree.getRoot();
            // Need to toggle a twice to make sure nothing is selected
            root.setSelected(false);
            root.setSelected(true);
            root.setSelected(false);
        });
        Button buttonOptionExpandAll = new Button();
        buttonOptionExpandAll.setText(Localization.lang("Expand all"));
        buttonOptionExpandAll.getStyleClass().add("text-button");
        buttonOptionExpandAll.setOnAction(event -> {
            CheckBoxTreeItem<FileNodeWrapper> root = (CheckBoxTreeItem<FileNodeWrapper>) tree.getRoot();
            expandTree(root, true);
        });
        Button buttonOptionCollapseAll = new Button();
        buttonOptionCollapseAll.setText(Localization.lang("Collapse all"));
        buttonOptionCollapseAll.getStyleClass().add("text-button");
        buttonOptionCollapseAll.setOnAction(event -> {
            CheckBoxTreeItem<FileNodeWrapper> root = (CheckBoxTreeItem<FileNodeWrapper>) tree.getRoot();
            expandTree(root, false);
            root.setExpanded(true);
        });

        textfieldDirectoryPath = new TextField();
        Path initialPath = databaseContext.getFirstExistingFileDir(preferences.getFilePreferences())
                                          .orElse(preferences.getWorkingDir());
        textfieldDirectoryPath.setText(initialPath.toAbsolutePath().toString());

        Label labelDirectoryDescription = new Label(Localization.lang("Select a directory where the search shall start."));
        Label labelFileTypesDescription = new Label(Localization.lang("Select file type:"));
        Label labelFilesDescription = new Label(Localization.lang("These files are not linked in the active library."));
        Label labelSearchingDirectoryInfo = new Label(Localization.lang("Searching file system..."));

        tree = new TreeView<>();
        tree.setPrefWidth(Double.POSITIVE_INFINITY);

        ScrollPane scrollPaneTree = new ScrollPane(tree);
        scrollPaneTree.setFitToWidth(true);

        ProgressIndicator progressBarSearching = new ProgressIndicator();
        progressBarSearching.setMaxSize(50, 50);

        setResultConverter(buttonPressed -> {
            if (buttonPressed == buttonTypeImport) {
                startImport();
            } else {
                if (findUnlinkedFilesTask != null) {
                    findUnlinkedFilesTask.cancel();
                }
            }
            return null;
        });

        new ViewModelTreeCellFactory<FileNodeWrapper>()
                .withText(node -> {
                    if (Files.isRegularFile(node.path)) {
                        // File
                        return node.path.getFileName().toString();
                    } else {
                        // Directory
                        return node.path.getFileName() + " (" + node.fileCount + " file" + (node.fileCount > 1 ? "s" : "") + ")";
                    }
                })
                .install(tree);
        List<FileChooser.ExtensionFilter> fileFilterList = Arrays.asList(
                FileFilterConverter.ANY_FILE,
                FileFilterConverter.toExtensionFilter(StandardFileType.PDF),
                FileFilterConverter.toExtensionFilter(StandardFileType.BIBTEX_DB)
        );

        comboBoxFileTypeSelection = new ComboBox<>(FXCollections.observableArrayList(fileFilterList));
        comboBoxFileTypeSelection.getSelectionModel().selectFirst();
        new ViewModelListCellFactory<FileChooser.ExtensionFilter>()
                .withText(fileFilter -> fileFilter.getDescription() + fileFilter.getExtensions().stream().collect(Collectors.joining(", ", " (", ")")))
                .withIcon(fileFilter -> ExternalFileTypes.getInstance().getExternalFileTypeByExt(fileFilter.getExtensions().get(0))
                                                         .map(ExternalFileType::getIcon)
                                                         .orElse(null))
                .install(comboBoxFileTypeSelection);

        panelSearchProgress = new VBox(5, labelSearchingDirectoryInfo, progressBarSearching);
        panelSearchProgress.toFront();
        panelSearchProgress.setVisible(false);

//        panelDirectory.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
//                Localization.lang("Select directory")));
//        panelFiles.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
//                Localization.lang("Select files")));
//        panelEntryTypesSelection.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
//                Localization.lang("BibTeX entry creation")));

        VBox panelDirectory = new VBox(5);
        panelDirectory.getChildren().setAll(
                labelDirectoryDescription,
                new HBox(10, textfieldDirectoryPath, buttonBrowse),
                new HBox(15, labelFileTypesDescription, comboBoxFileTypeSelection),
                buttonScan
        );
        HBox.setHgrow(textfieldDirectoryPath, Priority.ALWAYS);

        StackPane stackPaneTree = new StackPane(scrollPaneTree, panelSearchProgress);
        StackPane.setAlignment(panelSearchProgress, Pos.CENTER);
        BorderPane panelFiles = new BorderPane();
        panelFiles.setTop(labelFilesDescription);
        panelFiles.setCenter(stackPaneTree);
        panelFiles.setBottom(new HBox(5, buttonOptionSelectAll, buttonOptionDeselectAll, buttonOptionExpandAll, buttonOptionCollapseAll, buttonExport));

        VBox container = new VBox(20);
        container.getChildren().addAll(
                panelDirectory,
                panelFiles
        );
        container.setPrefWidth(600);
        getDialogPane().setContent(container);
    }

    /**
     * Expands or collapses the specified tree according to the <code>expand</code>-parameter.
     */
    private void expandTree(TreeItem<?> item, boolean expand) {
        if (item != null && !item.isLeaf()) {
            item.setExpanded(expand);
            for (TreeItem<?> child : item.getChildren()) {
                expandTree(child, expand);
            }
        }
    }

    /**
     * Starts the search of unlinked files according chosen directory and the file type selection. The search will
     * process in a separate thread and a progress indicator will be displayed.
     */
    private void startSearch() {
        Path directory = getSearchDirectory();
        FileFilter selectedFileFilter = FileFilterConverter.toFileFilter(comboBoxFileTypeSelection.getValue());

        findUnlinkedFilesTask = new UnlinkedFilesCrawler(directory, selectedFileFilter, databaseContext)
                .onRunning(() -> {
                    panelSearchProgress.setVisible(true);
                    buttonScan.setDisable(true);
                    tree.setRoot(null);
                })
                .onFinished(() -> {
                    panelSearchProgress.setVisible(false);
                    buttonScan.setDisable(false);
                })
                .onSuccess(root -> {
                    tree.setRoot(root);
                    root.setSelected(true);
                    root.setExpanded(true);

                    buttonApply.setDisable(false);
                    buttonExport.setDisable(false);
                    buttonScan.setDefaultButton(false);
                });
        findUnlinkedFilesTask.executeWith(Globals.TASK_EXECUTOR);
    }

    private Path getSearchDirectory() {
        Path directory = Paths.get(textfieldDirectoryPath.getText());
        if (Files.notExists(directory)) {
            directory = Paths.get(System.getProperty("user.dir"));
            textfieldDirectoryPath.setText(directory.toAbsolutePath().toString());
        }
        if (!Files.isDirectory(directory)) {
            directory = directory.getParent();
            textfieldDirectoryPath.setText(directory.toAbsolutePath().toString());
        }
        return directory;
    }

    /**
     * This will start the import of all file of all selected nodes in the file tree view.
     */
    private void startImport() {
        CheckBoxTreeItem<FileNodeWrapper> root = (CheckBoxTreeItem<FileNodeWrapper>) tree.getRoot();
        final List<Path> fileList = getFileListFromNode(root);

        if (fileList.isEmpty()) {
            return;
        }

        importHandler.importAsNewEntries(fileList);
    }

    /**
     * This starts the export of all files of all selected nodes in the file tree view.
     */
    private void startExport() {
        CheckBoxTreeItem<FileNodeWrapper> root = (CheckBoxTreeItem<FileNodeWrapper>) tree.getRoot();

        final List<Path> fileList = getFileListFromNode(root);
        if (fileList.isEmpty()) {
            return;
        }

        buttonExport.setVisible(false);
        buttonApply.setVisible(false);

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(preferences.get(JabRefPreferences.WORKING_DIRECTORY)).build();
        Optional<Path> exportPath = dialogService.showFileSaveDialog(fileDialogConfiguration);

        if (!exportPath.isPresent()) {
            buttonExport.setVisible(true);
            buttonApply.setVisible(true);
            return;
        }

        try (BufferedWriter writer =
                     Files.newBufferedWriter(exportPath.get(), StandardCharsets.UTF_8,
                             StandardOpenOption.CREATE)) {
            for (Path file : fileList) {
                writer.write(file.toString() + "\n");
            }
        } catch (IOException e) {
            LOGGER.warn("IO Error.", e);
        }

        buttonExport.setVisible(true);
        buttonApply.setVisible(true);
    }

    /**
     * Creates a list of all files (leaf nodes in the tree structure), which have been selected.
     *
     * @param node The root node representing a tree structure.
     */
    private List<Path> getFileListFromNode(CheckBoxTreeItem<FileNodeWrapper> node) {
        List<Path> filesList = new ArrayList<>();
        for (TreeItem<FileNodeWrapper> childNode : node.getChildren()) {
            CheckBoxTreeItem<FileNodeWrapper> child = (CheckBoxTreeItem<FileNodeWrapper>) childNode;
            if (child.isLeaf() && child.isSelected()) {
                Path nodeFile = child.getValue().path;
                if ((nodeFile != null) && Files.isRegularFile(nodeFile)) {
                    filesList.add(nodeFile);
                }
            }
        }
        return filesList;
    }

    public static class FileNodeWrapper {

        public final Path path;
        public final int fileCount;

        public FileNodeWrapper(Path path) {
            this(path, 0);
        }

        public FileNodeWrapper(Path path, int fileCount) {
            this.path = path;
            this.fileCount = fileCount;
        }
    }
}
