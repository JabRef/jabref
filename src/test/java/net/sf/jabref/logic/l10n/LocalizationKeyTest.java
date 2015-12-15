package net.sf.jabref.logic.l10n;

import org.junit.Test;

import static org.junit.Assert.*;

public class LocalizationKeyTest {

    @Test
    public void testConversionToPropertiesKey() {
        assertEquals("test_\\:_\\=", new LocalizationKey("test : =").getPropertiesKey());
    }

}