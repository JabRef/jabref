package org.jabref.logic.os;

import java.io.IOException;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class ProcessLauncherTest {

    private final ProcessBuilder processBuilder = mock(ProcessBuilder.class);

    @BeforeEach
    void setUp() throws IOException {
        when(processBuilder.command()).thenReturn(List.of("editor", "file.tex"));
        when(processBuilder.start()).thenReturn(mock(Process.class));
    }

    @Test
    void outputIsDiscarded() throws IOException {
        ProcessLauncher.startDetached(processBuilder);

        verify(processBuilder).redirectOutput(ProcessBuilder.Redirect.DISCARD);
        verify(processBuilder).redirectError(ProcessBuilder.Redirect.DISCARD);
    }

    @Test
    void inputIsReadFromTheNullDevice() throws IOException {
        ProcessLauncher.startDetached(processBuilder);

        verify(processBuilder).redirectInput(ProcessBuilder.Redirect.from(ProcessLauncher.NULL_DEVICE));
    }

    /// Guards the device name, which differs between Windows and other systems
    @Test
    void nullDeviceExists() {
        assertTrue(ProcessLauncher.NULL_DEVICE.exists());
    }
}
