package org.jabref.model.entry;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class IEEETranEntryTypesTest {

    @Test
    public void ctlTypeContainsYesNoFields() {
        Collection<String> ctlFields = IEEETranEntryTypes.IEEETRANBSTCTL.getAllFields();
        List<String> ynFields = InternalBibtexFields.getIEEETranBSTctlYesNoFields();

        assertTrue(ctlFields.containsAll(ynFields));
    }

}
