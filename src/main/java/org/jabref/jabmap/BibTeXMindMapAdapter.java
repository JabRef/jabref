package org.jabref.jabmap;

import org.jabref.jabmap.model.MindMap;
import org.jabref.jabmap.model.MindMapNode;

/**
 * This class handles converting data from BibTeX format, which is stored in the database, to JSON to be sent to JapMap
 * It does the reverse, converting JSON to BibTeX to be stored.
 */
public class BibTeXMindMapAdapter {



    //TODO: Method for converting bibtex to mindmap object
    public MindMap bibTeX2MindMap(){
        MindMap mindMap = new MindMap();

        return mindMap;
    }



    //TODO: Convert bibtex entry to node
    private MindMapNode bibTeX2Node(){
        MindMapNode node = new MindMapNode();


        return node;
    }


    //TODO: Method for converting mm object to bibtex
    public void mindMap2BibTeX(){

    }

}
