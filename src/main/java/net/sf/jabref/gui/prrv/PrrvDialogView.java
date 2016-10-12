package net.sf.jabref.gui.prrv;

import java.util.List;
import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.EntryTypeList;
import net.sf.jabref.gui.FXDialogs;
import net.sf.jabref.model.entry.BibEntry;

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

/**
 * @author Daniel Brühl
 */
public class PrrvDialogView extends FXMLView {
    private static final double WIDTH = 600;
    private static final double HEIGHT = 600;
    private static final String GROUP = "graph";
    private static Stage primaryStage = new Stage();

    public void show() {
        try {
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
            nodeData.addColumn("referenceCount", int.class);
            nodeData.addColumn("location", String.class);

            edgeData.addColumn(Graph.DEFAULT_SOURCE_KEY, int.class);
            edgeData.addColumn(Graph.DEFAULT_TARGET_KEY, int.class);

            // -- 2. load the data ------------------------------------------------
            // Create Graph backed by those tables.  Note that I'm creating a directed graph here also.
            Graph graph = new Graph(nodeData, edgeData, true);
            ReferenceRelationship rr = new ReferenceRelationship();
            rr.parseBibTexForReferences(graph);
            if (rr.entryNodeList.isEmpty()) {
                showErrorDialog();
            } else {
                // -- 3. the visualization --------------------------------------------
                Visualization vis = new Visualization();
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
            }
            // Throw if some reference fields are not accessible
        } catch (NullPointerException e) {
            showErrorDialog();
        }
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

    private static void showErrorDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        String title = "Error while loading references";
        String content = "All used entry types need an optional field named references\n"
                + "Should JabRef insert them automatically?\n"
                + "Node:\n "
                + "- You can insert them under BibTeX < Customize entry types.\n"
                + "- Several references need to be seperated by a semicolon!\n"
                + "- BibTex entries that are not referenced are not displayed.";
        Optional<ButtonType> result = FXDialogs.showCustomButtonDialogAndWait(Alert.AlertType.INFORMATION, title, content, ButtonType.OK, ButtonType.CANCEL);
        System.out.println("PRRV DIALOG SHOW");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // ... user chose OK

            JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext();

            List<BibEntry> pureEntryList = JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext().getDatabase().getEntries();
            if (!pureEntryList.isEmpty()) {
                for (int id = 0; pureEntryList.size() > id; id++) {
                    pureEntryList.get(id).getFieldMap().put("references", "");
                }
            }
            primaryStage.close();
            EntryTypeList typeComp;
            //typeComp.addF

        } else {
            primaryStage.close();
            // ... user chose CANCEL or closed the dialog
        }
    }

}
