package net.sf.jabref.model.entry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


public class BibtexStringTest {

    @Test
    public void test() {
        // Instantiate
        String id = IdGenerator.next();
        BibtexString bs = new BibtexString(id, "AAA", "An alternative action");
        assertEquals(id, bs.getId());
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

}
