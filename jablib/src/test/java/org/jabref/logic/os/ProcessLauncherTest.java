package org.jabref.logic.os;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    void outputIsDiscarded() throws IOException {
        ProcessLauncher.startDiscardingOutput(processBuilder);

        verify(processBuilder).redirectOutput(ProcessBuilder.Redirect.DISCARD);
        verify(processBuilder).redirectError(ProcessBuilder.Redirect.DISCARD);
        verify(process, never()).getInputStream();
    }

    @Test
    void outputAndErrorOutputAreForwarded() throws IOException, InterruptedException {
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("first\nsecond\n".getBytes(StandardCharsets.UTF_8)));
        List<String> lines = new CopyOnWriteArrayList<>();
        CountDownLatch allLinesRead = new CountDownLatch(2);

        ProcessLauncher.startForwardingOutput(processBuilder, line -> {
            lines.add(line);
            allLinesRead.countDown();
        });

        verify(processBuilder).redirectErrorStream(true);
        verify(processBuilder, never()).redirectOutput(any(ProcessBuilder.Redirect.class));
        assertTrue(allLinesRead.await(10, TimeUnit.SECONDS));
        assertEquals(List.of("first", "second"), lines);
    }

    @Test
    void stdinOfTheProgramIsClosed() throws IOException {
        ProcessLauncher.startDetached(processBuilder);

        verify(stdin).close();
    }
}
