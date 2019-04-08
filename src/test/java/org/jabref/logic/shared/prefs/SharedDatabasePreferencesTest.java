package org.jabref.logic.shared.prefs;

import org.junit.jupiter.api.Test;
import static org.junit.Assert.assertEquals;

public class SharedDatabasePreferencesTest {

    @Test
    public void givenPortName_whenGetPortNameMethod_thenReturnCorrectName() {
        SharedDatabasePreferences sut = new SharedDatabasePreferences();
        sut.setName("Donaldson");
        String result = sut.getName().toString();
        assertEquals(result, "Optional[Donaldson]");
    }
}
