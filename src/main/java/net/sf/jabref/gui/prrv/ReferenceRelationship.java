package net.sf.jabref.gui.prrv;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.FXDialogs;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import prefux.data.Edge;
import prefux.data.Graph;

//import net.sf.jabref.gui.prrv.model.Graph;

/**
 * @author Daniel Brühl
 */
public class ReferenceRelationship {

    private static final String NODESTORE_XML = "./nodestore-jaxb.xml";

    // List to store the temporary data for graph visualisation
    private List<BibEntry> pureEntryList = JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext().getDatabase().getEntries();
    private List<prefux.data.Node> entryNodeList = new ArrayList<>();
    private List<prefux.data.Node> externNodeList = new ArrayList<>();
    private List<prefux.data.Edge> currentEdgeList = new ArrayList<>();

    /**
     * Generate necessary node for prefux graph reader.
     * For every bibtex entry and reference to extern papers generate one node
     *
     */
    public void parseBibTexForReferences(Graph g) {

        if (JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext().getDatabase().getEntries() == null) {
            FXDialogs.showInformationDialogAndWait(Localization.lang("No_references_found"),"content");
        }
        else {

            // Init
            for (int sourceID = 0; pureEntryList.size() > sourceID; sourceID++) {
                // Split references
                String[] referenceLines = pureEntryList.get(sourceID).getField("references").get().split(";");
                // Create node of bibtex entry
                entryNodeList.add(createNode(sourceID, pureEntryList.get(sourceID).getField("title").get(), referenceLines.length + 1,g));
            }

            // Make Edges
            for (int sourceID = 0; pureEntryList.size() > sourceID; sourceID++) {

                // Split references
                String[] referenceLines = pureEntryList.get(sourceID).getField("references").get().split(";");

                // Go trough all reference lines of your current entry and check if it refers to some of your bibtex entries
                for (String referenceLine : referenceLines) {

                    // boolean to know if reference was found
                    boolean foundReference = false;

                    // Skip empty lines
                    if (referenceLine != null) {
                        for (int targetID = 0; pureEntryList.size() > targetID; targetID++) {
                            // Don't refer to himself
                            if (sourceID != targetID) {

                                // Reference matching with title of any bibtex entry?
                                if (referenceLine.trim().matches(pureEntryList.get(targetID).getField("title").get().trim())) {
                                    // Add edge and skip rest of search
                                    addEdge(sourceID, targetID, g);
                                    foundReference = true;
                                }
                            }
                        }

                        // Reference doesen't belong to your bibtex entries
                        if (foundReference == false) {
                            // Check extern library
                            for (int targetID = 0; externNodeList.size() > targetID; targetID++) {
                                System.out.println("Extern Node" + targetID + ": " + externNodeList.get(targetID).getString("title").trim());
                                if (referenceLine.trim().matches(externNodeList.get(targetID).getString("title").trim())) {
                                    // Refer to externe node
                                    addEdge(sourceID, targetID + pureEntryList.size(),g);

                                } else {
                                    // Create extern reference node and set edge
                                    externNodeList.add(createNode(pureEntryList.size() + externNodeList.size(), referenceLine, 1,g));
                                    addEdge(sourceID, pureEntryList.size() + externNodeList.size(),g);
                                    System.out.println("Added extern node: " + referenceLine);
                                }
                            }
                        }
                    }

                }
            }

            // create nodestore, assigning node
            //Graphml nodestore = new Graphml();
            //nodestore.getGraph().setNodeList(entryNodeList);
            //nodestore.getGraph().setEdgeList(currentEdgeList);

        }
    }

    /**
     * Creates an edge for JAXB context and graph reader
     * @param sourceID start of the edge
     * @param targetID end of the edge
     */
    private void addEdge(int sourceID, int targetID, Graph graph) {

        System.out.println("getEdgeCount" + graph.getEdgeCount());
        System.out.println("getNodeCount" + graph.getNodeCount());
        Edge edge = graph.addEdge(graph.getNode(sourceID),graph.getNode(targetID));
    }

    /**
     * Creates a node for for JAXB context and graph reader with necessary attributes
     * @param ID
     * @param title
     * @param referenceCount
     * @return node
     */
    private prefux.data.Node createNode(int ID, String title, int referenceCount, Graph graph) {
        // Create node
        prefux.data.Node node = graph.addNode();

        // Set & add title
        node.set("title",title);
        node.set("id", ID);
        node.set("referenceCount", referenceCount);
        return node;
    }

}