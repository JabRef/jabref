package org.jabref.logic.sharelatex;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ShareLatexJsonMessageTest {

    @Test
    public void testcreateDeleteInsertMessage() {
        String expected = "{\"name\":\"applyOtUpdate\",\"args\":[\"5936d96b1bd5906b0082f53e\",{\"doc\":\"5936d96b1bd5906b0082f53e\",\"op\":[{\"p\":0,\"d\":\"ToDelete \"},{\"p\":0,\"i\":\" To Insert\"}],\"v\":68}]}";
        ShareLatexJsonMessage message = new ShareLatexJsonMessage();

        String result = message.createDeleteInsertMessage("5936d96b1bd5906b0082f53e", 0, 68, "ToDelete ", " To Insert");
        assertEquals(expected, result);
    }

    @Test
    public void testCreateUpdateMessageAsInsertOrDelete() {
        String expected = "{\"name\":\"applyOtUpdate\",\"args\":[\"5936d96b1bd5906b0082f53e\",{\"doc\":\"5936d96b1bd5906b0082f53e\",\"op\":[{\"p\":183,\"d\":\"Wirtschaftsinformatik\"},{\"p\":183,\"i\":\"Test\"}],\"v\":468}]}";

        List<SharelatexDoc> docsForTest = new ArrayList<>();
        SharelatexDoc testDoc = new SharelatexDoc();
        testDoc.setContent("Wirtschaftsinformatik");
        testDoc.setPosition(183);
        testDoc.setOperation("d");
        docsForTest.add(testDoc);

        SharelatexDoc testDoc2 = new SharelatexDoc();
        testDoc2.setContent("Test");
        testDoc2.setPosition(183);
        testDoc2.setOperation("i");
        docsForTest.add(testDoc2);

        ShareLatexJsonMessage message = new ShareLatexJsonMessage();
        String result = message.createUpdateMessageAsInsertOrDelete("5936d96b1bd5906b0082f53e", 468, docsForTest);
        assertEquals(expected, result);

    }
}
