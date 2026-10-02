package com.template.api.shared.application.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CursorResult Unit Tests")
class CursorResultTest {

    record Item(String id, String name) {}

    @Test
    @DisplayName("Should create CursorResult with nextCursor when items exceed limit")
    void of_withMoreItemsThanLimit_shouldCalculateNextCursor() {
        List<Item> fetched = List.of(
                new Item("c1", "First"),
                new Item("c2", "Second"),
                new Item("c3", "Third") // limit + 1 element
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
        List<String> mutable = new ArrayList<>();
        mutable.add("item1");

        CursorResult<String> result = new CursorResult<>(mutable, "cursor-1");
        mutable.add("item2");

        assertThat(result.items()).containsExactly("item1");
        assertThatThrownBy(() -> result.items().add("item3"))
                .isInstanceOf(UnsupportedOperationException.class);

        CursorResult<String> nullItems = new CursorResult<>(null, null);
        assertThat(nullItems.items()).isEmpty();
    }

    @Test
    @DisplayName("Should map items preserving nextCursor")
    void map_shouldTransformElementsPreservingCursor() {
        CursorResult<Item> result = new CursorResult<>(
                List.of(new Item("1", "A"), new Item("2", "B")),
                "cursor-2"
        );

        CursorResult<String> mapped = result.map(Item::name);

        assertThat(mapped.items()).containsExactly("A", "B");
        assertThat(mapped.nextCursor()).isEqualTo("cursor-2");
    }

    @Test
    @DisplayName("Should map empty items preserving nextCursor")
    void map_emptyItems_shouldPreserveCursor() {
        CursorResult<String> empty = new CursorResult<>(List.of(), "cursor-next");
        CursorResult<Integer> mapped = empty.map(String::length);

        assertThat(mapped.items()).isEmpty();
        assertThat(mapped.nextCursor()).isEqualTo("cursor-next");
    }

    @Test
    @DisplayName("Should throw NullPointerException when mapper is null")
    void map_nullMapper_shouldThrowNullPointerException() {
        CursorResult<String> result = new CursorResult<>(List.of("a"), "c1");

        assertThatThrownBy(() -> result.map(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("mapper cannot be null");
    }
}
