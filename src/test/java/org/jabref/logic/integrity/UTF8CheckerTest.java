package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UTF8CheckerTest {

    private final BibEntry entry = new BibEntry();

    @Test
    void fieldAcceptsUTF8() {
        UTF8Checker checker = new UTF8Checker();
        entry.setField(StandardField.TITLE, "Only ascii characters!'@12");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void fieldDoesNotAcceptUmlauts() {
        System.getProperties().put("file.encoding", "GBK");
        UTF8Checker checker = new UTF8Checker();
        String NonUTF8 = "";
        try {
            NonUTF8 = new String("你好，这条语句使用GBK字符集".getBytes(), "GBK");
        }catch (Exception e){
            e.printStackTrace();
        }
        entry.setField(StandardField.MONTH, NonUTF8);
        assertEquals(List.of(new IntegrityMessage("Non-UTF-8 encoded found", entry, StandardField.MONTH)), checker.check(entry));
    }


}
