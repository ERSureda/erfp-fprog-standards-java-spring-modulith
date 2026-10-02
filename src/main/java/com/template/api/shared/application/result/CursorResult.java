package com.template.api.shared.application.result;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Minimalist immutable container for cursor-based (keyset) paginated query results.
 * <p>
 * Encapsulates a window of elements along with an opaque cursor string for subsequent window retrieval.
 * Eliminates database offset degradation and costly count operations without coupling to persistence frameworks.
 *
 * @param items      immutable list of elements present in the current window
 * @param nextCursor opaque pointer to fetch the subsequent window, or {@code null} if no further records exist
 * @param <T>        type of elements contained in the result
 */
public record CursorResult<T>(
        List<T> items,
        String nextCursor
) {

    public CursorResult {
        items = (items == null || items.isEmpty()) ? List.of() : List.copyOf(items);
    }

    /**
     * Factory method computing the next cursor from a limit-plus-one query result.
     *
     * @param itemsWithExtra  list containing up to {@code limit + 1} elements
     * @param limit           requested window size
     * @param cursorExtractor function extracting the cursor string from the boundary item
     * @param <T>             item type
     * @return initialized {@link CursorResult}
     */
    public static <T> CursorResult<T> of(
            List<T> itemsWithExtra,
            int limit,
            Function<? super T, String> cursorExtractor
    ) {
        if (itemsWithExtra == null || itemsWithExtra.isEmpty() || limit <= 0) {
            return new CursorResult<>(List.of(), null);
        }

        boolean hasMore = itemsWithExtra.size() > limit;
        List<T> content = hasMore ? itemsWithExtra.subList(0, limit) : itemsWithExtra;
        String nextCursor = hasMore ? cursorExtractor.apply(content.get(content.size() - 1)) : null;

        return new CursorResult<>(content, nextCursor);
    }

    /**
     * Transforms contained elements via a mapper while preserving the cursor pointer.
     *
     * @param mapper transformation function; must not be {@code null}
     * @param <U>    target item type
     * @return transformed {@link CursorResult}
     */
    public <U> CursorResult<U> map(Function<? super T, U> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        if (items.isEmpty()) {
            return new CursorResult<>(List.of(), nextCursor);
        }
        return new CursorResult<>(items.stream().map(mapper).toList(), nextCursor);
    }
}