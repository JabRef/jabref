package net.sf.jabref.model.entry;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class IEEETranEntryTypesTest {

    @Test
    public void ctlTypeContainsYesNoFields() {
        List<String> ctlFields = IEEETranEntryTypes.IEEETRANBSTCTL.getAllFields();
        List<String> ynFields = Arrays.asList(IEEETranEntryTypes.IEEETRANBSTCTL_YES_NO_FIELDS);

        Assert.assertTrue(ctlFields.containsAll(ynFields));
    }

    @Test
    public void ctlTypeContainsNumericFields() {
        List<String> ctlFields = IEEETranEntryTypes.IEEETRANBSTCTL.getAllFields();
        List<String> numericFields = Arrays.asList(IEEETranEntryTypes.IEEETRANBSTCTL_NUMERIC_FIELDS);

        Assert.assertTrue(ctlFields.containsAll(numericFields));
    }

}