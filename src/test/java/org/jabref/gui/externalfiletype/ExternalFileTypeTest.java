package org.jabref.gui.externalfiletype;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExternalFileTypeTest {

    @Test
    public void getOpenWithApplicationMustNotReturnNull() throws Exception {
        ExternalFileType type = new ExternalFileType(null, null, null, null, null, null);
        assertNotNull(type.getOpenWithApplication());
    }

    @Test
    public void getExtensionMustNotReturnNull() throws Exception {
        ExternalFileType type = new ExternalFileType(null, null, null, null, null, null);
        assertNotNull(type.getExtension());
    }

    @Test
    public void getMimeTypeMustNotReturnNull() throws Exception {
        ExternalFileType type = new ExternalFileType(null, null, null, null, null, null);
        assertNotNull(type.getMimeType());
    }

}
