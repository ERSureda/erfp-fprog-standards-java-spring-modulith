package com.template.api.shared.application.result;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Minimalist immutable container for cursor-based (keyset) paginated query results.
 * <p>
 * Encapsulates an item window and an opaque cursor pointer for subsequent window retrieval without offset degradation.
 * Conforms to SED-01.
 *
 * @param items      immutable list of elements present in the current window
 * @param nextCursor opaque pointer to fetch the subsequent window, or null if no further records exist
 * @param <T>        type of elements contained in the result
 */
public record CursorResult<T>(
        List<T> items,
        String nextCursor
) {

    public CursorResult {
        items = (items == null || items.isEmpty()) ? List.of() : List.copyOf(items);
    }

    public static <T> CursorResult<T> of(
            List<T> itemsWithExtra,
            int limit,
            Function<? super T, String> cursorExtractor
    ) {
        if (itemsWithExtra == null || itemsWithExtra.isEmpty() || limit <= 0) {
            return new CursorResult<>(List.of(), null);
        }
        Objects.requireNonNull(cursorExtractor, "cursorExtractor cannot be null");

        boolean hasMore = itemsWithExtra.size() > limit;
        List<T> content = hasMore ? itemsWithExtra.subList(0, limit) : itemsWithExtra;
        String nextCursor = hasMore ? cursorExtractor.apply(content.get(content.size() - 1)) : null;

        return new CursorResult<>(content, nextCursor);
    }

    public <U> CursorResult<U> map(Function<? super T, U> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        if (items.isEmpty()) {
            return new CursorResult<>(List.of(), nextCursor);
        }
        return new CursorResult<>(items.stream().map(mapper).toList(), nextCursor);
    }
}
