package org.jabref.logic.formatter;

import org.jabref.logic.cleanup.Formatter;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FormatterMockTest {
    @Mock
    private Formatter formatter;

    @Test
    void allFormatterKeysAreUnique() { // small test for now to ensure repo integratation successful
        formatter = Mockito.mock();
        formatter.getKey();
        Mockito.verify(formatter).getKey();
        Mockito.when(formatter.getKey().thenReturn("mocked"));
        assertEquals("identity", formatter.getKey());
    }

}
