package org.jabref.logic.xmp;

import org.junit.jupiter.api.Test;  
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveXMPTest.java {

    @Test 
    void verificaPDF(){
        String path = String.valueOf("teste.pdf");
        boolean result = XMPFunc.verificaPDF(path);
        assertEquals(expected: true, result);

    }
}

public static boolean verificaPDF(String path){ 
    return true;
}

public class RemoveXMPToPDFTest.java {

    @Test 
    void verificaRemoveXMP(){
        XMPFuncMeta primeiroPDF = new XMPFuncMeta(title: "Title", author: "Author", subject: "Subject", data: "Data");
        String result = new primeiroPDF.retiraVariavel(opcode: 1);
        assertEquals(expected: null, result);

    }
}