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
 * @author Daniel Brühl
 */
public class ReferenceRelationship {

    // List to store the temporary data for graph visualisation
    private List<BibEntry> pureEntryList = JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext().getDatabase().getEntries();
    public List<prefux.data.Node> entryNodeList = new ArrayList<>();
    public List<prefux.data.Node> externalNodeList = new ArrayList<>();
    private List<prefux.data.Edge> currentEdgeList = new ArrayList<>();

    /**
     * Generate necessary node for prefux graph reader.
     * For every BibTex entry and reference to external papers generate one node
     */
    public void parseBibTexForReferences(Graph g) {

        if (JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext().getDatabase().getEntries().isEmpty()) {
            FXDialogs.showInformationDialogAndWait(Localization.lang("No_references_found"), "Please load a database");
        } else {
            // Init - Create nodes for bibtex entries
            for (int sourceID = 0; pureEntryList.size() > sourceID; sourceID++) {
                if (pureEntryList.get(sourceID).getField("cites").isPresent()) {
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

                        // boolean to know if cites was found
                        boolean foundCite = false;

                        // Skip empty lines
                        if (citeLine != null) {
                            // Check every other entry
                            for (int targetID = 0; pureEntryList.size() > targetID; targetID++) {
                                // Don't refer to yourself
                                // Cite matching with bibtexkey of any bibtex entry?
                                if (citeLine.trim().equals((pureEntryList.get(targetID).getField("bibtexkey").get().trim())) && sourceID != targetID) {
                                    // Add edge and skip rest of search
                                    addEdge(sourceID, targetID, g);
                                    foundCite = true;
                                }
                            }

                            // Cites doesen't belong to your bibtex entries
                            if (!foundCite) {
                                // Check external library
                                if (externalNodeList.size() > 0) {
                                    // Already in external library?
                                    for (int targetID = 0; externalNodeList.size() > targetID; targetID++) {
                                        if (citeLine.trim().equals(externalNodeList.get(targetID).getString("bibtexkey").trim())) {
                                            // Refer to external node
                                            addEdge(sourceID, targetID + pureEntryList.size(), g);
                                            foundCite = true;
                                            break;
                                        }
                                    }
                                } else {
                                    // Create first external cites node and set edge
                                    externalNodeList.add(createNode(entryNodeList.size(), citeLine, 1, "E", g));
                                    addEdge(sourceID, entryNodeList.size(), g);
                                    foundCite = true;
                                }
                                if (!foundCite) {
                                    // Create another external cite node and set edge
                                    externalNodeList.add(createNode(entryNodeList.size() + externalNodeList.size() - 1, citeLine, 1, "E", g));
                                    addEdge(sourceID, entryNodeList.size() + externalNodeList.size() - 1, g);
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    /**
     * Creates an edge
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
     * @param bibtexkey          should be unique
     * @param citeCount
     * @param location
     * @param graph
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