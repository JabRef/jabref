package net.sf.jabref.gui.prrv;

import java.util.Iterator;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
import prefux.render.ShapeRenderer;
import prefux.util.ColorLib;
import prefux.util.PrefuseLib;
import prefux.visual.NodeItem;
import prefux.visual.VisualItem;

/**
 * @author Daniel Brühl
 */
public class PrrvDialogView {

    private static final double WIDTH = 600;
    private static final double HEIGHT = 600;
    private static final String GROUP = "graph";
    private static Stage primaryStage = new Stage();
    private CheckBox keyCB = new CheckBox();
    private Visualization vis = new Visualization();
    private FxDisplay display;
    private DisplayControl control = new DisplayControl();

    public void show() {
        // -- 1. setup dialog -----------------------------------------------------
        primaryStage.setTitle("PRRV");
        BorderPane root = new BorderPane();
        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
        root.getStyleClass().add("display");
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

        // -- 6. the display and interactive controls -------------------------
        display = new FxDisplay(vis);
        display.addControlListener(new DragControl());

        //Bind pref Witdh and Height to according stage
        root.prefWidthProperty().bind(primaryStage.widthProperty());
        root.prefHeightProperty().bind(primaryStage.heightProperty());

        root.setCenter(display);
        // Setup zoom box
        root.setBottom(buildControlPanel(display));
        root.setTop(showKeyCheckBox(display, root));
        // -- 7. launch the visualization -------------------------------------
        vis.run("nodes");
        vis.run("color");
        vis.run("layout");

        // Throw if some cite fields are not accessible
    }

    private Node buildControlPanel(FxDisplay display) {
        VBox vbox = new VBox();
        Label txt = new Label("Zoom Factor");
        Slider slider = new Slider(0.0, 10.0, 1.0);
        Label txt2 = new Label("");
        display.zoomFactorProperty().bind(slider.valueProperty());
        vbox.getChildren().addAll(txt, slider, txt2);
        return vbox;
    }

    private Node showKeyCheckBox(FxDisplay display, BorderPane pane) {
        // HERE NEXT - most important dude
        VBox vbox = new VBox();
        keyCB.setText("Show all keys");
        //attach click-method to all 3 checkboxes
        keyCB.setOnAction(checkBoxEvent -> handleCheckBoxAction(display, pane));
        vbox.getChildren().addAll(keyCB);
        return vbox;
    }

    private void handleCheckBoxAction(FxDisplay display, BorderPane pane) {

        if (keyCB.isSelected()) {

            Iterator<VisualItem> it = display.getVisualization().items();
            while(it.hasNext()) {
                if(it.hasNext()) {
                    VisualItem item = it.next();
                    if (item instanceof NodeItem) {
                        control.showNodeKey(item, display, pane);
                    }
                }
            }
        } else {
            control.hideNodeKeys();
        }
    }

}
// TODO Legende für farben
// TODO Root node ist ausgewählter bibentry (getselectedentry)
// Selected            //  View in Zentrum von selected bibentry