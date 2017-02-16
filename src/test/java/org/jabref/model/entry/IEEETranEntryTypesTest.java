package org.jabref.model.entry;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class IEEETranEntryTypesTest {

    @Test
    public void ctlTypeContainsYesNoFields() {
        List<String> ctlFields = IEEETranEntryTypes.IEEETRANBSTCTL.getAllFields();
        List<String> ynFields = InternalBibtexFields.getIEEETranBSTctlYesNoFields();

        Assert.assertTrue(ctlFields.containsAll(ynFields));
    }

}
