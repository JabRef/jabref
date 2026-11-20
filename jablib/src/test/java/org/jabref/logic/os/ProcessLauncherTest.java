package org.jabref.logic.os;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class ProcessLauncherTest {

    private final ProcessBuilder processBuilder = mock(ProcessBuilder.class);
    private final Process process = mock(Process.class);
    private final OutputStream stdin = mock(OutputStream.class);

    @BeforeEach
    void setUp() throws IOException {
        when(processBuilder.command()).thenReturn(List.of("editor", "file.tex"));
        when(processBuilder.start()).thenReturn(process);
        when(process.getOutputStream()).thenReturn(stdin);
    }

    @Test
    void outputOfTheProgramIsDiscarded() throws IOException {
        ProcessLauncher.startDetached(processBuilder);

        verify(processBuilder).redirectOutput(ProcessBuilder.Redirect.DISCARD);
        verify(processBuilder).redirectError(ProcessBuilder.Redirect.DISCARD);
    }

    @Test
    void stdinOfTheProgramIsClosed() throws IOException {
        ProcessLauncher.startDetached(processBuilder);

        verify(stdin).close();
    }
}
