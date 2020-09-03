package org.jabref.jabmap.model;

import java.util.List;

/**
 * This class is a data model for a complete mind map
 */
public class MindMap {

    private List<MindMapNode> nodes;
    private List<MindMapEdge> edges;

    public List<MindMapNode> getNodes(){
        return this.nodes;
    }

    public void addNode(MindMapNode node){
        this.nodes.add(node);
    }

    public void removeNode(MindMapNode node){
        this.nodes.remove(node);

        for (MindMapEdge edge : edges){
            if(edge.getParent().getId().equals(node.getId())){
                this.edges.remove(edge);
            }
        }
    }

    public List<MindMapEdge> getEdges(){
        return this.edges;
    }

    public void addEdge(MindMapEdge edge){
        this.edges.add(edge);
    }

    public void removeEdge(MindMapEdge edge){
        this.edges.remove(edge);
    }


}
