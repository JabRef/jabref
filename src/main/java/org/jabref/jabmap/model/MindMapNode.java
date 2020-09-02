package org.jabref.jabmap.model;

import org.jabref.model.entry.BibEntry;

import java.util.List;

public class MindMapNode {

    private String id;
    private List<MindMapNode> children;
    private String text;
    private BibEntry bibEntry;
    private List<NodeIcon> icons;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<MindMapNode> getChildren() {
        return children;
    }

    private void addChild(MindMapNode node){
        this.children.add(node);
    }

    private void removeChild(MindMapNode node){
        this.children.remove(node);
    }

    public void setChildren(List<MindMapNode> children) {
        this.children = children;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public BibEntry getBibEntry() {
        return bibEntry;
    }

    public void setBibEntry(BibEntry bibEntry) {
        this.bibEntry = bibEntry;
    }

    public List<NodeIcon> getIcons() {
        return icons;
    }

    public void addIcon(NodeIcon icon){
        this.icons.add(icon);
    }

    public void removeIcon(NodeIcon icon){
        this.icons.remove(icon);
    }

    public void setIcons(List<NodeIcon> icons) {
        this.icons = icons;
    }
}
