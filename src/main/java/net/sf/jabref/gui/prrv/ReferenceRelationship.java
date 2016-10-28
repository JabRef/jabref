package net.sf.jabref.gui.prrv;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.FXDialogs;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import prefux.data.Edge;
import prefux.data.Graph;

/**
 * Gather cites of each bibtexentry
 */
public class ReferenceRelationship {

    // List to store the temporary data for graph visualisation
    private List<BibEntry> pureEntryList = JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext().getDatabase().getEntries();
    private List<prefux.data.Node> entryNodeList = new ArrayList<>();
    private List<prefux.data.Node> externalNodeList = new ArrayList<>();
    private List<prefux.data.Edge> currentEdgeList = new ArrayList<>();

    private boolean allowExternalNodes = false;
    // Some config
    // Set the text of cites when initialized
    private String initCitesField = "";
    private boolean showNodesWithoutEdges = true;

    /**
     * Generate necessary node for prefux
     * For every BibTex entry and reference to external papers generate one node
     */
    public void parseBibTexForReferences(Graph g) {

        if (JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext().getDatabase().getEntries().isEmpty()) {
            FXDialogs.showInformationDialogAndWait(Localization.lang("No_references_found"), "Please load a database");
        } else {
            // Init - Create nodes for each bibtex entries
            for (int sourceID = 0; pureEntryList.size() > sourceID; sourceID++) {
                if (pureEntryList.get(sourceID).getField("bibtexkey").isPresent()) {
                    if(!pureEntryList.get(sourceID).getField("cites").isPresent()) {
                        pureEntryList.get(sourceID).getFieldMap().put("cites", initCitesField);
                    }

                    // Split cites for citeCount
                    String[] citeLines = pureEntryList.get(sourceID).getField("cites").get().split(",");
                    // Create node of bibtex entry
                    entryNodeList.add(createNode(sourceID, pureEntryList.get(sourceID).getField("bibtexkey").get(), citeLines.length + 1, "I", g));
                }
            }

            // Create edges & external nodes
            for (int sourceID = 0; pureEntryList.size() > sourceID; sourceID++) {
                if (pureEntryList.get(sourceID).getField("cites").isPresent()) {
                    // Split cites
                    String[] citeLines = pureEntryList.get(sourceID).getField("cites").get().split(",");

                    // Go trough all cites lines of your current entry and check if it refers to some of your bibtex entries
                    for (String citeLine : citeLines) {

                        // boolean to know if cites was been found
                        boolean foundCite = false;

                        // Skip empty lines
                        if (!citeLine.isEmpty()) {
                            // Check every other entry
                            for (int targetID = 0; pureEntryList.size() > targetID; targetID++) {
                                // Don't refer to yourself
                                // Cite matching with bibtexkey of any bibtex entry?
                                if (citeLine.trim().equals((pureEntryList.get(targetID).getField("bibtexkey").get().trim()))
                                        && sourceID != targetID ) {
                                    // Add edge and skip rest of search
                                    addEdge(sourceID, targetID, g);
                                    foundCite = true;
                                }
                            }

                            // Cites doesen't belong to your bibtex entries
                            if (!foundCite) {
                                // Check external library
                                if (allowExternalNodes) {
                                    // Already in external library?
                                    if(externalNodeList.size() > 0) {
                                        for (int targetID = 0; externalNodeList.size() > targetID; targetID++) {
                                            if (citeLine.trim().equals(externalNodeList.get(targetID).getString("bibtexkey").trim())) {
                                                // Refer to external node
                                                addEdge(sourceID, targetID + pureEntryList.size(), g);
                                                foundCite = true;
                                                break;
                                            }
                                        }
                                    }
                                    if(!foundCite) {
                                        // Create another external cite node and set edge
                                        externalNodeList.add(createNode(entryNodeList.size(), citeLine, 1, "E", g));
                                        addEdge(sourceID, entryNodeList.size(), g);
                                        foundCite = true;
                                    }
                                }
                                if (!foundCite && showNodesWithoutEdges) {
                                    // In prefux it will fxDisplay nodes if they got edges, even if they target on themself
                                    addEdge(sourceID, sourceID, g);
                                }
                            }
                        }
                        else {
                            if (showNodesWithoutEdges) {
                                addEdge(sourceID, sourceID, g);
                            }
                        }

                    }
                }
            }
        }
    }

    /**
     * Adds an edge to the graph
     *
     * @param sourceID start of the edge
     * @param targetID end of the edge
     */
    private void addEdge(int sourceID, int targetID, Graph graph) {
        Edge edge = graph.addEdge(graph.getNode(sourceID), graph.getNode(targetID));
    }

    /**
     * Creates a node with necessary attributes
     *
     * @param ID             unique value
     * @param bibtexkey      should be unique
     * @param citeCount      define size of note
     * @param location       intern or extern reference
     * @param graph          Graph of prrv
     * @return node
     */
    private prefux.data.Node createNode(int ID, String bibtexkey, int citeCount, String location, Graph graph) {
        // Create node
        prefux.data.Node node = graph.addNode();

        // Set & add node
        node.set("id", ID);
        node.set("bibtexkey", bibtexkey);
        node.set("citeCount", citeCount);
        node.set("location", location);

        return node;
    }

}