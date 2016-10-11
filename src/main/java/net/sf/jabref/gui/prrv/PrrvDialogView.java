package net.sf.jabref.gui.prrv;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.airhacks.afterburner.views.FXMLView;
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
import prefux.visual.VisualItem;

public class PrrvDialogView extends FXMLView {
    private static final double WIDTH = 900;
    private static final double HEIGHT = 750;
    private static final String GROUP = "graph";

    public void show() {

        // -- 1. setup dialog -----------------------------------------------------
        Stage primaryStage = new Stage();
        primaryStage.setTitle("PRRV");
        BorderPane root = new BorderPane();
        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
        root.getStyleClass().add("display");
        primaryStage.show();


        // -- New Table for graph

        // Create tables for node and edge data, and configure their columns.
        Table nodeData = new Table();
        Table edgeData = new Table();

        nodeData.addColumn("title", String.class);
        nodeData.addColumn("id", int.class);
        nodeData.addColumn("referenceCount", int.class);

        edgeData.addColumn(Graph.DEFAULT_SOURCE_KEY, int.class);
        edgeData.addColumn(Graph.DEFAULT_TARGET_KEY, int.class);

        // -- 2. load the data ------------------------------------------------
        // -- New graph
        // Create Graph backed by those tables.  Note that I'm
        // creating a directed graph here also.
        Graph graph = new Graph(nodeData, edgeData, true);
        ReferenceRelationship rr = new ReferenceRelationship();
        rr.parseBibTexForReferences(graph);

        //File file = new File("./nodestore-jaxb.xml");
        //graph = new GraphMLReader().readGraph(file);

        // TODO ZOOM auf x/y von biggest node


        // -- 3. the visualization --------------------------------------------
        Visualization vis = new Visualization();
        vis.addGraph(GROUP, graph);


        // -- 4. the renderers and renderer factory ---------------------------
        ShapeRenderer sr = new ShapeRenderer();
        sr.setFillMode(ShapeRenderer.GRADIENT_SPHERE);
        //sr.setBaseSize(20);

        DefaultRendererFactory rfa = new DefaultRendererFactory();
        Predicate exp = ExpressionParser.predicate("ISNODE()");
        rfa.add(exp, sr);
        //rfa.setDefaultEdgeRenderer(sr);
        vis.setRendererFactory(rfa);

        // -- 5. the processing actions ---------------------------------------
        ActionList layout = new ActionList(Activity.INFINITY);
        layout.add(new ForceDirectedLayout("graph"));
        layout.add(new RepaintAction());
        vis.putAction("layout", layout);

        ActionList nodeActions = new ActionList();
        final String NODES = PrefuseLib.getGroupName(GROUP, Graph.NODES);
        NodeDegreeSizeAction size = new NodeDegreeSizeAction(NODES);
        nodeActions.add(size);

        // vis.setValue(NODES, exp, VisualItem.SHAPE, new Integer(Constants.SHAPE_ELLIPSE) );

        int[] colorPalette = new int[]{
                ColorLib.rgb(1, 70, 54),
                ColorLib.rgb(1, 108, 89),
                ColorLib.rgb(2, 129, 138),
                ColorLib.rgb(54, 144, 192),
                ColorLib.rgb(103, 169, 207),
                ColorLib.rgb(166, 189, 219),
                ColorLib.rgb(208, 209, 230),
                ColorLib.rgb(236, 226, 240),
                ColorLib.rgb(255, 247, 251)
        };

        ColorAction nStroke = new ColorAction("graph.nodes", VisualItem.STROKECOLOR);
        nStroke.setDefaultColor(ColorLib.gray(100));
        //DataColorAction dataColor = new DataColorAction(NODES, exp, "referenceCount",
          //      Constants.NUMERICAL, VisualItem.FILLCOLOR, colorPalette);
            DataColorAction nFill = new DataColorAction("graph.nodes", "referenceCount",
                  Constants.NUMERICAL, VisualItem.FILLCOLOR, colorPalette);
        ColorAction edges = new ColorAction("graph.edges",
                VisualItem.STROKECOLOR, ColorLib.gray(255));
        ColorAction arrow = new ColorAction("graph.edges",
               VisualItem.FILLCOLOR, ColorLib.gray(200));
        ActionList color = new ActionList();
        color.add(nStroke);
        color.add(nFill);
        color.add(edges);
        color.add(arrow);

        //nodeActions.add();

        vis.putAction("nodes", nodeActions);
        vis.putAction("color", color);

        // -- 6. the display and interactive controls -------------------------
        FxDisplay display = new FxDisplay(vis);
        display.addControlListener(new DragControl());
        root.setCenter(display);
        // -- 7. launch the visualization -------------------------------------

        vis.run("nodes");
        vis.run("color");
        vis.run("layout");

        // FXAlert prrvDialog = new FXAlert(AlertType.INFORMATION, Localization.lang("Paper Reference Relationship Visualization"));
        //prrvDialog.setDialogPane((DialogPane) this.getView());
        //prrvDialog.show();
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

}
