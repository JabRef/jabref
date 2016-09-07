package net.sf.jabref.logic.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.bouncycastle.asn1.ua.DSTU4145NamedCurves.params;

public class OptionalUtil {

    public static <T> List<T> toList(Optional<T> value) {
        if (value.isPresent()) {
            return Collections.singletonList(value.get());
        } else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("varargs")
    public static <T> List<T> toList(Optional<T>... values) {
        return Stream.of(values).flatMap(optional -> toList(optional).stream()).collect(Collectors.toList());
    }
}
