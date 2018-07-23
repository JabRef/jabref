package org.jabref.model.entry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BibtexStringTest {

    @Test
    public void test() {
        // Instantiate
        BibtexString bs = new BibtexString("AAA", "An alternative action");
        bs.setId("ID");
        assertEquals("ID", bs.getId());
        assertEquals("AAA", bs.getName());
        assertEquals("An alternative action", bs.getContent());
        assertEquals(BibtexString.Type.OTHER, bs.getType());

        // Clone
        BibtexString bs2 = (BibtexString) bs.clone();
        assertEquals(bs.getId(), bs2.getId());
        assertEquals(bs.getName(), bs2.getName());
        assertEquals(bs.getContent(), bs2.getContent());
        assertEquals(bs.getType(), bs2.getType());

        // Change cloned BibtexString
        bs2.setId(IdGenerator.next());
        assertNotEquals(bs.getId(), bs2.getId());
        bs2.setName("aOG");
        assertEquals(BibtexString.Type.AUTHOR, bs2.getType());
        bs2.setContent("Oscar Gustafsson");
        assertEquals("aOG", bs2.getName());
        assertEquals("Oscar Gustafsson", bs2.getContent());

    }

    @Test
    public void getContentNeverReturnsNull() {
        BibtexString bs = new BibtexString("SomeName", null);
        assertNotNull(bs.getContent());
    }
}
