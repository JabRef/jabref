package net.sf.jabref.logic.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OptionalUtil {

    public static <T> List<T> toList(Optional<T> value) {
        if (value.isPresent()) {
            return Collections.singletonList(value.get());
        } else {
            return Collections.emptyList();
        }
    }

    @SafeVarargs
    public static <T> List<T> toList(Optional<T>... values) {
        return Stream.of(values).flatMap(optional -> toList(optional).stream()).collect(Collectors.toList());
    }
}
