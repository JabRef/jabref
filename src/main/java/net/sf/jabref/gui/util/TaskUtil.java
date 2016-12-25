package net.sf.jabref.gui.util;

import java.util.concurrent.Callable;

import javafx.concurrent.Task;

public class TaskUtil {
    public static <T> Task<T> create(Callable<T> callable) {
        return new Task<T>() {
            @Override
            public T call() throws Exception {
                return callable.call();
            }
        };
    }
}
