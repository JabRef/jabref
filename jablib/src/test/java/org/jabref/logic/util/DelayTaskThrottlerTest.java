package org.jabref.logic.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DelayTaskThrottlerTest {

    @Test
    void cancelWithoutScheduledTaskDoesNotThrow() {
        DelayTaskThrottler throttler = new DelayTaskThrottler(200);
        try {
            assertDoesNotThrow(throttler::cancel);
        } finally {
            throttler.shutdown();
        }
    }

    @Test
    void cancelPreventsScheduledTaskExecution() throws InterruptedException {
        DelayTaskThrottler throttler = new DelayTaskThrottler(200);
        AtomicInteger executionCount = new AtomicInteger();
        try {
            throttler.schedule(executionCount::incrementAndGet);
            throttler.cancel();

            Thread.sleep(300);
            assertEquals(0, executionCount.get());
        } finally {
            throttler.shutdown();
        }
    }
}
