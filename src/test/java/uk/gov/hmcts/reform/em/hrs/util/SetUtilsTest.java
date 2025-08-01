package uk.gov.hmcts.reform.em.hrs.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SetUtilsTest {
    private final Set<String> setA = Set.of("", "  ", "b", "c", "d");
    private final Set<String> setB = Set.of("c", "", "  ", "d", "e", "f");

    @Test
    void testIntersectionResultingSetSizeAndValues() {
        final int expectedSize = 4;
        final Set<String> expectedSet = Set.of("c", "d", "", "  ");
        final Set<String> intersection = SetUtils.intersect(setA, setB);

        assertThat(intersection).hasSize(expectedSize).hasSameElementsAs(expectedSet);
    }

    @Test
    void testIntersectionWhenNoModificationIsNeeded() {
        final Set<String> subset = Set.of("c", "d");
        final Set<String> superset = Set.of("a", "b", "c", "d");

        final Set<String> intersection = SetUtils.intersect(subset, superset);

        assertThat(intersection)
            .hasSize(2)
            .hasSameElementsAs(subset)
            .isSameAs(subset);
    }

    @Test
    void testIntersectionResultingSetSizeWithNull() {

        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> SetUtils.intersect(null, null)
        );

        assertEquals("setA is marked non-null but is null", exception.getMessage());

        NullPointerException exception2 = assertThrows(
            NullPointerException.class,
            () -> SetUtils.intersect(setA, null)
        );

        assertEquals("setB is marked non-null but is null", exception2.getMessage());

        NullPointerException exception3 = assertThrows(
            NullPointerException.class,
            () -> SetUtils.intersect(null, setB)
        );

        assertEquals("setA is marked non-null but is null", exception3.getMessage());
    }

    @Test
    void testUnionResultingSetSizeAndValues() {
        final int expectedSize = 7;
        final Set<String> expectedSet = Set.of("", "  ", "c", "b", "d", "e", "f");
        final Set<String> intersection = SetUtils.union(setA, setB);

        assertThat(intersection).hasSize(expectedSize).hasSameElementsAs(expectedSet);
    }

    @Test
    void testUnionResultingSetElementsForNull() {

        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> SetUtils.union(null, null)
        );

        assertEquals("setA is marked non-null but is null", exception.getMessage());

        NullPointerException exception2 = assertThrows(
            NullPointerException.class,
            () -> SetUtils.union(null, setB)
        );

        assertEquals("setA is marked non-null but is null", exception2.getMessage());

        NullPointerException exception3 = assertThrows(
            NullPointerException.class,
            () -> SetUtils.union(setA, null)
        );

        assertEquals("setB is marked non-null but is null", exception3.getMessage());
    }

}
