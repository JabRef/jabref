package org.jabref.logic.xmp;

public class XMPToPDF {
    public static boolean verificaPDF(String path)
    if(path.endsWith(".pdf")){
        return true;
    }else{
        return false;
    }
}
