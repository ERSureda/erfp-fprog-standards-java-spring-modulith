package com.template.api.shared.application.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test suite for {@link CursorResult}.
 * <p>
 * Verifies keyset pagination calculations, cursor extraction, and collection immutability.
 */
@DisplayName("CursorResult Unit Tests")
class CursorResultTest {

    record Item(String id, String name) {}

    @Test
    @DisplayName("Should create CursorResult with nextCursor when items exceed limit")
    void of_withMoreItemsThanLimit_shouldCalculateNextCursor() {
        List<Item> fetched = List.of(
                new Item("c1", "First"),
                new Item("c2", "Second"),
                new Item("c3", "Third")
        );

        CursorResult<Item> result = CursorResult.of(fetched, 2, Item::id);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items()).extracting(Item::id).containsExactly("c1", "c2");
        assertThat(result.nextCursor()).isEqualTo("c2");
    }

    @Test
    @DisplayName("Should create CursorResult with null nextCursor when items do not exceed limit")
    void of_withItemsWithinLimit_shouldHaveNullCursor() {
        List<Item> fetched = List.of(
                new Item("c1", "First"),
                new Item("c2", "Second")
        );

        CursorResult<Item> result = CursorResult.of(fetched, 2, Item::id);

        assertThat(result.items()).hasSize(2);
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("Should handle empty, null or non-positive limit safely")
    void of_withInvalidInputs_shouldReturnEmpty() {
        assertThat(CursorResult.of(null, 10, Object::toString).items()).isEmpty();
        assertThat(CursorResult.of(null, 10, Object::toString).nextCursor()).isNull();

        assertThat(CursorResult.of(List.of(), 10, Object::toString).items()).isEmpty();
        assertThat(CursorResult.of(List.of("a"), 0, Object::toString).items()).isEmpty();
        assertThat(CursorResult.of(List.of("a"), -1, Object::toString).items()).isEmpty();
    }

    @Test
    @DisplayName("Should ensure items list is defensively copied and unmodifiable")
    void immutability_itemsShouldBeUnmodifiable() {
        List<Item> mutableList = new ArrayList<>();
        mutableList.add(new Item("c1", "First"));

        CursorResult<Item> result = new CursorResult<>(mutableList, "c1");
        mutableList.add(new Item("c2", "Second"));

        assertThat(result.items()).hasSize(1);
        assertThatThrownBy(() -> result.items().add(new Item("c3", "Third")))
                .isInstanceOf(UnsupportedOperationException.class);

        CursorResult<Item> nullItemsResult = new CursorResult<>(null, null);
        assertThat(nullItemsResult.items()).isEmpty();
    }

    @Test
    @DisplayName("Should map items preserving the cursor pointer")
    void map_shouldTransformElementsPreservingCursor() {
        CursorResult<Item> original = new CursorResult<>(
                List.of(new Item("c1", "Alice"), new Item("c2", "Bob")),
                "c2"
        );

        CursorResult<String> mapped = original.map(Item::name);

        assertThat(mapped.items()).containsExactly("Alice", "Bob");
        assertThat(mapped.nextCursor()).isEqualTo("c2");
    }

    @Test
    @DisplayName("Should return empty items when mapping an empty CursorResult")
    void map_withEmptyItems_shouldReturnEmptyResult() {
        CursorResult<Item> original = new CursorResult<>(List.of(), "c0");

        CursorResult<String> mapped = original.map(Item::name);

        assertThat(mapped.items()).isEmpty();
        assertThat(mapped.nextCursor()).isEqualTo("c0");
    }

    @Test
    @DisplayName("Should reject null mapper function")
    void map_withNullMapper_shouldThrowNullPointerException() {
        CursorResult<Item> original = new CursorResult<>(List.of(new Item("c1", "Alice")), "c1");

        assertThatThrownBy(() -> original.map(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should reject null cursor extractor when calculation is needed")
    void of_withNullCursorExtractor_shouldThrowNullPointerException() {
        List<Item> fetched = List.of(new Item("c1", "Alice"));

        assertThatThrownBy(() -> CursorResult.of(fetched, 1, null))
                .isInstanceOf(NullPointerException.class);
    }
}
