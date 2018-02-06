package org.jabref;

import org.junit.rules.ExternalResource;

/**
 * JUnit by default ignores exceptions, which are reported via {@link Thread.UncaughtExceptionHandler}.
 * With this rule also these kind of exceptions result in a failure of the test.
 */
public class CatchExceptionsFromThread extends ExternalResource {
    @Override
    protected void before() throws Throwable {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            // We simply rethrow the exception (as a RuntimeException) so that JUnit picks it up
            throw new RuntimeException(exception);
        });
    }

    @Override
    protected void after() {
        Thread.setDefaultUncaughtExceptionHandler(new FallbackExceptionHandler());
    }
}
