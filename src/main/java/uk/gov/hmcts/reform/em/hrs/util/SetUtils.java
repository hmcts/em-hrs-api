package uk.gov.hmcts.reform.em.hrs.util;

import lombok.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SetUtils {
    private SetUtils() {
    }

    public static <T> Set<T> intersect(@NonNull final Set<T> setA, @NonNull final Set<T> setB) {
        Set<T> a = new HashSet<>(setA);
        Set<T> b = new HashSet<>(setB);

        return a.retainAll(b) ? Collections.unmodifiableSet(a) : setA;
    }

    public static <T> Set<T> union(@NonNull final Set<T> setA, @NonNull final Set<T> setB) {
        return Stream.concat(setA.stream(), setB.stream())
            .collect(Collectors.toSet());
    }
}
