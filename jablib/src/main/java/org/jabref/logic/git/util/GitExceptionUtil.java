package org.jabref.logic.git.util;

import org.eclipse.jgit.errors.LockFailedException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class GitExceptionUtil {

    private GitExceptionUtil() {
    }

    public static boolean containsCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public static boolean isLockFailure(Throwable throwable) {
        return containsCause(throwable, LockFailedException.class);
    }

    public static boolean isMissingObjectFailure(Throwable throwable) {
        return containsCause(throwable, MissingObjectException.class);
    }
}
