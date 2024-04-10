package org.jabref.model.openoffice.uno;

import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

public class DocumentAnnotation{
    private XTextDocument doc;

    public XTextDocument getDoc(){
        return doc;
    }

    public void setDoc(XTextDocument doc){
        this.doc=doc;
    }

    private String name;

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name=name;
    }

    private XTextRange range;

    public XTextRange getRange(){
        return range;
    }

    public void setRange(XTextRange range){
        this.range=range;
    }

    private boolean absorb;

    public boolean getAbsorb(){
        return absorb;
    }

    public void setAbsorb(boolean absorb){
        this.absorb=absorb;
    }

    public DocumentAnnotation(XTextDocument doc,String name,XTextRange range,boolean absorb){
        this.doc=doc;
        this.name=name;
        this.range=range;
        this.absorb=absorb;
    }
}

