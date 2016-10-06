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
import prefux.render.LabelRenderer;
import prefux.render.ShapeRenderer;
import prefux.util.ColorLib;
import prefux.util.PrefuseLib;
import prefux.visual.VisualItem;

public class PrrvDialogView extends FXMLView {
    private static final double WIDTH = 900;
    private static final double HEIGHT = 750;
    private static final String GROUP = "Graph";

    public void show() {


        //Graph graph = null;
        // -- 1. setup dialog -----------------------------------------------------
        Stage p = new Stage();
        p.setTitle("PRRV");
        BorderPane root = new BorderPane();
        p.setScene(new Scene(root, WIDTH, HEIGHT));
        root.getStyleClass().add("display");
        p.show();


        // -- New Table for graph

        // Create tables for node and edge data, and configure their columns.
        Table nodeData = new Table();
        Table edgeData = new Table();

        nodeData.addColumn("id", Integer.class);
        nodeData.addColumn("title", String.class);
        nodeData.addColumn("referenceCount", Integer.class);

        edgeData.addColumn(Graph.DEFAULT_SOURCE_KEY, int.class);
        edgeData.addColumn(Graph.DEFAULT_TARGET_KEY, int.class);

        // -- New graph
        // Create Graph backed by those tables.  Note that I'm
        // creating a directed graph here also.
        Graph graph = new Graph(nodeData, edgeData, true);
        ReferenceRelationship rr = new ReferenceRelationship();
        rr.parseBibTexForReferences(graph);

    // -- 2. load the data ------------------------------------------------
    //File file = new File("./nodestore-jaxb.xml");
    //graph = new GraphMLReader().readGraph(file);

    // TODO ZOOM auf x/y von biggest node


    // -- 3. the visualization --------------------------------------------
    Visualization vis = new Visualization();
    vis.addGraph(GROUP, graph);



    // -- 4. the renderers and renderer factory ---------------------------
    LabelRenderer lr = new LabelRenderer("title");
    ShapeRenderer sr = new ShapeRenderer();
    sr.setFillMode(ShapeRenderer.GRADIENT_SPHERE);
        sr.setBaseSize(20);
    // lr.translate(5.0, 5.0);
    // LabelRenderer lr2 = new LabelRenderer("name");
    // lr2.addStyle("invisible");
    // BorderPaneRenderer r = new BorderPaneRenderer();
//			CombinedRenderer r = new CombinedRenderer();
//			r.add(lr);
//			r.add(sr);

    // create a new default renderer factory
    // return our name label renderer as the default for all
    // non-EdgeItems
    // includes straight line edges for EdgeItems by default
    DefaultRendererFactory rfa = new DefaultRendererFactory();
    Predicate exp = ExpressionParser.predicate("ISNODE()");
    rfa.add(exp, sr);
        rfa.setDefaultEdgeRenderer(sr);
    //vis.setRendererFactory(rfa);

    // -- 5. the processing actions ---------------------------------------
    ActionList layout = new ActionList(Activity.INFINITY, 30);
    layout.add(new ForceDirectedLayout("Graph"));
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

    DataColorAction dataColor = new DataColorAction(NODES, exp, "referenceCount",
            Constants.NUMERICAL, VisualItem.FILLCOLOR, colorPalette);
    nodeActions.add(dataColor);
    vis.putAction("nodes", nodeActions);

    // -- 6. the display and interactive controls -------------------------
    FxDisplay display = new FxDisplay(vis);
    display.addControlListener(new DragControl());

    // -- 7. launch the visualization -------------------------------------
    root.setCenter(display);
    root.setBottom(buildControlPanel(display));
    vis.run("nodes");
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
