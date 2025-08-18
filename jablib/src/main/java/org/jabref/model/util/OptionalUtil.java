package org.jabref.model.util;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class OptionalUtil {

    public static <T, U> boolean equals(Optional<T> left, Optional<U> right, BiPredicate<T, U> equality) {
        return left.map(t -> right.filter(u -> equality.test(t, u)).isPresent()).orElseGet(right::isEmpty);
    }

    /**
     * @return An immutable list containing the value - if no value: empty immutable list
     */
    public static <T> List<T> toList(Optional<T> value) {
        return value.map(List::of).orElseGet(List::of);
    }

    /**
     * @return An immutable list containing the values - if no values present: empty immutable list
     */
    @SafeVarargs
    public static <T> List<T> toList(Optional<T>... values) {
        return Stream.of(values).flatMap(Optional::stream).toList();
    }

    public static <T, R> Stream<R> flatMapFromStream(Optional<T> value, Function<? super T, ? extends Stream<? extends R>> mapper) {
        return value.stream().flatMap(mapper);
    }

    public static <T, R> Stream<R> flatMap(Optional<T> value, Function<? super T, ? extends Collection<? extends R>> mapper) {
        return value.stream().flatMap(element -> mapper.apply(element).stream());
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
