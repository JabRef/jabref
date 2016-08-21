package net.sf.jabref.model.entry;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class IEEETranEntryTypesTest {

    @Test
    public void ctlTypeContainsYesNoFields() {
        List<String> ctlFields = IEEETranEntryTypes.IEEETRANBSTCTL.getAllFields();
        List<String> ynFields = InternalBibtexFields.IEEETRANBSTCTL_YES_NO_FIELDS;

        Assert.assertTrue(ctlFields.containsAll(ynFields));
    }

}