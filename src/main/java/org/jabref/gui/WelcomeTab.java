package org.jabref.gui;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.importer.ImportCommand;
import org.jabref.gui.importer.NewDatabaseAction;
import org.jabref.gui.shared.ConnectToSharedDatabaseCommand;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WelcomeTab extends Tab {
    String fontStyle = """
                    -fx-text-fill: #007ACC;
                    -fx-font-weight: bold;
                    -fx-font-size: 16px;
                    -fx-padding: 10px 20px;
                    -fx-font-family: Arial;
                """;
    String hover = """
                   -fx-text-fill: #005FA9;
                   -fx-font-weight: bold;
                   -fx-font-size: 16px;
                   -fx-padding: 10px 20px;
                   -fx-font-family: Arial;""";

    String headings = """
                   -fx-text-fill: #999999;
                   -fx-font-weight: bold;
                   -fx-font-size: 20px;
                   -fx-padding: 10px 20px;
                   -fx-font-family: Arial;""";


    private TabPane tabPane;
    //private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeTab.class);
    private JabRefFrame jabRefFrame;
    private Tab welcome;
    private SplitPane splitPane;
    private final PreferencesService preferencesService;

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;
    VBox items = new VBox();
    VBox welcomeTabItems;
    ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());
    Label newLibraryLabel;
    Label openLibrarylabel;
    Label connectToDatabase;

    Label importNewLib;

    private static WelcomeTab welcomeTabInstance = null;

    public static WelcomeTab getInstance(TabPane welcomeTabPane, PreferencesService preferencesService,
                                         StateManager stateManager,
                                         FileUpdateMonitor fileUpdateMonitor,
                                         TaskExecutor taskExecutor,
                                         DialogService dialogService) {
        if (welcomeTabInstance == null) {
            welcomeTabInstance = new WelcomeTab(welcomeTabPane, preferencesService, stateManager, fileUpdateMonitor, taskExecutor, dialogService);
        }
        return welcomeTabInstance;
    }

    private WelcomeTab(TabPane tabPane, PreferencesService preferencesService,
                       StateManager stateManager,
                       FileUpdateMonitor fileUpdateMonitor,
                       TaskExecutor taskExecutor,
                       DialogService dialogService) {
        this.tabPane = tabPane;
        this.preferencesService = preferencesService;

        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;

        initialiseWelcomeTab();
        runLabelTasks();
    }

    public void addStartLabel() {
        Label start = new Label("Start:");
        start.setStyle(headings);
//        start.setFont(Font.font("Arial", FontWeight.BOLD, 30));


        start.setTranslateX(start.getTranslateX() - 90);
        items.getChildren().add(start);
        items.setAlignment(Pos.CENTER_LEFT);
        addNewLibraryButton();
//        addOpenLibraryButton();
        addConnectDataBaseButton();
        addImportLibraryButton();
    }

    public void initialiseWelcomeTab() {
        welcome = new Tab("Welcome to JabRef");
        welcomeTabItems = new VBox();
        // Need to set Padding for the Elements inside the Veritcal Box
        welcomeTabItems.setPadding(new Insets(10));
        welcomeTabItems.setAlignment(Pos.BASELINE_CENTER);
        // Adding the title for teh Welcome Tab
        Label title = new Label();
        title.setText("Welcome to JabRef – Your Bibliographic Management and Citation Tool");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        welcomeTabItems.getChildren().add(title);
        welcome.setContent(welcomeTabItems);
        tabPane.getTabs().add(welcome);

        addStartLabel();
        items.setAlignment(Pos.CENTER);
        welcomeTabItems.getChildren().add(items);
    }

    public void addNewLibraryButton() {
        newLibraryLabel = new Label("New Library");
        newLibraryLabel.setStyle(fontStyle);
        newLibraryLabel.setOnMouseEntered(event -> {
            newLibraryLabel.setStyle(hover);
        });
        newLibraryLabel.setOnMouseExited(event -> {
            newLibraryLabel.setStyle(fontStyle);
        });
        newLibraryLabel.setAlignment(Pos.CENTER);
        //VBox.setMargin(newLibraryLabel, new javafx.geometry.Insets(0, 0, 0, 20));
        items.getChildren().add(newLibraryLabel);

    }

    public void addOpenLibraryButton() {
        openLibrarylabel = new Label("Open Library");
        openLibrarylabel.setStyle(fontStyle);
        openLibrarylabel.setOnMouseEntered(event -> {
            openLibrarylabel.setStyle(hover);
        });
        openLibrarylabel.setOnMouseExited(event -> {
            openLibrarylabel.setStyle(fontStyle);
        });
        openLibrarylabel.setAlignment(Pos.CENTER);

        items.getChildren().add(openLibrarylabel);
    }

    public void addConnectDataBaseButton() {
        connectToDatabase = new Label("Connect to Shared Database");
        connectToDatabase.setStyle(fontStyle);
        connectToDatabase.setOnMouseEntered(event -> {
            connectToDatabase.setStyle(hover);
        });
        connectToDatabase.setOnMouseExited(event -> {
            connectToDatabase.setStyle(fontStyle);
        });
        connectToDatabase.setAlignment(Pos.CENTER);

        items.getChildren().add(connectToDatabase);
    }

    public void addImportLibraryButton() {
        importNewLib = new Label("Import into New Library");
        importNewLib.setStyle(fontStyle);
        importNewLib.setOnMouseEntered(event -> {
            importNewLib.setStyle(hover);
        });
        importNewLib.setOnMouseExited(event -> {
            importNewLib.setStyle(fontStyle);
        });
        importNewLib.setAlignment(Pos.CENTER);

        items.getChildren().add(importNewLib);
    }

    public void runLabelTasks() {
        newLibraryLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new NewDatabaseAction(JabRefGUI.getMainFrame(), preferencesService).execute();
            }
        });
        connectToDatabase.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new ConnectToSharedDatabaseCommand(JabRefGUI.getMainFrame()).execute();
            }
        });

        importNewLib.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                new ImportCommand(JabRefGUI.getMainFrame(), ImportCommand.ImportMethod.AS_NEW, preferencesService, stateManager, fileUpdateMonitor, taskExecutor, dialogService).execute();
            }
        });
    }

    public void consumeWelcomeTab() {
        try {
            if (tabPane != null && welcome != null) {
                tabPane.getTabs().remove(welcome);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //LOGGER.error("Cannot consume Welcome tab", e);
        }
    }



}

