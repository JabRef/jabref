package org.jabref.model.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
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

    /**
     * No longer needed in Java 9 where {@code Optional<T>.stream()} is added.
     */
    public static <T> Stream<T> toStream(Optional<T> value) {
        if (value.isPresent()) {
            return Stream.of(value.get());
        } else {
            return Stream.empty();
        }
    }

    @SafeVarargs
    public static <T> List<T> toList(Optional<T>... values) {
        return Stream.of(values).flatMap(optional -> toList(optional).stream()).collect(Collectors.toList());
    }

    public static <T, R> Stream<R> flatMapFromStream(Optional<T> value, Function<? super T, ? extends Stream<? extends R>> mapper) {
        return toStream(value).flatMap(mapper);
    }

    public static <T, R> Stream<R> flatMap(Optional<T> value, Function<? super T, ? extends Collection<? extends R>> mapper) {
        return toStream(value).flatMap(element -> mapper.apply(element).stream());
    }

    public static <T> Boolean isPresentAnd(Optional<T> value, Predicate<T> check) {
        return value.isPresent() && check.test(value.get());
    }

    public static <T, S, R> Optional<R> combine(Optional<T> valueOne, Optional<S> valueTwo, BiFunction<T, S, R> combine) {
        if (valueOne.isPresent() && valueTwo.isPresent()) {
            return Optional.ofNullable(combine.apply(valueOne.get(), valueTwo.get()));
        } else {
            return Optional.empty();
        }
    }
}
