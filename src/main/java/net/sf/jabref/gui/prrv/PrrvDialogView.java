package net.sf.jabref.gui.prrv;

import java.util.Iterator;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import prefux.Constants;
import prefux.FxDisplay;
import prefux.Visualization;
import prefux.action.ActionList;
import prefux.action.RepaintAction;
import prefux.action.assignment.ColorAction;
import prefux.action.assignment.DataColorAction;
import prefux.action.assignment.NodeDegreeSizeAction;
import prefux.action.layout.graph.ForceDirectedLayout;
import prefux.activity.Activity;
import prefux.controls.DragControl;
import prefux.data.Graph;
import prefux.data.Table;
import prefux.data.expression.Predicate;
import prefux.data.expression.parser.ExpressionParser;
import prefux.render.DefaultRendererFactory;
import prefux.render.EdgeRenderer;
import prefux.render.ShapeRenderer;
import prefux.util.ColorLib;
import prefux.util.PrefuseLib;
import prefux.visual.NodeItem;
import prefux.visual.VisualItem;

/**
 *  Launch PRRV application
 */
public class PrrvDialogView {

    private final double WIDTH = 800;
    private final double HEIGHT = 600;
    private final String GROUP = "graph";

    public static boolean isStarted = false;
    private static BorderPane root = new BorderPane();
    public static FxDisplay fxDisplay;

    private Stage primaryStage = new Stage();


    private Visualization vis;

    private CheckBox keyCB = new CheckBox();
    private static DisplayControl control = new DisplayControl();


    public void show() {
        // -- 1. setup dialog -----------------------------------------------------
        root = new BorderPane();
        vis = new Visualization();
        primaryStage.setTitle("PRRV");

        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
        root.getStyleClass().add("fxDisplay");
        primaryStage.show();

        // Create tables for node and edge data, and configure their columns.
        Table nodeData = new Table();
        Table edgeData = new Table();

        nodeData.addColumn("id", int.class);
        nodeData.addColumn("bibtexkey", String.class);
        nodeData.addColumn("citeCount", int.class);
        nodeData.addColumn("location", String.class);


        edgeData.addColumn(Graph.DEFAULT_SOURCE_KEY, int.class);
        edgeData.addColumn(Graph.DEFAULT_TARGET_KEY, int.class);

        // -- 2. load the data ------------------------------------------------
        // Create Graph backed by those tables.  Note that I'm creating a directed graph here also.
        Graph graph = new Graph(nodeData, edgeData, true);
        ReferenceRelationship rr = new ReferenceRelationship();
        rr.parseBibTexForReferences(graph);

        // -- 3. the visualization --------------------------------------------

        vis.addGraph(GROUP, graph);

        // -- 4. the renderers and renderer factory ---------------------------
        // Different shaperenderer for intern and external nodes
        ShapeRenderer intern = new ShapeRenderer();
        intern.setFillMode(ShapeRenderer.GRADIENT_SPHERE);
        ShapeRenderer external = new ShapeRenderer();
        external.setFillMode(ShapeRenderer.GRADIENT_SPHERE);
        // Separate predicates for intern and external nodes
        DefaultRendererFactory rfa = new DefaultRendererFactory();
        Predicate internExp = ExpressionParser.predicate("location='I'");
        Predicate externalExp = ExpressionParser.predicate("location='E'");
        rfa.add(internExp, intern);
        rfa.add(externalExp, external);
        vis.setRendererFactory(rfa);

        // -- 5. the processing actions ---------------------------------------
        // Apply layout
        ActionList layout = new ActionList(Activity.INFINITY);
        layout.add(new ForceDirectedLayout("graph"));
        layout.add(new RepaintAction());
        vis.putAction("layout", layout);
        // Nodes are big as their in/output count of edges
        ActionList nodeActions = new ActionList();
        final String NODES = PrefuseLib.getGroupName(GROUP, Graph.NODES);
        NodeDegreeSizeAction size = new NodeDegreeSizeAction(NODES);
        nodeActions.add(size);

        // Color palettes
        int[] internPalette = new int[]{
                ColorLib.rgb(158, 0, 0),
                ColorLib.rgb(210, 0, 0),
                ColorLib.rgb(222, 3, 1),
                ColorLib.rgb(254, 89, 0),
                ColorLib.rgb(255, 137, 1),
                ColorLib.rgb(255, 183, 1),
                ColorLib.rgb(251, 206, 1),
                ColorLib.rgb(255, 255, 105)
        };

        int[] externalPalette = new int[]{
                ColorLib.rgb(30, 144, 255),
                ColorLib.rgb(28, 134, 238),
                ColorLib.rgb(24, 116, 205),
                ColorLib.rgb(16, 78, 139)
        };

        // Apply colors to the predicates
        ColorAction nStroke = new ColorAction("graph.nodes", VisualItem.STROKECOLOR);
        nStroke.setDefaultColor(ColorLib.gray(100));
        DataColorAction colorI = new DataColorAction(NODES, internExp, "citeCount",
                Constants.NUMERICAL, VisualItem.FILLCOLOR, internPalette);
        DataColorAction colorE = new DataColorAction(NODES, externalExp, "citeCount",
                Constants.NUMERICAL, VisualItem.FILLCOLOR, externalPalette);
        ColorAction edges = new ColorAction("graph.edges",
                VisualItem.STROKECOLOR, ColorLib.gray(255));
        ColorAction arrow = new ColorAction("graph.edges",
                VisualItem.FILLCOLOR, ColorLib.gray(200));
        ActionList color = new ActionList();
        color.add(nStroke);
        color.add(colorI);
        color.add(colorE);
        color.add(edges);
        color.add(arrow);
        vis.putAction("nodes", nodeActions);
        vis.putAction("color", color);

        // -- 6. the fxDisplay and interactive controls -------------------------
        fxDisplay = new FxDisplay(vis);
        fxDisplay.addControlListener(new DragControl());

        // Add arrowheads
        for (Polygon arrowHead:EdgeRenderer.arrowHeadList) {
            fxDisplay.getChildren().addAll(arrowHead);
        }

        //Bind pref Witdh and Height to according stage
        root.prefWidthProperty().bind(primaryStage.widthProperty());
        root.prefHeightProperty().bind(primaryStage.heightProperty());
        root.setStyle("-fx-background-color: white");
        root.setCenter(fxDisplay);
        // Setup zoom box
        root.setTop(showKeyCheckBox(fxDisplay));
        root.setBottom(buildControlPanel(fxDisplay));
        // -- 7. launch the visualization -------------------------------------
        vis.run("nodes");
        vis.run("color");
        vis.run("layout");

        // Allow bibentry tablelistener to move position on prrv
        isStarted = true;

        // Deinitialize used lists
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                isStarted = false;
                fxDisplay.getChildren().clear();
                root.getChildren().clear();
                EdgeRenderer.arrowHeadList.clear();
                EdgeRenderer.edgeList.clear();
            }
        });
    }

    /**
     * Put a zoomslider to control zoom property
     * @param fxDisplay Target
     * @return vbox for fxdisplay
     */
    private Node buildControlPanel(FxDisplay fxDisplay) {
        VBox vbox = new VBox();
        //vbox.setStyle("-fx-background-color: aliceblue");
        Label txt = new Label("Zoom Factor");
        Slider slider = new Slider(0.0, 10.0, 1.0);
        Label txt2 = new Label("");
        fxDisplay.zoomFactorProperty().bind(slider.valueProperty());
        vbox.getChildren().addAll(txt, slider, txt2);
        return vbox;
    }

    /**
     * Put a checkbox on display
     * @param fxDisplay Target
     * @return vbox, which need to be applied on display
     */
    private Node showKeyCheckBox(FxDisplay fxDisplay) {
        VBox vbox = new VBox();
        //vbox.setStyle("-fx-background-color: aliceblue");
        keyCB.setText("Show all keys");
        keyCB.setOnAction(checkBoxEvent -> handleCheckBoxAction(fxDisplay));
        vbox.getChildren().addAll(keyCB);
        return vbox;
    }

    private void handleCheckBoxAction(FxDisplay fxDisplay) {

        if (keyCB.isSelected()) {

            Iterator<VisualItem> it = fxDisplay.getVisualization().items();
            while(it.hasNext()) {
                if(it.hasNext()) {
                    VisualItem item = it.next();
                    if (item instanceof NodeItem) {
                        control.showNodeKey(item, fxDisplay);
                    }
                }
            }
        } else {
            control.hideNodeKeys();
        }
    }

    public static void notifyDisplayChange(String bibtexkey) {
        control.viewOnSelectedBibTexKey(bibtexkey, fxDisplay, root);
    }

}
// TODO Legende für farben