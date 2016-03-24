package net.sf.jabref.external;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExternalFileTypeTest {

    @Test
    public void getOpenWithApplicationMustNotReturnNull() throws Exception {
        ExternalFileType type = new ExternalFileType(null, null, null, null, null, null);

        assertNotNull(type.getOpenWithApplication());
    }
}