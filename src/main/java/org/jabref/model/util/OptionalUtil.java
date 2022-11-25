package org.jabref.model.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
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

    public static <T, U> boolean equals(Optional<T> left, Optional<U> right, BiPredicate<T, U> equality) {
        if (!left.isPresent()) {
            return !right.isPresent();
        } else {
            if (right.isPresent()) {
                return equality.test(left.get(), right.get());
            } else {
                return false;
            }
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

    public static <T> Boolean isPresentAndTrue(Optional<Boolean> value) {
        return value.isPresent() && value.get();
    }

    public static <T, S, R> Optional<R> combine(Optional<T> valueOne, Optional<S> valueTwo, BiFunction<T, S, R> combine) {
        if (valueOne.isPresent() && valueTwo.isPresent()) {
            return Optional.ofNullable(combine.apply(valueOne.get(), valueTwo.get()));
        } else {
            return Optional.empty();
        }
    }

    public static <T> Optional<T> orElse(Optional<? extends T> valueOne, Optional<? extends T> valueTwo) {
        if (valueOne.isPresent()) {
            return valueOne.map(f -> f);
        } else {
            return valueTwo.map(f -> f);
        }
    }

    public static <T extends S, S> S orElse(Optional<T> optional, S otherwise) {
        if (optional.isPresent()) {
            return optional.get();
        } else {
            return otherwise;
        }
    }
}
