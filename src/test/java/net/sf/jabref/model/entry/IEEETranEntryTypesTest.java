package net.sf.jabref.model.entry;

import java.util.List;

import net.sf.jabref.bibtex.InternalBibtexFields;

import org.junit.Assert;
import org.junit.Test;

public class IEEETranEntryTypesTest {

    @Test
    public void ctlTypeContainsYesNoFields() {
        List<String> ctlFields = IEEETranEntryTypes.IEEETRANBSTCTL.getAllFields();
        List<String> ynFields = InternalBibtexFields.IEEETRANBSTCTL_YES_NO_FIELDS;

        Assert.assertTrue(ctlFields.containsAll(ynFields));
    }

    @Test
    public void ctlTypeContainsNumericFields() {
        List<String> ctlFields = IEEETranEntryTypes.IEEETRANBSTCTL.getAllFields();
        List<String> numericFields = InternalBibtexFields.IEEETRANBSTCTL_NUMERIC_FIELDS;

        Assert.assertTrue(ctlFields.containsAll(numericFields));
    }

}