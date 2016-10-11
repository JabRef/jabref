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

        nodeData.addColumn("id", int.class);
        nodeData.addColumn("title", String.class);
        nodeData.addColumn("referenceCount", int.class);
        nodeData.addColumn("location", String.class);

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
        // TODO UTF8 Zeichen vollständig vergleichen können
        // TODO Haken für external nodes
        // TODO Message für ; syntax
        // TODO Arrowhead einfügen


        // -- 3. the visualization --------------------------------------------
        Visualization vis = new Visualization();
        vis.addGraph(GROUP, graph);


        // -- 4. the renderers and renderer factory ---------------------------
        //ShapeRenderer sr = new ShapeRenderer();
        //sr.setFillMode(ShapeRenderer.GRADIENT_SPHERE);
        //sr.setBaseSize(20);
        ShapeRenderer intern = new ShapeRenderer();
        intern.setFillMode(ShapeRenderer.GRADIENT_SPHERE);
        ShapeRenderer external = new ShapeRenderer();
        external.setFillMode(ShapeRenderer.GRADIENT_SPHERE);

        DefaultRendererFactory rfa = new DefaultRendererFactory();
        //Predicate exp = ExpressionParser.predicate("ISNODE()");
        Predicate internExp = ExpressionParser.predicate("location='I'");
        Predicate externalExp = ExpressionParser.predicate("location='E'");
        //rfa.add(exp, sr);
        rfa.add(internExp, intern);
        rfa.add(externalExp, external);
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

        int[] internPalette = new int[]{
                ColorLib.rgb(1, 70, 54)
        };

        int[] externalPalette = new int[] { ColorLib.rgb(122, 1, 119)};

        ColorAction nStroke = new ColorAction("graph.nodes", VisualItem.STROKECOLOR);
        nStroke.setDefaultColor(ColorLib.gray(100));
        //DataColorAction dataColor = new DataColorAction(NODES, exp, "referenceCount",
          //      Constants.NUMERICAL, VisualItem.FILLCOLOR, colorPalette);
        DataColorAction colorI = new DataColorAction(NODES, internExp, "referenceCount",
                Constants.NUMERICAL, VisualItem.FILLCOLOR, internPalette);
        DataColorAction colorE = new DataColorAction(NODES, externalExp, "referenceCount",
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

        //nodeActions.add();

        vis.putAction("nodes", nodeActions);
        vis.putAction("color", color);

        // -- 6. the display and interactive controls -------------------------
        FxDisplay display = new FxDisplay(vis);
        display.addControlListener(new DragControl());
        root.setCenter(display);
        // Setup zoom box
        root.setBottom(buildControlPanel(display));
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
