package com.template.api.shared.application.result;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Minimalist immutable container for offset-based paginated query results.
 * <p>
 * Encapsulates a page window alongside total count and computed page metadata.
 * Conforms to SED-01.
 *
 * @param items         immutable list of elements present in the current page
 * @param page          zero-based page index
 * @param size          page size limit
 * @param totalElements aggregate count of matching records across all pages
 * @param totalPages    total count of computed pages
 * @param <T>           type of items contained in the result
 */
public record PageResult<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public PageResult {
        items = (items == null || items.isEmpty()) ? List.of() : List.copyOf(items);
    }

    public static <T> PageResult<T> of(List<T> items, int page, int size, long totalElements) {
        int totalPages = (size > 0 && totalElements > 0) ? (int) Math.ceilDiv(totalElements, (long) size) : 0;
        return new PageResult<>(items, page, size, totalElements, totalPages);
    }

    public <U> PageResult<U> map(Function<? super T, U> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        if (items.isEmpty()) {
            return new PageResult<>(List.of(), page, size, totalElements, totalPages);
        }
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
