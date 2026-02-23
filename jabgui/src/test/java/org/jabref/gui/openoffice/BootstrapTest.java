package org.jabref.gui.openoffice;

import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.connection.NoConnectException;
import com.sun.star.uno.XComponentContext;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BootstrapTest {

    @Test
    void bootstrap_retriesWithoutRealSleep_andEventuallyConnects() throws Exception {
        // Arrange
        XComponentContext expectedContext = mock(XComponentContext.class);

        // Mock resolver: fail twice, then succeed
        XUnoUrlResolver resolver = mock(XUnoUrlResolver.class);
        when(resolver.resolve(anyString()))
                .thenThrow(new NoConnectException("not ready"))
                .thenThrow(new NoConnectException("still not ready"))
                .thenReturn(expectedContext); // see note below

        // No-op sleeper that records calls (no real waiting)
        AtomicInteger sleepCalls = new AtomicInteger();
        Sleeper sleeper = millis -> sleepCalls.incrementAndGet();

        // Mock process so no real OpenOffice is started
        Process fakeProcess = mock(Process.class);
        when(fakeProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(fakeProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        ProcessStarter processStarter = cmd -> fakeProcess;
        UnoResolverFactory resolverFactory = ctx -> resolver;

        XComponentContext actual = Bootstrap.bootstrap(
                new String[] {"--nologo"},
                Path.of("fake-soffice"),
                sleeper,
                processStarter,
                resolverFactory,
                10,
                500
        );

        // Assert
        assertSame(expectedContext, actual);
        assertEquals(2, sleepCalls.get(), "Should sleep once per failed connect attempt");
        verify(resolver, times(3)).resolve(anyString());
    }
}
