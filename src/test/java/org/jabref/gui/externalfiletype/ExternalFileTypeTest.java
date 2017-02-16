package org.jabref.gui.externalfiletype;

import org.jabref.testutils.category.GUITests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertNotNull;

@Category(GUITests.class)
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
